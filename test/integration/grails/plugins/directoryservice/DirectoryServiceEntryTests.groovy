package grails.plugins.directoryservice

import grails.test.mixin.*
import org.junit.*

import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.Filter as LDAPFilter
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.SearchResultEntry

import grails.plugins.directoryservice.listener.InMemoryDirectoryServer

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class DirectoryServiceEntryTests extends GroovyTestCase {

    /* DirectoryService to use for all tests. */
    def directoryService
    
    /* DirectoryServiceEntry */
    def dse

    def inMemServer

    /**
     * Set up by creating an DirectoryService and setting grailsApplication config
     * based on ConfigurationHolder.config. Then set up the UnboundID
     * InMemoryDirectoryServer, and then set the port in LdapService to the
     * port that was created with the InMemoryDirectoryServer started (it is
     * random each time it starts).
     */
    protected void setUp() {
        super.setUp()
        
        def config = grails.util.GrailsConfig.ds.sourcesForInMemoryServer['directory']
        
        inMemServer = new InMemoryDirectoryServer(
            "dc=someu,dc=edu",
            config,
            "test/ldif/schema/directory-schema.ldif",
            "test/ldif/directory.ldif"
        )
        
        // Set up lse to be the person with uid=1
        dse = directoryService.findPersonWhere('uid':'6')
        // Since the search returns a LdapServiceEntry, we have to get the
        // SearchResultEntry if we want to create a new LdapServiceEntry.
        //dse = new DirectoryServiceEntry(person.getSearchResultEntry())
    }

    protected void tearDown() {
        inMemServer?.shutDown()
    }

    /**
     * Test that the underlying Entry still works.
     */
    void testUnboundIDEntry() {
        assertEquals dse.entry.getDN(), 'uid=6,ou=people,dc=someu,dc=edu'
        assertEquals dse.entry.getAttributeValueAsDate('someuEduEmpExpDate').toString(), 
            'Fri Dec 31 15:59:59 PST 9999'
    }

    /**
     * Test getAttributeValue from the set entry object.
     */
    void testGetAttributeValue() {
        assertEquals dse.getAttributeValue('sn'), 'Nguyen'
    }
    
    /**
     * Test getAttributeValues from the set entry object.
     */
    void testGetAttributeValues() {
        assertEquals dse.getAttributeValues('cn').size(), 2
    }
    
    /**
     * Test invoke getValue using attribute name as method.
     */
    void testAttributeNameAsMethod() {
        assertEquals dse.dn(), 'uid=6,ou=people,dc=someu,dc=edu'
        assertEquals dse.sn(), 'Nguyen'
        assertEquals dse.givenName(), 'Julie'
    }
    
    /**
     * Test invoke getValue using attribute name as property.
     */
    void testAttributeNameAsProperty() {
        assertEquals dse.dn, 'uid=6,ou=people,dc=someu,dc=edu'
        assertEquals dse.sn, 'Nguyen'
        assertEquals dse.givenName, 'Julie'
    }
    
    /**
     * Test attribute as Date.
     */
    void testAttributeAsDate() {
        assertEquals dse.someuEduEmpExpDateAsDate().toString(),
            'Fri Dec 31 15:59:59 PST 9999'
    }
    
    /**
     * Test attribute as Boolean.
     */
    void testAttributeAsBoolean() {
        assertTrue dse.someuEduFacultyFlagAsBoolean()
        assertTrue Boolean.parseBoolean(dse.someuEduFacultyFlag)
    }
    
    /**
     * Test isDirty()
     */
    void testIsDirty() {
        assertFalse dse.isDirty()
        assertFalse dse.isDirty('mail')
    }
    
    /**
     * Simple test to make sure the Entry is really and entry, and not a
     * SearchResultEntry, as SearchResultEntry objects do not allow mods.
     */
    void testSetOfEntry() {
        assert dse.entry instanceof Entry
        dse.entry.setAttribute('mail', 'new.name@someu.edu')
    }
    
    /**
     * Test modification of one attribute using the updateModifications()
     * method directly.
     */
    void testUpdateModificationsOneAttribute() {
        assertEquals dse.modifications.size(), 0
        dse.updateModifications('mail', 'new.name@someu.edu')
        assertEquals dse.modifications.size(), 1
        assertEquals dse.modifications.get(0).getValues().length, 1
        
        // Update mail again, but it should replace it, and not add
        dse.updateModifications('mail', 'newname2.name@someu.edu')
        assertEquals dse.modifications.size(), 1
        assertEquals dse.modifications.get(0).getValues().length, 1
        
        dse.updateModifications('mail', 'new.name@someu.edu', 'newname2.name@someu.edu')
        assertEquals dse.modifications.size(), 1
        assertEquals dse.modifications.get(0).getValues().length, 2
    }
    
    /**
     * Test modifications with more than one attribute using the
     * updateModifications() method directly.
     */
    void testUpdateModificationsMultipleAttributes() {
        assertEquals dse.modifications.size(), 0
        dse.updateModifications('mail', 'new.name@someu.edu')
        assertEquals dse.modifications.size(), 1
        
        dse.updateModifications('cn', 'Julie Nguyen', 'Nguyen, Julia')
        assertEquals dse.modifications.size(), 2
        assertEquals dse.modifications.get(0).getValues().length, 1
        assertEquals dse.modifications.get(1).getValues().length, 2
        
        dse.updateModifications('mail', 'new.name@someu.edu', 'Julia.Nguyen@someu.edu')
        assertEquals dse.modifications.size(), 2
        assertEquals dse.modifications.get(0).getValues().length, 2
    }
    
    /**
     * Test propertyMissing() with a single value.
     */
    void testPropertyMissingSingleVal() {
        assertEquals dse.mail, 'Julie.Nguyen@someu.edu'
        dse.mail = 'new.name@someu.edu'
        assertTrue dse.isDirty()
        assertTrue dse.isDirty('mail')
        assertEquals dse.mail, 'new.name@someu.edu'
        assertEquals dse.mods['mail'], 'new.name@someu.edu'
        assertEquals dse.modifications.size(), 1
        assertEquals dse.modifications.get(0).getValues().length, 1
    }
    
    /**
     * Test propertyMissing with multiple values.
     */
    void testPropertyMissingMultipleVals() {
        assertEquals dse.mail, 'Julie.Nguyen@someu.edu'
        dse.mail = ['new.name@someu.edu', 'another.email@someu.edu']
        assertTrue dse.isDirty()
        assertTrue dse.isDirty('mail')
        //assertEquals dse.mail, ['new.name@someu.edu', 'another.email@someu.edu']
        assertEquals dse.mods['mail'], ['new.name@someu.edu', 'another.email@someu.edu']
        assertEquals dse.modifications.size(), 1
        assertEquals dse.modifications.get(0).getValues().length, 2
    }

}