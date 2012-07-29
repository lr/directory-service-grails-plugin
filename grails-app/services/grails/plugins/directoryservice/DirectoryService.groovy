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
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.LDAPSearchException
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.ModificationType
import com.unboundid.ldap.sdk.ResultCode
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.util.ssl.SSLUtil
import com.unboundid.util.ssl.TrustAllTrustManager

import org.apache.log4j.Logger

/**
 * DirectoryService is a Grails Service that makes interacting with an LDAP
 * directory very easy. It is loosely based on GORM. However, because it is one
 * service class (as opposed to numerous concrete domain classes which is the
 * case with GORM) that interacts with a any part of a directory tree, and even
 * many different directory servers at once, you invoke an operation by
 * specifying the directory object to interact with as part of the method
 * signature.
 *
 * Given the following configuration:
 * <pre>
 *   grails.plugins.directoryservice.sources = [
 *       'directory':[
 *           'address': 'server1,server2',
 *           'port': ' 636 ,636',
 *           'useSSL': true,
 *           'trustSSLCert': true,
 *           'followReferrals': true,
 *           'bindDN': 'cn=Some BindDN',
 *           'bindPassword': 'password'
 *       ],
 *       'ad':[
 *           'address': 'net1,net2',
 *           'port': '3269,3269',
 *           'useSSL': true,
 *           'trustSSLCert': true,
 *           'followReferrals': true,
 *           'bindDN': 'cn=AD BindDN',
 *           'bindPassword': 'password'
 *       ]
 *   ]
 *   
 *   grails.plugins.directoryservice.dit = [
 *       'ou=people,dc=someu,dc=edu':[
 *           'singular':'person',
 *           'plural':'people',
 *           'rdnAttribute':'uid',
 *           'source':'directory'
 *       ],
 *       'ou=departments,dc=someu,dc=edu':[
 *           'singular':'department',
 *           'plural':'departments',
 *           'rdnAttribute':'ou',
 *           'source':'directory'
 *       ],
 *       'ou=accounts,dc=someu,dc=edu':[
 *           'singular':'account',
 *           'plural':'accounts',
 *           'rdnAttribute':'cn',
 *           'source':'ad'
 *       ]
 *  ]
 * </pre>
 *
 * You can search the branches of the directories as follows:
 *
 * <pre>
 *   // People
 *   // Find 1 person matching params
 *   def person = directoryService.findPersonWhere('uid': '1234')
 *   // Find all people matching params
 *   def people = directoryService.findPeopleWhere('sn': 'nguyen')
 *
 *   // Departments
 *   // Find 1 department matching params
 *   def department = directoryService.findDepartmentWhere('departmentNumber': '54321')
 *   // Find all departments matching params
 *   def departments = directoryService.findDepartmentsWhere('someuEduDepartmentParent': '54321')
 *
 *   // AD Accounts (which are in a different directory server)
 *   // Find 1 account matching params
 *   def account = directoryService.findAccountWhere('sAMAccountName': 'lrockwell')
 *   // Find all accounts matching the provided filter
 *   def filter = directoryService.createFilter('(&(sAMAccountName=wa*))')
 *   def accounts = directoryService.findAccountsWhere(filter)
 * </pre>
 *
 * Find more examples in the official documentation.
 *
 * @author Lucas Rockwell
 */
class DirectoryService {

    static transactional = false

    /* Logger for this class. */
    private Logger log = Logger.getLogger(DirectoryService.class);

    /* grailsApplication, which is necessary for configuration info. */
    def grailsApplication

    /* Holds the current LDAPConnection. */
    private LDAPConnection conn = null
    
    /* Map that holds any ServerSet objects that are created during the duration
       of this object's life. */
    private Map serverSets = [:]
    
    /**
     * Catches the following methods:
     *
     * <ul>
     *   <li>find&lt;plural&gt;Where(Map of attribute/value pairs)</li>
     *   <li>find&lt;singular&gt;Where(Map of attribute/value pairs)</li>
     *   <li>find&lt;plural&gt;UsingFilter(Filter)</li>
     *   <li>get&lt;singular&gt;(RDN attribute value)</li>
     * </ul>
     */
    def methodMissing(String name, args) {
        if (args) {
            def method
            if (args[0] instanceof Map) {
                // If it is a find*, we want to inspect it...
                if (name.matches(/^find(\w+)*$/)) {
                    // Check for find*Where (plural)
                    method = grailsApplication.config.grails.plugins.directoryservice.dit.find {
                        name.matches(/^find${it.value.plural?.capitalize()}Where*$/)
                    }
                    if (method) {
                        return findAll(method.key, args[0])
                    }
                    else {
                        // Didn't find plural, so check for singular
                        method = grailsApplication.config.grails.plugins.directoryservice.dit.find {
                            name.matches(/^find${it.value.singular?.capitalize()}Where*$/)
                        }
                        if (method) {
                            return find(method.key, args[0])
                        }
                    }
                }
            }
            else if (args[0] instanceof LDAPFilter) {
                method = grailsApplication.config.grails.plugins.directoryservice.dit.find {
                    name.matches(/^find${it.value.plural?.capitalize()}Where*$/)
                }
                if (method) {
                    return findAllUsingFilter(method.key, args[0])
                }
            }
            else if (args[0] instanceof String) {
                if (name.matches(/^get(\w+)$/)) {
                    method = grailsApplication.config.grails.plugins.directoryservice.dit.find {
                        name.matches(/^get${it.value.singular?.capitalize()}$/)
                    }
                    if (method) {
                        def dit = grailsApplication.config.grails.plugins.directoryservice.dit[method.key]
                        return find(method.key, [(dit.rdnAttribute):args[0]])
                    }
                }
            }
        }
        
        throw new MissingMethodException(name, delegate, args)
    }
    
