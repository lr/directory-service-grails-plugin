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

import java.util.LinkedList
import java.util.ArrayList

import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.ModificationType
import com.unboundid.ldap.sdk.SearchResultEntry

import org.apache.log4j.Logger

/**
 *
 *
 *
 * @author Lucas Rockwell
 */
class DirectoryServiceEntry {
    
    /* The original searchResultEntry */
    def SearchResultEntry searchResultEntry
    
    /* UnboundID Entry object */
    def Entry entry
    
    /* Keeps track of whether or not an attribute has changed. We keep this
       up-to-date as well as modifications because it is a lot easier to
       inspect this for changes when calling isDirty(attribute) than
       looping through the modifications array. */
    def mods = [:]
    
    /* Holds the actual directory modifications */
    ArrayList<Modification> modifications = new ArrayList<Modification>()
    
    /**
     * Constructs a new DirectoryServiceEntry object by passing in
     * an UnboundID SearchResultEntry. It then takes the SearchResultEntry
     * and calls {@code duplicate()} on it so that we end up with 
     * an Entry object which and then be modified.
     *
     * @param searchResultEntry     The search result entry that will
     * be used in this object.
     */
    def DirectoryServiceEntry(SearchResultEntry searchResultEntry) {
        this.searchResultEntry = searchResultEntry
        this.entry = searchResultEntry.duplicate()
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
     * For modifying the object.
     */
    def propertyMissing(String name, value) {
        println "value in propertyMissing: ${value}"
        mods[name] = value
        
        if (value) {
            if (value instanceof String || value instanceof String[]) {
                entry.setAttribute(name, value)
            }
            else {
                entry.setAttribute(name, value.toArray(new String[value.size()]))
            }
        }
        else {
            if (entry.getAttributeValue(name)) {
                entry.remoteAttribute(name)
            }
        }
        // Now update the modifications list
        if (value instanceof String || value instanceof String[]) {
            updateModifications(name, value)
        }
        else {
            updateModifications(name, value.toArray(new String[value.size()]))
        }
    }
    
    def methodMissing(String name, args) {
        if (entry) {
            if (name.matches(/^getAttributeValues?$/)) {
                return entry.invokeMethod(name, args)
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
     * {@code mods} map and the {@code modifications} array.
     */
    def discard() {
        mods = [:]
        modifications = new ArrayList<Modification>()
    }
    
    /**
     *
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
    
    def updateModifications(String name, String... values) {
        if (searchResultEntry) {
            def exists = false
            modifications.eachWithIndex() {obj, i ->
                if (obj.getAttributeName()?.equalsIgnoreCase(name)) {
                    exists = true
                    if (values) {
                        println "we have values..."
                        modifications.set(i, new Modification(
                                ModificationType.REPLACE, name, values))
                    }
                    else {
                        println "nope..."
                        modifications.set(i, new Modification(
                            ModificationType.DELETE, name))
                    }
                }
            }
            if (!exists) {
                if (values) {
                    println "values..."
                    modifications.add(new Modification(
                        ModificationType.REPLACE, name, values))
                }
                else {
                    println "no values..."
                    modifications.add(new Modification(
                        ModificationType.DELETE, name))
                }
            }
        }
    }

}