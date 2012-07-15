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

import java.security.GeneralSecurityException
import java.util.LinkedList
import java.util.ArrayList

import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.FailoverServerSet
import com.unboundid.ldap.sdk.Filter as LDAPFilter
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPConnectionOptions
import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.LDAPSearchException
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.ModificationType
import com.unboundid.ldap.sdk.ResultCode
import com.unboundid.ldap.sdk.RoundRobinServerSet
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.util.LDAPSDKUsageException
import com.unboundid.util.ssl.SSLUtil
import com.unboundid.util.ssl.TrustAllTrustManager

import org.apache.log4j.Logger

/**
 *
 *
 *
 * @author Lucas Rockwell
 */
class DirectoryService {

    static transactional = false

    /* Logger for this class. */
    Logger log = Logger.getLogger(DirectoryService.class);

    /* grailsApplication, which is necessary for configuration info. */
    def grailsApplication

    /* Holds the current LDAPConnection. */
    LDAPConnection conn = null
    
    def serverSets = [:]
    
    /**
     * Catches the following methods:
     *
     * <ul>
     *   <li>findAll*Where(Map of attribute/value pairs), where * is in the 
     *     ldap.dit plural Map.</li>
     *   <li>find*Where(Map of attribute/value pairs), where * is in the 
     *     ldap.dit.schema singular Map.</li>
     *   <li>findAll*UsingFilter(Filter), where * is in the ldap.dit 
     *     plural Map.</li>
     *   <li>find*UsingFilter(Filter), where * is in the ldap.dit singular 
     *     Map.</li>
     * </ul>
     */
    def methodMissing(String name, args) {
        if (args && (args[0] instanceof Map)) {
            // If it is a find*, we want to inspect it...
            if (name.matches(/^find(\w+)*$/)) {
                // Check for find*Where (plural)
                def method = grailsApplication.config.ds.dit.find {
                    name.matches(/^find${it.value.plural.capitalize()}Where*$/)
                }
                if (method) {
                    return findAll(method.key, args[0])
                }
                else {
                    // Didn't find plural, so check for singular
                    method = grailsApplication.config.ds.dit.find {
                        name.matches(/^find${it.value.singular.capitalize()}Where*$/)
                    }
                    if (method) {
                        return find(method.key, args[0])
                    }
                    else {
                        throw new MissingMethodException(name, delegate, args)
                    }
                }
            }
        }
        else if (args && (args[0] instanceof LDAPFilter)) {
            def method = grailsApplication.config.ds.dit.find {
                name.matches(/^find${it.value.plural.capitalize()}Where*$/)
            }
            if (method) {
                // Return the key of the "method", as it is the baseDN
                return findAllUsingFilter(method.key, args[0])
            }
            else {
                throw new MissingMethodException(name, delegate, args)
            }
        }
        else {
            throw new MissingMethodException(name, delegate, args)
        }
    }
    
    /**
     * Performs a find based on the passed in {@code baseDn} and {@code args}
     * and returns the first entry found. If no entry is found, it returns null.
     *
     * @param baseDN        The base DN to use in the search.
     * @param args          The Map of key:value pairs which will be turned
      * into an AND filter.
     * @return A LdapServiceEntry, which is the first result of the resulting
     * search.
     */
    def find(String baseDn, Map args) {
        List<SearchResultEntry> entries = searchUsingFilter(baseDn, 
            andFilterFromArgs(args).toString())
        if (entries.size() > 0) {
            return new DirectoryServiceEntry(entries.get(0))
        }
        return null;
        
    }
    
    /**
     * Performs a find based on the passed in {@code baseDn} and {@code args}
     * and returns a List of all of the results.
     *
     * @param baseDN        The base DN to use in the search.
     * @param args          The Map of key:value pairs which will be turned
     * into an AND filter.
     * @return List of LdapServiceEntry objects.
     */
    def findAll(String baseDn, Map args) {
        List<SearchResultEntry> entries = searchUsingFilter(baseDn, 
            andFilterFromArgs(args).toString())
        def list = []
        entries.each {
            list.add(new DirectoryServiceEntry(it))
        }
        return list
    }
    
    /**
     * Performs a find based on the passed in {@code baseDn} and {@code filter}
     * and returns a List of all of the results.
     *
     * @param baseDN        The base DN to use in the search.
     * @param filter        The com.unboundid.ldap.sdk.Filter to use in the
     * search request
     * @return List of LdapServiceEntry objects.
     */
    def findAllUsingFilter(String baseDn, LDAPFilter filter) {
        List<SearchResultEntry> entries = searchUsingFilter(baseDn,
            filter.toString())
        def list = []
        entries.each {
            list.add(new DirectoryServiceEntry(it))
        }
        return list
    }
    
    /**
     * Creates an AND filter from the passed in args.
     */
    def andFilterFromArgs(Map args) {
        
        LDAPFilter filter
                
        if (args.length == 1) {
            filter = LDAPFilter.createEqualityFilter(args.key, args.value)
            return filter
        }
        else {
            def filters = []
            args.each { arg ->
                filters.push(
                    LDAPFilter.createEqualityFilter(arg.key, arg.value))
            }
            filter = LDAPFilter.createANDFilter(filters)
            return filter
        }

    }

