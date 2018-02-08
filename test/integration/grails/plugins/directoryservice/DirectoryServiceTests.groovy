package grails.plugins.directoryservice
import com.unboundid.ldap.sdk.Filter as LDAPFilter
/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 *
 * This test class assumes that an inMemServer and adInMemServer are defined in resources.groovy
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

    def inMemServer
    def adInMemServer

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
     * Test that we can find a single person by using a filter.
     */
    void testFindOnePersonUsingFilter() {
        def filter = directoryService.createFilter('(&(givenName=Catherine)(sn=Smith)(|(uid=288)))')
        def person = directoryService.findPersonWhere(filter)
        assertNotNull person
        assertEquals person.cn, 'Smith, Catherine'
        assertEquals person.givenName, 'Catherine'
        assertEquals person.sn, 'Smith'
    }

    /**
     * Test that we can find a single person by using a filter.
     */
    void testFindOnePersonUsingString() {
        def filter = '(&(givenName=Catherine)(sn=Smith)(|(uid=288)))'
        def person = directoryService.findPersonWhere(filter)
        assertNotNull person
        assertEquals person.cn, 'Smith, Catherine'
        assertEquals person.givenName, 'Catherine'
        assertEquals person.sn, 'Smith'
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
      * Test that we are only returning the cn, sn, and creatorsName as that is
      * what is listed in the DIT config.
      */
     void testOnlySnAndCnReturnedViaDITConfig() {
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
      * Test that we are only returning the cn, sn, and creatorsName because
      * we are specifying only those attributes (when searching using a String)
      */
     void testOnlySnAndCnReturnedAsArgumentsUsingStringFilter() {
         def person = directoryService.findPersonWhere('(&(uid=3))', [attrs:['sn', 'cn', 'creatorsName']])
         assertNotNull person
         assertEquals person.sn, 'Rockland'
         assertEquals person.cn, 'Rockland, Mabel'
         assertEquals person.creatorsName, 'cn=Internal Root User'
         assertNull person.givenName
         assertNull person.employeeNumber
         assertNull person.mail
     }
     
     /**
      * Test that we are only returning the cn, sn, and creatorsName because
      * we are specifying only those attributes (when searching using a Filter)
      */
     void testOnlySnAndCnReturnedAsArgumentsUsingFilter() {
         def filter = directoryService.createFilter('(&(uid=3))')
         def person = directoryService.findPersonWhere(filter, [attrs:['sn', 'cn', 'creatorsName']])
         assertNotNull person
         assertEquals person.sn, 'Rockland'
         assertEquals person.cn, 'Rockland, Mabel'
         assertEquals person.creatorsName, 'cn=Internal Root User'
         assertNull person.givenName
         assertNull person.employeeNumber
         assertNull person.mail
     }
     
     /**
      * Test that we are only returning sn, and creatorsName because
      * we are specifying only those attributes (when searching using a String)
      */
     void testOnlySnIsReturnedAsSpecifingJustTwoRecordsAsArgumentsUsingStringFilter() {
         def people = directoryService.findPeopleWhere('(&(sn=smith))', [attrs:['sn'], sizeLimit:2])
         assertNotNull people
         def person = people[0]
         assertEquals people.size(), 2
         assertEquals person.sn, 'Smith'
         assertNull person.cn
         assertNull person.givenName
         assertNull person.employeeNumber
         assertNull person.mail
     }
     
     /**
      * Test that we are only returning sn, and creatorsName because
      * we are specifying only those attributes (when searching using a String)
      */
     void testOnlyCNIsReturnedForTwoRecordsSortedByCN() {
         def people = directoryService.findPeopleWhere('(&(sn=smith))', [attrs:['cn'], sizeLimit:2, sort:'cn'])
         assertNotNull people
         def person = people[0]
         assertEquals people.size(), 2
         assertEquals person.cn, 'Smith, Catherine'
         assertNull person.sn
         assertNull person.givenName
         assertNull person.employeeNumber
         assertNull person.mail
         
         assertEquals people[1].cn, 'Smith, John'
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
      * Test get, which operates on the RDN attribute of the dit object
      * specified, but only return the specified attributes.
      */
     void testGetWithSpecifiedAttrs() {
        def person = directoryService.getPerson('1', [attrs:['uid']])
        assertNotNull person
        assertEquals person.uid, '1'
        assertNull person.givenName
        assertNull person.sn

        def account = directoryService.getAccount('Lavers, Matthew', [attrs:['uid']])
        assertNotNull account
        assertEquals account.uid, '139'
        assertNull person.givenName
        assertNull person.sn
     }

     /**
      * Tests searchUsingFilter using a variety of the features.
      */
     void testSearchUsingFilter() {
         def baseDN = 'ou=people,dc=someu,dc=edu'
         def filter = '(&(sn=smith))'
         def people = directoryService.searchUsingFilter(baseDN, filter)
         assertEquals people.size(), 5
         
         people = directoryService.searchUsingFilter(baseDN, filter, [sort:'cn'])
         assertEquals people.size(), 5
         assertEquals people[0].getAttributeValue('givenName'), 'Catherine'
         assertNotNull people[0].getAttributeValue('mail')
         
         people = directoryService.searchUsingFilter(baseDN, filter, [sort:'cn', attrs:['givenName']])
         assertEquals people.size(), 5
         assertEquals people[0].getAttributeValue('givenName'), 'Catherine'
         assertNull people[0].getAttributeValue('mail')
         assertNull people[1].getAttributeValue('mail')
         
         // Supplying attrs at the end of the method.
         people = directoryService.searchUsingFilter(baseDN, filter, [sort:'cn'], 'sn', 'cn')
         assertEquals people.size(), 5
         assertEquals people[0].getAttributeValue('sn'), 'Smith'
         assertEquals people[0].getAttributeValue('cn'), 'Smith, Catherine'
         assertEquals people[1].getAttributeValue('sn'), 'Smith'
         assertEquals people[1].getAttributeValue('cn'), 'Smith, John'
         assertNull people[0].getAttributeValue('givenName')
         assertNull people[0].getAttributeValue('mail')
         assertNull people[1].getAttributeValue('givenName')
         assertNull people[1].getAttributeValue('mail')
         
         // Supplying attrs in the map overrides the attrs at the end of the method.
         people = directoryService.searchUsingFilter(baseDN, filter, [sort:'cn', attrs:['sn', 'cn']], 'givenName', 'mail')
         assertEquals people.size(), 5
         assertEquals people[0].getAttributeValue('sn'), 'Smith'
         assertEquals people[0].getAttributeValue('cn'), 'Smith, Catherine'
         assertEquals people[1].getAttributeValue('sn'), 'Smith'
         assertEquals people[1].getAttributeValue('cn'), 'Smith, John'
         assertNull people[0].getAttributeValue('givenName')
         assertNull people[0].getAttributeValue('mail')
         assertNull people[1].getAttributeValue('givenName')
         assertNull people[1].getAttributeValue('mail')
     }

     /**
      * Tests paged searching. We have to resort to some trickery to see if we
      * can test the size. To ensure we are actually doing a paged search, we
      * have enabled warn logging and can now verify the number of paged searches
      * in the log.
      */
     void testPagedSearch() {
         def people = directoryService.findPeopleWhere("(&(uid=*))", [pagedSearch: true, pageSize: 10])
         assertNotNull people
         assertEquals people.size(), 385
         
         people = directoryService.findPeopleWhere("(&(uid=*))",
             [sort: 'cn', attrs:['givenName', 'sn'], pagedSearch: true, pageSize: 10])
         assertNotNull people
         assertEquals people.size(), 385
         
         // If the sizeLimit is 10 or more, we will always get the entire amount
         // of results.
         people = directoryService.findPeopleWhere("(&(uid=*))",
             [sort: 'cn', attrs:['givenName', 'sn'], pagedSearch: true, pageSize: 10, sizeLimit: 10])
         assertNotNull people
         assertEquals people.size(), 385
         
         people = directoryService.findPeopleWhere("(&(uid=*))",
             [sort: 'cn', attrs:['givenName', 'sn'], pagedSearch: true, pageSize: 10, sizeLimit: 15])
         assertNotNull people
         assertEquals people.size(), 385
         
         // Each page size is 10, and we limit the number of results to 9, so
         // that means, desipte pagings, we are only going to get 9.
         people = directoryService.findPeopleWhere("(&(uid=*))",
             [sort: 'cn', attrs:['givenName', 'sn'], pagedSearch: true, pageSize: 10, sizeLimit: 9])
         assertNotNull people
         assertEquals people.size(), 9
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
     
     /**
      * Test deleting an object using the connection.
      */
     void testDeleteUsingConnection() {
         def person = directoryService.getPerson('2')
         assertNotNull person
         directoryService.connection(peopleBaseDn)?.delete(person.dn)
         
         person = directoryService.getPerson('2')
         assertNull person
     }

     /**
      * Test snapshotting
      */
     void testSnapshot() {
         def snapshot = inMemServer.createSnapshot()
         def person = directoryService.getPerson('1')
         assertNotNull person
         
         directoryService.connection(peopleBaseDn)?.delete(person.dn)  
         person = directoryService.getPerson('1')
         assertNull person

         inMemServer.restoreSnapshot(snapshot)
         person = directoryService.getPerson('1')
         assertNotNull person
    }

}
