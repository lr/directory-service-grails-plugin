package grails.plugins.directoryservice.listener

import grails.test.mixin.*
import org.junit.*

import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.Filter as LDAPFilter
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SearchScope

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class InMemoryDirectoryServerTests extends GroovyTestCase {

    def peopleBaseDN = 'ou=people,dc=someu,dc=edu'

    protected void setUp() {
        super.setUp()
    }
    
    void testConfig() {
        assertEquals grails.util.GrailsConfig.ds.sources['directory'].port,  '33389'
    }
    
    void testSchemaEntryFromLDIF() {
        def inMemServer = new InMemoryDirectoryServer()
        def schema = inMemServer.schemaEntryFromLDIF("test/ldif/schema/directory-schema.ldif")
        assertEquals schema.schemaEntry.DN, 'cn=schema'
    }
    
    void testADSchema() {
        def inMemServer = new InMemoryDirectoryServer()
        def schema = inMemServer.schemaEntryFromLDIF("test/ldif/schema/ad-schema.ldif")
        assertEquals schema.schemaEntry.DN, 'cn=schema'
    }
    
    void testListenAndSearch() {
        def config = grails.util.GrailsConfig.ds.sources['directory']
        
        def server = new InMemoryDirectoryServer(
            "dc=someu,dc=edu",
            config,
            "test/ldif/schema/directory-schema.ldif",
            "test/ldif/directory.ldif"
        )
        
        def conn = new LDAPConnection(
            "localhost",
            Integer.parseInt(config.port),
            config.bindDN,
            config.bindPassword
        )
        
        def result = conn.search(
            peopleBaseDN,
            SearchScope.SUB,
            "sn=Hampshire"
        )
        
        def entries = result.getSearchEntries()
        
        assertEquals entries.size(), 4
        
    }


}