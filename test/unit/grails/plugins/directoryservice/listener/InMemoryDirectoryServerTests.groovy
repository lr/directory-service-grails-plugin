package grails.plugins.directoryservice.listener

import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldif.LDIFReader

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class InMemoryDirectoryServerTests extends GroovyTestCase {

    def peopleBaseDN = 'ou=people,dc=someu,dc=edu'

    /**
     * Test that we can read from the sources map properly.
     */
    void testConfig() {
        assertEquals grails.util.GrailsConfig.grails.plugins.directoryservice.sourcesForInMemoryServer['directory'].port,  '33389'
    }

    /**
     * Test that schema is parsed properly.
     */
    void testSchemaEntryFromLDIF() {
        def inMemServer = new InMemoryDirectoryServer()
        def schema = inMemServer.schemaEntryFromLDIF("test/ldif/schema/directory-schema.ldif")
        assertEquals schema.schemaEntry.DN, 'cn=schema'
    }

    /**
     * Test that schema is parsed properly.
     */
    void testADSchema() {
        def inMemServer = new InMemoryDirectoryServer()
        def schema = inMemServer.schemaEntryFromLDIF("test/ldif/schema/ad-schema.ldif")
        assertEquals schema.schemaEntry.DN, 'cn=schema'
    }

    /**
     * Test setting up of the server, listening on the provided port, and
     * then searching. Basically, if you can perform a search, the server
     * was set up properly.
     */
    void testListenAndSearch() {
        def config = grails.util.GrailsConfig.grails.plugins.directoryservice.sourcesForInMemoryServer['directory']

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

        server.shutDown()

    }

    /**
     * Test exporting of data from the server.
     */
    void testExport() {
        def config = grails.util.GrailsConfig.grails.plugins.directoryservice.sourcesForInMemoryServer['directory']

        def server = new InMemoryDirectoryServer(
            "dc=someu,dc=edu",
            config,
            "test/ldif/schema/directory-schema.ldif",
            "test/ldif/directory.ldif"
        )

        def path = "/tmp/myexport.ldif"
        server.export(path)
        def reader = new LDIFReader(path)
        def entry = reader.readEntry()
        assertEquals entry.getDN(), 'dc=someu,dc=edu'
    }
}
