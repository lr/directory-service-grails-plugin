package grails.plugins.directoryservice

import com.unboundid.ldap.sdk.Entry

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class DirectoryServiceEntryTests extends GroovyTestCase {

    /* grailsApplication, which is necessary for configuration info. */
    def grailsApplication

    /* DirectoryService to use for all tests. */
    def directoryService
    private dse
    private getDirectoryServiceEntry() {
        if(!dse) {
            dse = directoryService.findPersonWhere('uid': '6')
        }
        return dse
    }

    /**
     * Test that the underlying Entry still works.
     */
    void testUnboundIDEntry() {
        assertEquals directoryServiceEntry.entry.getDN(), 'uid=6,ou=people,dc=someu,dc=edu'
        assertEquals directoryServiceEntry.entry.getAttributeValueAsDate('someuEduEmpExpDate').toString(),
                'Fri Dec 31 15:59:59 PST 9999'
    }

    /**
     * Test getAttributeValue from the set entry object.
     */
    void testGetAttributeValue() {
        assertEquals directoryServiceEntry.getAttributeValue('sn'), 'Nguyen'
    }

    /**
     * Test getAttributeValues from the set entry object.
     */
    void testGetAttributeValues() {
        assertEquals directoryServiceEntry.getAttributeValues('cn').size(), 2
    }

    /**
     * Test values from the set entry object.
     */
    void testValues() {
        assertEquals directoryServiceEntry.cnValues().size(), 2
    }

    /**
     * Test invoke getValue using attribute name as method.
     */
    void testAttributeNameAsMethod() {
        assertEquals directoryServiceEntry.dn(), 'uid=6,ou=people,dc=someu,dc=edu'
        assertEquals directoryServiceEntry.sn(), 'Nguyen'
        assertEquals directoryServiceEntry.givenName(), 'Julie'
    }

    /**
     * Test invoke getValue using attribute name as property.
     */
    void testAttributeNameAsProperty() {
        assertEquals directoryServiceEntry.dn, 'uid=6,ou=people,dc=someu,dc=edu'
        assertEquals directoryServiceEntry.sn, 'Nguyen'
        assertEquals directoryServiceEntry.givenName, 'Julie'
    }

    /**
     * Test attribute as Date.
     */
    void testAttributeAsDate() {
        assertEquals directoryServiceEntry.someuEduEmpExpDateAsDate().toString(),
                'Fri Dec 31 15:59:59 PST 9999'
    }

    /**
     * Test attribute as Boolean.
     */
    void testAttributeAsBoolean() {
        assertTrue directoryServiceEntry.someuEduFacultyFlagAsBoolean()
        assertTrue Boolean.parseBoolean(directoryServiceEntry.someuEduFacultyFlag)
    }

    /**
     * Test isDirty()
     */
    void testIsDirty() {
        assertFalse directoryServiceEntry.isDirty()
        assertFalse directoryServiceEntry.isDirty('mail')
    }

    /**
     * Simple test to make sure the Entry is really and entry, and not a
     * SearchResultEntry, as SearchResultEntry objects do not allow mods.
     */
    void testSetOfEntry() {
        assert directoryServiceEntry.entry instanceof Entry
        directoryServiceEntry.entry.setAttribute('mail', 'new.name@someu.edu')
    }

    /**
     * Test modification of one attribute using the updateModifications()
     * method directly.
     * As of 0.6.1, You can not pass mods directly to updateModifications().
     */
    /*
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
    */

    /**
     * Test modifications with more than one attribute using the
     * updateModifications() method directly.
     * As of 0.6.1, You can not pass mods directly to updateModifications().
     */
    /*
    void testUpdateModificationsMultipleAttributes() {
        assertEquals dse.modifications.size(), 0
        dse.updateModifications('mail', 'new.name@someu.edu')
        assertEquals dse.modifications.size(), 1

        assertEquals dse.getAttributeValues('cn').size(), 2
        dse.updateModifications('cn', 'Julie Nguyen', 'Nguyen, Julie', 'Julie A Nguyen', 'Nguyen, Julie A')
        assertEquals dse.modifications.size(), 2
        assertEquals dse.modifications.get(1).getValues().length, 4

        // Double check that mail is still 1
        assertEquals dse.modifications.get(0).getValues().length, 1

        dse.updateModifications('mail', 'new.name@someu.edu', 'Julie.Nguyen@someu.edu')
        assertEquals dse.modifications.size(), 2
        assertEquals dse.modifications.get(0).getValues().length, 2

        // Double check that cn is still 4
        assertEquals dse.modifications.get(1).getValues().length, 4
    }
    */

    /**
     * Test propertyMissing() with a single value.
     */
    void testPropertyMissingSingleVal() {
        assertEquals directoryServiceEntry.mail, 'Julie.Nguyen@someu.edu'
        directoryServiceEntry.mail = 'new.name@someu.edu'
        assertTrue directoryServiceEntry.isDirty()
        assertTrue directoryServiceEntry.isDirty('mail')
        assertEquals directoryServiceEntry.mail, 'new.name@someu.edu'
        assertEquals directoryServiceEntry.mods['mail'], 'new.name@someu.edu'
        assertEquals directoryServiceEntry.modifications.size(), 1
        assertEquals directoryServiceEntry.modifications.get(0).getValues().length, 1
    }

    /**
     * Test propertyMissing with multiple values.
     */
    void testPropertyMissingMultipleVals() {
        assertEquals directoryServiceEntry.mail, 'Julie.Nguyen@someu.edu'
        directoryServiceEntry.mail = ['new.name@someu.edu', 'another.email@someu.edu']
        assertTrue directoryServiceEntry.isDirty()
        assertTrue directoryServiceEntry.isDirty('mail')
        //assertEquals dse.mail, ['new.name@someu.edu', 'another.email@someu.edu']
        assertEquals directoryServiceEntry.modifications.size(), 1
        assertEquals directoryServiceEntry.modifications.get(0).getValues().length, 2

        directoryServiceEntry.cn = ['Julie Nguyen', 'Nguyen, Julie', 'Julie A Nguyen', 'Nguyen, Julie A']
        assertEquals directoryServiceEntry.modifications.size(), 2
        assertEquals directoryServiceEntry.modifications.get(0).getValues().length, 4
        assertEquals directoryServiceEntry.getAttributeValues('cn').length, 4
    }

    /**
     * Test adding attribute that does not exist in the entry. The number of
     * attributes is 28 because we are getting operational attributes.
     */
    void testPropertyMissingAddAttribute() {
        assertEquals directoryServiceEntry.entry.getAttributes().size(), 28
        directoryServiceEntry.carLicense = 'B12345C'
        assertEquals directoryServiceEntry.modifications.size(), 1
        assertEquals directoryServiceEntry.entry.getAttributes().size(), 29
        assertEquals directoryServiceEntry.carLicense, 'B12345C'
        directoryServiceEntry.carLicense = null
        assertEquals directoryServiceEntry.modifications.size(), 0
        assertEquals directoryServiceEntry.entry.getAttributes().size(), 28
    }

    /**
     * Test propertyMissing with deleting values.
     */
    void testPropertyMissingRemoveValue() {
        assertEquals directoryServiceEntry.mail, 'Julie.Nguyen@someu.edu'
        directoryServiceEntry.mail = ''
        assertNull directoryServiceEntry.mail
        assertEquals directoryServiceEntry.modifications.size(), 1
        assertEquals directoryServiceEntry.modifications[0].getModificationType().getName(), 'REPLACE'

        directoryServiceEntry.mail = 'some.mail@someu.edu'
        assertEquals directoryServiceEntry.modifications.size(), 1
        assertEquals directoryServiceEntry.mail, 'some.mail@someu.edu'
    }

    /**
     * Test discard.
     */
    void testDiscard() {
        directoryServiceEntry.mail = null
        directoryServiceEntry.carLicense = 'B12345C'
        assertNull directoryServiceEntry.mail
        assertEquals directoryServiceEntry.carLicense, 'B12345C'
        directoryServiceEntry.discard()
        assertEquals directoryServiceEntry.mail, 'Julie.Nguyen@someu.edu'
        assertNull directoryServiceEntry.carLicense
    }
}