    /**
     * Tries to create an LDAPFilter from the passed in string.
     *
     * @param filterString          String to be converted into an
     * {@code LDAPFilter}.
     *
     * @return ldap filter.
     */
    def createFilter(String filterString) {
        try {
            def filter = LDAPFilter.create(filterString)
            return filter
        }
        catch (LDAPException e) {
            log.error("Error creating filter from string: ${e.getMessage()}")
        }
        return filterString
    }

    def serverSetForSourceName(sourceName) {
        if (serverSets[sourceName]) {
            return serverSets[sourceName]
        }
        else {
            def source = grailsApplication.config.ds.sources[sourceName]
            def serverSet = serverSetForSource(
                source.address,
                source.port,
                source.useSSL,
                source.trustSSLCert
            )
            serverSets[sourceName] = serverSet
            return serverSet
        }
    }
    
    def serverSetForSource(final String addresses, final String ports,
        boolean useSSL, boolean trustSSLCert) {
        final String[] addressesArray   = addresses.split(",")
        final String[] portsStringArray = ports.split(",")
        
        int[] portsIntArray = new int[portsStringArray.length]
        
        if (portsStringArray.length == 1) {
            portsIntArray[0] = Integer.parseInt(portsStringArray[0])
        }
        else {
            for (int i = 0; i < portsStringArray.length; i++) {
                portsIntArray[i] = Integer.parseInt(portsStringArray[i])
            }
        }
        
        LDAPConnectionOptions options = new LDAPConnectionOptions()
        //options.setAutoReconnect(autoReconnect)
        //options.setFollowReferrals(followReferrals)
        try {
            if (useSSL) {
                SSLUtil sslUtil;
                if (trustSSLCert) {
                    sslUtil = new SSLUtil(new TrustAllTrustManager())
                }
                else {
                    sslUtil = new SSLUtil();
                }
                return new FailoverServerSet(
                    addressesArray, portsIntArray, sslUtil.createSSLSocketFactory(), options)
            }
            else {
                return new FailoverServerSet(addressesArray, portsIntArray, options)
            }
        }
        catch (GeneralSecurityException gse) {
            log.error "Error connecting via SSL: " + gse.toString()
            return null
        }
    }

    /**
     * Get a connection from the {@code serverSet} and then bind. Right now
     * it assumes it is going to connect and then it just binds. This should
     * really throw an exception...
     *
     * @return LDAPConnection the authenticated LDAPConnection
     */
    def connection(String base) {
        def sourceName = sourceNameFromBase(base)
        def serverSet = serverSetForSourceName(sourceName)
        LDAPConnection conn = serverSet?.getConnection()
        def source = grailsApplication.config.ds.sources[sourceName]
        conn?.bind(source.bindDN, source.bindPassword)
        return conn
    }

    /**
     * Returns the {@code ldap.sources} source based on the passed in
     * {@code base} which is a key in the {@code ldap.dit} Map.
     *
     */
    def sourceNameFromBase(String base) {
        def dit = grailsApplication.config.ds.dit[base]
        return dit.source
    }
    
    def sourceFromBase(String base) {
        def dit = grailsApplication.config.ds.dit[base]
        return grailsApplication.config.ds.sources[dit.source]
    }

    /**
     * Returns a new List<SearchResultEntry> with the results of
     * of the search. Returns an empty LinkedList<SearchResultEntry> if
     * any exceptions are thrown.
     *
     * Note: The search scope is SearchScope.SUBORDINATE_SUBTREE.
     *
     * @param {@code attribute} - attribute to search for
     * @param {@code value} - the value of {@code attribute}
     * @param {@code base} - the search base
     * @return List<SearchResultEntry> - a List of SearchResultEntry objects
     */
    private List<SearchResultEntry> search(final String base,
        final String attribute, final String value) {
        String filter = "(${attribute}=${cleanString(value)})"
        return searchUsingFilter(base, filter)
    }

    private List<SearchResultEntry> searchUsingFilter(final String base,
        final String filter) {

        SearchRequest searchRequest = new SearchRequest(
                base, 
                SearchScope.SUB, 
                filter,
                "*")
        def conn = connection(base)
        try {
            SearchResult results = conn.search(searchRequest)
            List<SearchResultEntry> entries = results.getSearchEntries()
            return entries
        }
        catch (LDAPSearchException lse) {
            log.error "Exception while searching: ${lse.getMessage()}"
            return new LinkedList<SearchResultEntry>()
        }
        finally {
            conn?.close()
        }
    }

    /**
     * Determines if today is between the {@code start} and {@code expires}.
     * It first checks to make sure that {@code start} and {@code expires}
     * are not null.
     *
     * @param {@code start} - the start date
     * @param {@code expires} - the expires date
     * @return boolean - true if start and expires are not null and today is
     * between start and expires, and false otherwise.
     */
    private boolean withinDates(final Date start, final Date expires) {
        Date now = new Date();
        if (expires != null && start != null &&
            start.before(now) && expires.after(now)) {
            return true;
        }
        return false;
    }

}