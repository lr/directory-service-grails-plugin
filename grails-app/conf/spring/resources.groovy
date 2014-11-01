import grails.plugins.directoryservice.listener.InMemoryDirectoryServerFactory
import grails.util.Environment

beans = {
    if(Environment.current == Environment.TEST) {
        inMemServer(InMemoryDirectoryServerFactory) {
            baseDN = 'dc=someu,dc=edu'
            configurationName = 'grails.plugins.directoryservice.sourcesForInMemoryServer.directory'
            schemaPath = 'test/ldif/schema/directory-schema.ldif'
            contentsPath = 'test/ldif/directory.ldif'
        }

        adInMemServer(InMemoryDirectoryServerFactory) {
            baseDN = "dc=someu,dc=edu"
            configurationName = 'grails.plugins.directoryservice.sourcesForInMemoryServer.ad'
            schemaPath = "test/ldif/schema/ad-schema.ldif"
            contentsPath = "test/ldif/accounts.ldif"
        }
    }
}
