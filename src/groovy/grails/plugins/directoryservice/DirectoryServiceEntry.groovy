/*
 * Copyright 2012 Lucas Rockwell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugins.directoryservice

import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.SearchResultEntry

/**
 * When DirectoryService returns a result set, it wraps each result object in
 * a DirectoryServiceEntry object. This class allows you to interact with an
 * UnboundID SearchResultEntry as if it were a writable Entry. In fact, when
 * it is constructed with a SearchResultEntry, it duplicates the entry which
 * results in an UnboundID Entry object, which you can update. The updating
 * is done by overriding {@code propertyMissing(String name, value)}.
 *
 * @author Lucas Rockwell
 */
class DirectoryServiceEntry implements Serializable {
    private static final long serialVersionUID = -648257487032312756L;

    /**
     * The original searchResultEntry.
     *
     * Gets reset to a copy of entry on cleanupAfterSave()
     */
    SearchResultEntry searchResultEntry

    /**
     * UnboundID Entry object.
     *
     * Gets reset to searchResultEntry.duplicate() by discard().
     */
    Entry entry

    /**
     * The base DN which was used for searching for this entry. It is
     * important that we have this because we need it when we save or
     * delete the entry, i.e., it is critical that we map this entry to its
     * source.
     */
    String baseDN

    /**
     * Simple map for keeping track of whether or not an attribute has changed.
     * We keep this up-to-date as well as modifications because it is a lot
     * easier to inspect this for changes when calling isDirty(attribute) than
     * looping through the modifications array.
     *
     * Gets reset by discard() and cleanupAfterSave().
     */
    private Map mods = [:]

    /**
     * Simple map for keeping track of any errors which might occur when using
     * the object. For now, if directoryService.save(this) throws an error, it
     * will populate errors['save'] with the reason.
     *
     * Gets reset by discard() and cleanupAfterSave().
     *
     * Note: A future version of this class will have an errors object that
     * implements the Spring Errors interface, so the error handling will
     * change.
     */
    Map errors = [:]

    /**
     * Holds the actual directory modifications.
     *
     * Gets reset by discard() and cleanupAfterSave().
     */
    private List<Modification> modifications = []

    /**
     * Constructs a new DirectoryServiceEntry object by passing in
     * an UnboundID SearchResultEntry. It then takes the SearchResultEntry
     * and calls {@code duplicate()} on it so that we end up with
     * an Entry object which and then be modified.
     *
     * @param searchResultEntry     The search result entry that will
     * be used in this object.
     * @param baseDN                The baseDN which was used to get the
     * connection to the appropriate directory.
     */
    DirectoryServiceEntry(SearchResultEntry searchResultEntry,
        String baseDN) {
        this.searchResultEntry = searchResultEntry
        this.entry = searchResultEntry.duplicate()
        this.baseDN = baseDN
    }

    /**
     * Any property which is called on this class is passed as a
     * getAttributeValue() method call to the {@code entry} object which is
     * set on this class. If the {@code entry} object is not set, the method
     * throws a {@code MissingPropertyException}.
     *
     * This method detects if the attribute name is 'dn', and if it is, it
     * calls {@code entry.getDN()} instead of {@code entry.getAttributeValue()}.
     */
    def propertyMissing(String name) {
        if (entry) {
            if (name == 'dn') {
                return entry.getDN()
            }
            else {
                return entry.getAttributeValue(name)
            }
        }
        else {
            throw new MissingPropertyException(name)
        }
    }

    /**
     * This propertyMissing gets invoked when a value is supplied with the
     * property name, so we override it so that we can set values in the
     * entry object. We also set the mods map, and update the modifications
     * List.
     */
    def propertyMissing(String name, value) {
        if (entry) {
            mods[name] = value
            if (value) {
                if (value instanceof String || value instanceof String[]) {
                    entry.setAttribute(name, value)
                }
                else {
                    entry.setAttribute(name,
                        value.toArray(new String[value.size()]))
                }
            }
            else {
                if (entry.getAttributeValue(name)) {
                    entry.removeAttribute(name)
                }
            }
            // Now update the modifications list
            /*
            if (value instanceof String || value instanceof String[]) {
                updateModifications(name, value)
            }
            else {
                updateModifications(name,
                    value?.toArray(new String[value ? value.size(): 1]))
            }
            */
            updateModifications()
        }
    }

