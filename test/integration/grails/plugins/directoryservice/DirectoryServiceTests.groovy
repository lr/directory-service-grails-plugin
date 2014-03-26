package grails.plugins.directoryservice

import grails.test.mixin.*
import org.junit.*

import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.Filter as LDAPFilter
import com.unboundid.ldap.sdk.LDAPConnection

import grails.plugins.directoryservice.listener.InMemoryDirectoryServer

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class DirectoryServiceTests extends GroovyTestCase {

    /* DirectoryService to use for all tests. */
    def directoryService
    
    /* Used for comparison. */
    def peopleBaseDn = 'ou=people,dc=someu,dc=edu'
    
    /* Used for testing the fake AD server. */
    def accountsBaseDn = 'ou=accounts,dc=someu,dc=edu'
    
    /* Used for testing the Economics accounts branch. */
    def accountsEconomicsBaseDn = 'ou=Economics,ou=accounts,dc=someu,dc=edu'
    
    def dirInMemServer
    def adInMemServer
    
    /**
     * Set up by creating an DirectoryService and setting grailsApplication config
     * based on ConfigurationHolder.config. Then set up the UnboundID
     * InMemoryServer.
     *
     * ~/bin/unboundid-ldapsdk-2.3.4-se/tools/in-memory-directory-server -b "dc=someu,dc=edu" -p 33389 -l test/ldif/directory.ldif -A -D "cn=directory manager" -w "password"
     */
    protected void setUp() {
        super.setUp()
        
        def dirConfig = grails.util.GrailsConfig.grails.plugins.directoryservice.sourcesForInMemoryServer['directory']
        def adConfig = grails.util.GrailsConfig.grails.plugins.directoryservice.sourcesForInMemoryServer['ad']

        /*
        dirInMemServer = new InMemoryDirectoryServer(
            "dc=someu,dc=edu",
            dirConfig,
            "test/ldif/schema/directory-schema.ldif",
            "test/ldif/directory.ldif"
        )
        */
        adInMemServer = new InMemoryDirectoryServer(
            "dc=someu,dc=edu",
            adConfig,
            "test/ldif/schema/ad-schema.ldif",
            "test/ldif/accounts.ldif"
        )
    }
    
    protected void tearDown() {
        dirInMemServer?.shutDown()
        adInMemServer?.shutDown()
    }
    
    /*
     * Test the getting the source from the provided base.
     */
    void testSource() {
        def source = directoryService.sourceFromBase(peopleBaseDn)
        
        assertEquals source.address, 'localhost , localhost'
        assertEquals source.port, ' 11389 ,33389'
        assertFalse  source.useSSL
        assertTrue   source.trustSSLCert
        assertEquals source.bindDN, 'cn=Directory Manager'
        assertEquals source.bindPassword, 'password'
        
        source = directoryService.sourceFromBase(accountsBaseDn)
        assertEquals source.address, 'localhost'
        assertEquals source.port, '33268'
        assertFalse  source.useSSL
        assertTrue   source.trustSSLCert
        assertEquals source.bindDN, 'cn=AD Manager'
        assertEquals source.bindPassword, 'password'
    }
    
    /**
     * Test andFilterFromArgs to make sure it outputs an LDAPFilter.
     */
    void testAndFilterFromArgs() {
        def args = ['sn':'nguyen']
        def filter = directoryService.andFilterFromArgs(args)
        assertNotNull filter
        assert filter instanceof com.unboundid.ldap.sdk.Filter
        assertEquals filter.toString(), '(&(sn=nguyen))'
        
        args = ['sn':'smith', 'givenName':'sally']
        filter = directoryService.andFilterFromArgs(args)
        assertNotNull filter
        assert filter instanceof com.unboundid.ldap.sdk.Filter
        assertEquals filter.toString(), '(&(sn=smith)(givenName=sally))'
        
    }
    
    /**
     * Test the base find method itself. This will return the actual
     * entry itself because it is "find", and not "findAll".
     */
    void testFind() {
        def args = ['sn':'evans']
        def result = directoryService.findEntry(peopleBaseDn, args)
        
        assertNotNull result
        assert result instanceof grails.plugins.directoryservice.DirectoryServiceEntry
        assert result.entry instanceof com.unboundid.ldap.sdk.Entry
        
        // Test the UnboundID SearchResultEntry
        assertEquals result.getAttributeValue("sn"), 'Evans'
        // Test the LdapServiceEntry
        assertEquals result.sn, 'Evans'
        
    }
    
    /**
     * The the base find using an AD Economics OU. There are a lot
     * of Jills in AD, but only one in econ.
     */
    void testFindADOU() {
        def args = ['givenName':'jill']
        def result = directoryService.findEntry(accountsEconomicsBaseDn, args)
        
        assertNotNull result
        assert result instanceof grails.plugins.directoryservice.DirectoryServiceEntry
        assert result.entry instanceof com.unboundid.ldap.sdk.Entry
        
        // Test the UnboundID SearchResultEntry
        assertEquals result.getAttributeValue("sn"), 'Kannel'
        // Test the LdapServiceEntry
        assertEquals result.sn, 'Kannel'
        
    }
    
    /**
     * Test the base findAll method itself.
     */
    void testFindAll() {
        def args = ['sn':'James']
        def results = directoryService.findEntries(peopleBaseDn, args)
        
        assertNotNull results
        assert results[0] instanceof grails.plugins.directoryservice.DirectoryServiceEntry
        assert results[0].entry instanceof com.unboundid.ldap.sdk.Entry
        assertEquals results[0].getAttributeValue("sn"), 'James'
        assertEquals results.size(), 4
    }
    
    /**
     * The the base findAll using an AD Economics OU. There are a lot
     * of Jills in AD, but only one in econ.
     */
    void testFindAllADOU() {
        def args = ['givenName':'jill']
        def results = directoryService.findEntries(accountsEconomicsBaseDn, args)
        
        assertNotNull results
        assert results[0] instanceof grails.plugins.directoryservice.DirectoryServiceEntry
        assert results[0].entry instanceof com.unboundid.ldap.sdk.Entry
        
        assertEquals results.size(), 1
        
        // Test the UnboundID SearchResultEntry
        assertEquals results[0].getAttributeValue("sn"), 'Kannel'
        // Test the LdapServiceEntry
        assertEquals results[0].sn, 'Kannel'
        
    }
    
    /**
     * Test findPersonWhere with a single argument.
     */
    void testFindPersonWhereSingleArg() {
        def person = directoryService.findPersonWhere('uid':'1')
        assertNotNull person
        assertEquals person.getAttributeValue("uid"), '1'
        assertEquals person.getAttributeValue("sn"), 'Williams'
    }
    
    /**
     * Test findPersonWhere with multiple arguments.
     */
    void testFindPersonWhereMultipleArgs() {
        def person = directoryService.findPersonWhere('givenName':'roland', 'sn':'conner')
        assertNotNull person
        assertEquals person.getAttributeValue("uid"), '11'
        assertEquals person.getAttributeValue("givenName"), 'Roland'
        assertEquals person.getAttributeValue("sn"), 'Conner'
    }
    
    /**
     * Test findAllPeopleWhere with a single argument.
     */
    void testFindPeopleWhereSingleArg() {
        def people = directoryService.findPeopleWhere('sn':'williams')
        assertNotNull people
        assertEquals people.size(), 4
        assertEquals people[0].getAttributeValue("sn"), 'Williams'
    }
    
    /**
     * Test findPeopleWhere with size limit
     */
    void testFindPeopleWhereWithSizeLimit() {
        def filter = directoryService.createFilter('(&(sn=wa*))')
        assert filter instanceof LDAPFilter      
        def people = directoryService.findPeopleWhere(filter, [sizeLimit:10])
        assertNotNull people
        assertEquals 10, people.size()
    }
    
    /**
     * Test findAllPeopleWhere with multiple arguments.
     */
    void testFindPeopleWhereMultipleArgs() {
        def people = directoryService.findPeopleWhere('sn':'williams', 'l':'berkeley')
        assertNotNull people
        assertEquals people.size(), 2
        assertEquals people[0].getAttributeValue("l"), 'Berkeley'
    }
    
    
    /**
     * Test findAllPeopleWhere with multiple arguments, and sort
     */
    void testFindPeopleWhereMultipleArgsWithSort() {
        def people = directoryService.findPeopleWhere(sn:'williams', st:'ca', [sort:'cn'])
        assertNotNull people
        assertEquals people.size(), 4
        assertEquals people[0].cn, 'Williams, Jim'
        assertEquals people[1].cn, 'Williams, Matthew'
        assertEquals people[2].cn, 'Williams, Russ'
        assertEquals people[3].cn, 'Williams, Sandy'
        
        people = directoryService.findPeopleWhere(sn:'williams', l:'berkeley', [sort:'cn'])
        assertNotNull people
        assertEquals people.size(), 2
        assertEquals people[0].cn, 'Williams, Jim'
        assertEquals people[1].cn, 'Williams, Matthew'
    }
    
    /**
     * Test findAllPeopleWhere with multiple arguments, size limit and sort
     */
    void testFindPeopleWhereMultipleArgsWithSizeLimitAndSort() {
        def people = directoryService.findPeopleWhere(sn:'williams', st:'ca', [sort:'cn'])
        assertNotNull people
        assertEquals people.size(), 4
        assertEquals people[0].cn, 'Williams, Jim'
        assertEquals people[1].cn, 'Williams, Matthew'
        assertEquals people[2].cn, 'Williams, Russ'
        assertEquals people[3].cn, 'Williams, Sandy'
        
        people = directoryService.findPeopleWhere(sn:'williams', st:'ca', [sizeLimit:3, sort:'cn'])
        assertNotNull people
        assertEquals people.size(), 3
        assertEquals people[0].cn, 'Williams, Jim'
        assertEquals people[1].cn, 'Williams, Matthew'
        assertEquals people[2].cn, 'Williams, Russ'
        
        people = directoryService.findPeopleWhere(sn:'williams', l:'berkeley', [sort:'cn'])
        assertNotNull people
        assertEquals people.size(), 2
        assertEquals people[0].cn, 'Williams, Jim'
        assertEquals people[1].cn, 'Williams, Matthew'
    }
    
    /**
     * Test the createFilter method.
     */
    void testCreateFilter() {
        def filter = directoryService.createFilter('(&(sn=do*))')
        assert filter instanceof LDAPFilter
        
        filter = directoryService.createFilter('(&(sn=doe)(departmentNumber=12345)(|(sn=rockwell)))')
        assert filter instanceof LDAPFilter
    }
    
    /**
     * Test the findAllUsingFilter method.
     */
    void testFindAllUsingFilter() {
        def filter = directoryService.createFilter('(&(sn=wa*))')
        assert filter instanceof LDAPFilter
        
        def people = directoryService.findEntriesUsingFilter(peopleBaseDn, filter)
        assertNotNull people
        assertEquals 16, people.size()
    }
    
    void testFindAllUsingFilterWithSizeLimit() {
        def filter = directoryService.createFilter('(&(sn=wa*))')
        assert filter instanceof LDAPFilter
        
        def people = directoryService.findEntriesUsingFilter(peopleBaseDn, filter, [sizeLimit:10])
        assertNotNull people
        assertEquals 10, people.size()
    }
    
    void testFindAllUsingFilterWithTimeLimit() {
        def filter = directoryService.createFilter('(&(sn=wa*))')
        assert filter instanceof LDAPFilter
        
        def people = directoryService.findEntriesUsingFilter(peopleBaseDn, filter, [timeLimit:10])
        assertNotNull people
        assertEquals 16, people.size()
    }
    
    /**
     * Test the findAllPeopleWhere method when supplying a filter.
     */
    void testFindAllPeopleWhereUsingFilter() {
        def filter = directoryService.createFilter('(&(sn=wa*))')
        def people = directoryService.findPeopleWhere(filter)
        assertNotNull people
        assertEquals people.size(), 16
        
        filter = directoryService.createFilter('(|(sn=walters)(sn=williams))')
        people = directoryService.findPeopleWhere(filter)
        assertNotNull people
        assertEquals people.size(), 8
    }
    
    /**
     * Test the findAllPeopleWhere method when supplying a filter and a sort
     * attribute.
     */
    void testFindAllPeopleWhereUsingFilterWithSort() {
        def filter = directoryService.createFilter('(&(sn=smith))')
        def people = directoryService.findPeopleWhere(filter, [sort:'cn'])
        assertNotNull people
        assertEquals people.size(), 5
        assertEquals people[0].cn, 'Smith, Catherine'
        assertEquals people[4].cn, 'Smith, William'
    }
    
    /**
     * Test of specified attributes, which are "*" and "createTimestamp".
     */
    void testFindWithReturnAttributes() {
        def person = directoryService.findPersonWhere(uid:'3')
        assertEquals person.displayName, 'Mabel Rockland'
        assertNotNull person.createTimestamp
    }
    
    /**
     * Test of sub entries.
     */
    void testFindSubentries() {
        def entries = directoryService.findSubentriesWhere(
            'uid=0,ou=people,dc=someu,dc=edu',
            [objectClass:'room'])
        assertNotNull entries
        assertEquals entries.size(), 4

        entries = directoryService.findSubentriesWhere(
            'uid=0,ou=people,dc=someu,dc=edu',
            [objectClass:'room'],
            [sort:'cn'])
        assertNotNull entries
        assertEquals entries.size(), 4
        assertEquals entries[0].cn, 'Cafe'
        assertEquals entries[1].cn, 'Lab'
        assertEquals entries[2].cn, 'Main Office'
        assertEquals entries[3].cn, 'Starlight Room'
        
    }

    /**
     * Test of sub entries using a filter instead of a Map.
     */
    void testFindSubentriesUsingFilter() { 
        def filter = directoryService.createFilter('(&(objectClass=room))')
        def entries = directoryService.findSubentriesWhere(
            'uid=0,ou=people,dc=someu,dc=edu',
            filter,
            [sort:'cn'])
        assertNotNull entries
        assertEquals entries.size(), 4
        assertEquals entries[0].cn, 'Cafe'
        assertEquals entries[1].cn, 'Lab'
        assertEquals entries[2].cn, 'Main Office'
        assertEquals entries[3].cn, 'Starlight Room'
    }
    
    /**
     * Test search using an anonymous bind. The InMemoryDirectoryServer allows
     * anonymous binds.
     */
     void testAnonymousBind() {
        def people = directoryService.findPeepsWhere('sn':'nguyen')
        assertNotNull people
        assertEquals people.size(), 4
     }
     
     /**
      * Test that we are only returning the cn, sn, and creatorsName.
      */
     void testOnlySnAndCnReturned() {
        def person = directoryService.findPeepWhere(uid:'3')
        assertNotNull person
        assertEquals person.sn, 'Rockland'
        assertEquals person.cn, 'Rockland, Mabel'
        assertEquals person.creatorsName, 'cn=Internal Root User'
        assertNull person.givenName
        assertNull person.employeeNumber
        assertNull person.mail
     }
     
     /**
      * Test get, which operates on the RDN attribute of the dit object
      * specified.
      */
     void testGet() {
        def person = directoryService.getPerson('1')
        assertNotNull person
        assertEquals person.uid, '1'
        
        def account = directoryService.getAccount('Lavers, Matthew')
        assertNotNull account
        assertEquals account.uid, '139'
        
     }
     
     /**
      * Test save().
      */
     void testSave() {
        def person = directoryService.getPerson('2')
        assertEquals person.sn, 'Evans'
        assertEquals person.cnValues().size(), 2
        person.sn = 'Evans-Peters'
        person.cn = ['Evans-Peters, Sally', 'Sally Evans-Peters', 'Sally Evans', 'Sally Peters']
        person.displayName = 'Sally Evans-Peters'
        person.mail = ''
        assertTrue person.isDirty()
        directoryService.save(person)
        
        // Now test that the original object got cleaned up.
        assertFalse person.isDirty()
        person.mail = 'testing@someu.edu'
        person.discard() // This will test that searchResultEntry got updated
        assertEquals person.sn, 'Evans-Peters'
        
        // Now fetch the person from the directory again to check that the
        // change was actually saved.
        def person2 = directoryService.getPerson('2')
        assertEquals person2.sn, 'Evans-Peters'
        assertEquals person2.cnValues().size(), 4
        assertEquals person2.displayName, 'Sally Evans-Peters'
        // Now let's really make sure this is the same person.
        assertEquals person2.initials, 'SDE'
        assertEquals person2.telephoneNumber, '+1 022 028 9350'
     }
     
     /**
      * Test save() with no mods. Should not throw an exception.
      */
     void testSaveWithNoMods() {
        def person = directoryService.getPerson('2')
        directoryService.save(person)
        
        person.sn = 'Something New'
        assertTrue person.isDirty()
        person.sn = 'Evans'
        assertTrue person.isDirty()
        directoryService.save(person)
        assertFalse person.isDirty()
     }
    
}