    /**
     * Performs a find based on the passed in baseDN and args and returns
     * the first entry found. If no entry is found, it returns null.
     *
     * @param baseDN        The base DN to use in the search.
     * @param args          The map of key:value pairs which will be turned
      * into an AND filter.
     * @return A DirectoryServiceEntry, which is the first result of the resulting
     * search.
     */
    def find(String baseDN, Map args) {
        List<SearchResultEntry> entries = searchUsingFilter(baseDN, 
            andFilterFromArgs(args).toString())
        if (entries.size() > 0) {
            return new DirectoryServiceEntry(entries.get(0), baseDN)
        }
        return null;
        
    }
    
    /**
     * Performs a find based on the passed in {@code baseDN} and {@code args}
     * and returns a List of all of the results.
     *
     * @param baseDN        The base DN to use in the search.
     * @param args          The map of key:value pairs which will be turned
     * into an AND filter.
     * @return List of LdapServiceEntry objects.
     */
    def findAll(String baseDN, Map args) {
        List<SearchResultEntry> entries = searchUsingFilter(baseDN, 
            andFilterFromArgs(args).toString())
        def list = []
        entries.each {
            list.add(new DirectoryServiceEntry(it, baseDN))
        }
        return list
    }
    
    /**
     * Performs a find based on the passed in {@code baseDN} and {@code filter}
     * and returns a List of all of the results.
     *
     * @param baseDN        The base DN to use in the search.
     * @param filter        The com.unboundid.ldap.sdk.Filter to use in the
     * search request
     * @return List of LdapServiceEntry objects.
     */
    def findAllUsingFilter(String baseDN, LDAPFilter filter) {
        List<SearchResultEntry> entries = searchUsingFilter(baseDN,
            filter.toString())
        def list = []
        entries.each {
            list.add(new DirectoryServiceEntry(it, baseDN))
        }
        return list
    }
    
    /**
     * Creates an AND filter from the passed in map of args.
     *
     * @param args          The arguments to use for creating the
     * filter.
     * @return An AND LDAPFilter that was created from the passed in map of args.
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
     * @param filterString   String to be converted into an LDAPFilter.
     * @return An LDAPFilter that was created from the filterString.
     */
    def createFilter(String filterString) {
        try {
            def filter = LDAPFilter.create(filterString)
            return filter
        }
        catch (LDAPException e) {
            log.error "Error creating filter from string: ${e.getMessage()}"
        }
        return filterString
    }

    /**
     * Saves the passed in DirectoryServiceEntry to the directory that
     * is associated with the given DN. This could get tricky if you have
     * objects from competing directories that might have the same parent DN
     * of the passed in object. But for now, this is the only way I know how to
     * do this.
     *
     * Note: A future version of DirectoryServiceEntry will have an errors
     * object that implements the Spring Errors interface, so the error handling
     * will change.
     *
     * @param entry         The DirectoryServiceEntry to save.
     * @return {@code true} on success, {@code false} otherwise.
     * @throws 
     */
    def save(DirectoryServiceEntry entry) {
        def conn = connection(entry?.baseDN)
        if (conn) {
            try {
                conn.modify(entry?.entry?.getDN(), entry.modifications)
                entry.cleanupAfterSave()
                return true
            }
            catch (LDAPException e) {
                log.error "Could not save/modify ${entry?.entry?.getDN()}: ${e.getMessage()}"
                entry.errors['save'] = e.getMessage()
            }
        }
        else {
            def reason = 
                "Could not save/modify because a connection to the directory server could not be established. "
            entry.errors['save'] = reason
            log.error reason
        }
        return false
    }

    /**
     * Returns the ServerSet associated with the provided sourceName.
     * DirectoryService uses a FailoverServerSet.
     *
     * Since this there may be numerous directory servers specified as part
     * of the configuration for the DirectoryService, the method stashes them
     * away in an internal map that is consulted each time a request is made.
     * If there is already a ServerSet associated with the provided source,
     * it returns that. If not, it creates a new one, stores it for later,
     * and then returns it.
     *
     * @param sourceName            The name of the source to use for creating
     * the ServerSet.
     * @return A ServerSet which can be used for getting a connection.
     * @see #serverSetForSource(String addresses, String ports, boolean useSSL, boolean trustSSLCert, boolean followReferrals=true)
     */
    def serverSetForSourceName(sourceName) {
        if (serverSets[sourceName]) {
            return serverSets[sourceName]
        }
        else {
            def source = grailsApplication.config.grails.plugins.directoryservice.sources[sourceName]
            def serverSet = serverSetForSource(
                source.address,
                source.port,
                source.useSSL,
                source.trustSSLCert,
                source.followReferrals
            )
            serverSets[sourceName] = serverSet
            return serverSet
        }
    }
    