    /**
     * Intercepts the following methods:
     *
     * <ul>
     *   <li>getAttributeValues()</li>
     *   <li>&lt;attribute name&gt;Values()</li>
     *   <li>&lt;attribute name&gt;AsDate()</li>
     *   <li>&lt;attribute name&gt;AsBoolean()</li>
     *   <li>&lt;attribute name&gt;AsLong()</li>
     *   <li>dn()</li>
     * </ul>
     *
     * These need to be called as methods instead of properties because with
     * LDAP you never know if someone is going to name their attribute with one
     * of these names at the end, so it is safest to force them to be methods.
     */
    def methodMissing(String name, args) {
        if (entry) {
            if (name.matches(/^getAttributeValues?$/)) {
                return entry.invokeMethod(name, args)
            }
            else if (name.matches(/^(\w+)Values?$/)) {
                return entry.getAttributeValues(name - 'Values')
            }
            else if (name.matches(/^(\w+)?AsDate$/)) {
                return entry.getAttributeValueAsDate(name - 'AsDate')
            }
            else if (name.matches(/^(\w+)?AsBoolean$/)) {
                return entry.getAttributeValueAsBoolean(name - 'AsBoolean')
            }
            else if (name.matches(/^(\w+)?AsLong$/)) {
                return entry.getAttributeValueAsLong(name - 'AsLong')
            }
            else {
                if (name == 'dn') {
                    return entry.getDN()
                }
                else {
                    return entry.getAttributeValue(name)
                }
            }
        }
        else {
            throw new MissingMethodException(name, delegate, args)
        }
    }

    /**
     * Discards any changes made to the object. Reinitializes both the
     * {@code mods} map and the {@code modifications} array, and sets
     * the entry as a duplicate of the searchResultEntry.
     */
    def discard() {
        mods = [:]
        errors = [:]
        modifications = []
        entry = searchResultEntry.duplicate()
    }

    /**
     * Similar to discard(), but instead of resetting the entry, it recreates
     * the searchResultEntry from the entry object, as we want the state of
     * this object to be what was just saved to the directory.
     */
    def cleanupAfterSave() {
        mods = [:]
        errors = [:]
        modifications = []
        searchResultEntry =
            new SearchResultEntry(entry, searchResultEntry.getControls())
    }

    /**
     * Checks to see if this object has been modified. You can also pass in
     * an optional attribute name to check to see if just that one attribute
     * has been modified.
     *
     * Note: This method is named isDirty to match GORM.
     *
     * @param attribute         Optional attribute name to check to see if it
     * has been modified.
     * @return {@code true} if this object (or the supplied attribute) has been
     * modified, and {@code false} otherwise.
     */
    def isDirty(attribute=null) {
        if (mods) {
            if (attribute) {
                return mods[attribute] ? true : false
            }
            return true
        }
        return false
    }

    /**
     * Note for 0.6.1: There is no reason to maintain this since the UnboundID SDK has
     * a Entry.diff() method that does this for us! Plus, this was not a
     * documented API, so I feel OK removing it.
     *
     * Updates the modifications List with the UnboundID Modification
     * that will be used when the entry is updated in the directory.
     *
     * This is called by the propertyMissing(String name, value) method, so
     * there is no need to call it directly. However, if you want to
     * short-circuit propertyMissing and update a bunch of attributes and then
     * immediately save, you can use this method directly. But understand that
     * the mods map will not be updated, and isDirty() will no know that you
     * have modified the object.
     *
     * @param name              The name of the attribute for the modification.
     * @param values            One or more String values to apply to the
     * supplied attribute name.
     * @see #propertyMissing(String name, value)
     */
    /*
    def updateModifications(String name, String... values) {
        if (searchResultEntry) {
            def exists = false
            modifications.eachWithIndex() {obj, i ->
                if (obj.getAttributeName()?.equalsIgnoreCase(name)) {
                    exists = true
                    if (values) {
                        modifications.set(i, new Modification(
                                ModificationType.REPLACE, name, values))
                    }
                    else {
                        modifications.set(i, new Modification(
                            ModificationType.DELETE, name))
                    }
                }
            }
            if (!exists) {
                if (values?.size() > 0) {
                    modifications.add(new Modification(
                        ModificationType.REPLACE, name, values))
                }
                else {
                    modifications.add(new Modification(
                        ModificationType.DELETE, name))
                }
            }
        }
    }
    */

    /**
     * Updates the {@code modifications} list by calling the UnboundID
     * Entry.diff() method against the {@code searchResultEntry} and the
     * modified {@code entry} objects. It uses the "REPLACE" method for
     * doing modifications instead of the "DELETE"/"ADD" method.
     */
    def updateModifications() {
        modifications = Entry.diff(searchResultEntry, entry, true, false)
    }
}
