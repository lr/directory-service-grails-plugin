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
class DirectoryServicePoolTests extends GroovyTestCase {

    /* DirectoryService to use for all tests. */
    def directoryService
    
    /* Used for comparison. */
    def peopleBaseDn = 'ou=people,dc=someu,dc=edu'
    
    /**
     * Tests connection pool speed (measured by final time to run tests).
     */
    void testFind() {
        def args = ['sn':'evans']
        for (int i = 0; i < 200; i++) {
            def result = directoryService.findEntry(peopleBaseDn, args)
            assertEquals result.sn, 'Evans'
        }
    }
    
}