    /**
     * Creates a new FailoverServerSet from the passed in args.
     *
     * @param addresses         One or more addresses, separated by a "," if
     * more than one.
     * @param ports             One or more ports, separated by a "," if more
     * than one. The number of ports and addresses must match.
     * @param useSSL            Whether or not to use SSL for the connection.
     * @param trustSSLCert      Whether or not to implicitly trust the SSL
     * certificate. If this is {@code false}, then your JVM must trust the SSL
     * certificate.
     * @param followReferrals   Whether or not to follow referrals. This defaults
     * to true.
     * @return A FailoverServerSet which is created based on the passed in args.
     */
    def serverSetForSource(String addresses, String ports,
        boolean useSSL, boolean trustSSLCert, boolean followReferrals=true) {
        
        final String[] addressesArray = 
            addresses?.replaceAll(' ', '')?.split(',')
            
        final String[] portsStringArray = 
            ports?.replaceAll(' ', '')?.split(',')
        
        int[] portsIntArray = new int[portsStringArray.length]
        
        if (portsStringArray.length == 1) {
            portsIntArray[0] = Integer.parseInt(portsStringArray[0])
        }
        else {
            portsStringArray.eachWithIndex() { obj, i ->
                portsIntArray[i] = Integer.parseInt(obj)
            }
        }
        
        LDAPConnectionOptions options = new LDAPConnectionOptions()
        options.setFollowReferrals(followReferrals)
        
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
     * Get a connection from the ServerSet that is associated with the passed
     * in base.
     * 
     * This method also performs the bind to the server using the bindDN and
     * bindPassword associated with the source for provided base. If there are
     * is no bindDN in the source, then the connection is returned
     * unauthenticated, i.e., it is an anonymous bind.
     *
     * @param base          The search base that will be used to look
     * up the source map, and then the corresponding serverSet for that source.
     * @return The authenticated LDAPConnection.
     */
    def connection(String base) {
        def sourceName = sourceNameFromBase(base)
        def serverSet = serverSetForSourceName(sourceName)
        LDAPConnection conn = serverSet?.getConnection()
        def source = grailsApplication.config.grails.plugins.directoryservice.sources[sourceName]
        // If there is a bindDN, then bind, otherwise, treat as 
        // anonymous.
        if (source.bindDN) {
            conn?.bind(source.bindDN, source.bindPassword)
        }
        return conn
    }

    /**
     * Returns the {@code ldap.sources} Map based on the passed in
     * base, which should be a key in the {@code ldap.dit} Map.
     *
     * @param base          The search base that will be used to look
     * up the source map.
     * @return The map which contains the source for this base.
     */
    def sourceNameFromBase(String base) {
        def dit = grailsApplication.config.grails.plugins.directoryservice.dit[base]
        return dit.source
    }
    
    /**
     * Takes in the passed base and returns the corresponding
     * source from grails.plugins.directoryservice.source.
     *
     * @param base      The search base which will be used as the key
     * for the lookup in the grails.plugins.directoryservice.source.
     * @return The Map which corresponds to the source for the provided
     * base.
     */
    def sourceFromBase(String base) {
        def dit = grailsApplication.config.grails.plugins.directoryservice.dit[base]
        return grailsApplication.config.grails.plugins.directoryservice.sources[dit.source]
    }

    /**
     * Returns a list of SearchResultEntry objects. Returns an empty 
     * list if any exceptions are thrown. This method returns
     * #searchUsingFilter(final String base, final String filter, String... attrs='*')
     *
     * @param attribute     The attribute to search for.
     * @param value         The value of attribute.
     * @param base          The search base.
     * @param attrs         The attributes to return. By default it will return
     * everything the bind has access to.
     * @return A List of SearchResultEntry objects.
     * @see #searchUsingFilter(final String base, final String filter, String... attrs='*')
     */
    def search(String base, String attribute, String value,
        String... attrs='*') {
        def filter = createFilter("(${attribute}=${value})")
        return searchUsingFilter(base, filter.toString())
    }
    
    /**
     * Returns a list of SearchResultEntry objects. Returns an empty 
     * list if any exceptions are thrown.
     *
     * It performs the search using a scope of SearchScope.SUB.
     *
     * @param attribute     The attribute to search for.
     * @param value         The value of attribute.
     * @param base          The search base.
     * @param attrs         The attributes to return. By default it will return
     * everything the bind has access to.
     * @return A List of SearchResultEntry objects.
     */
    def searchUsingFilter(final String base, final String filter,
        String... attrs='*') {

        SearchRequest searchRequest = new SearchRequest(
                base, 
                SearchScope.SUB, 
                filter,
                attrs)
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

}