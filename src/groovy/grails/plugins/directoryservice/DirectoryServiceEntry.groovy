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
    
    def SearchResultEntry searchResultEntry
    
    def mods = []
    
    def DirectoryServiceEntry(SearchResultEntry searchResultEntry) {
        this.searchResultEntry = searchResultEntry
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
        if (searchResultEntry) {
            if (name == 'dn') {
                return searchResultEntry.getDN()
            }
            else {
                return searchResultEntry.getAttributeValue(name)
            }
        }
        else {
            throw new MissingPropertyException(name)
        }
    }
    
    def methodMissing(String name, args) {
        if (searchResultEntry) {
            if (name.matches(/^getAttributeValues?$/)) {
                return searchResultEntry.invokeMethod(name, args)
            }
            else if (name.matches(/^(\w+)?AsDate$/)) {
                return searchResultEntry.getAttributeValueAsDate(name - 'AsDate')
            }
            else if (name.matches(/^(\w+)?AsBoolean$/)) {
                return searchResultEntry.getAttributeValueAsBoolean(name - 'AsBoolean')
            }
            else if (name.matches(/^(\w+)?AsLong$/)) {
                return searchResultEntry.getAttributeValueAsLong(name - 'AsLong')
            }
            else {
                if (name == 'dn') {
                    return searchResultEntry.getDN()
                }
                else {
                    return searchResultEntry.getAttributeValue(name)
                }
            }
        }
        else {
            throw new MissingMethodException(name, delegate, args)
        }
    }

}