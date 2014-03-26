package grails.plugins.directoryservice

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

    /**
     * Tests to make sure a bind actually uses the pool. By calling
     * the search over and over, we are testing that we are using the pool.
     */
    void testBind() {
        def people = directoryService.findPeopleWhere(sn:'williams')
        assertNotNull people
        assertEquals people.size(), 4

        def people2 = directoryService.findPeopleWhere(sn:'williams')
        assertNotNull people2
        assertEquals people2.size(), 4

        def people3 = directoryService.findPeopleWhere(sn:'williams')
        assertNotNull people3
        assertEquals people3.size(), 4
    }

    /**
     * Tests to make sure the anonymous bind actually uses the pool. By calling
     * the search over and over, we are testing that we are using the pool.
     */
    void testAnonBind() {
        def people = directoryService.findAnonPeepsWhere(sn:'williams')
        assertNotNull people
        assertEquals people.size(), 4

        def people2 = directoryService.findAnonPeepsWhere(sn:'williams')
        assertNotNull people2
        assertEquals people2.size(), 4

        def people3 = directoryService.findAnonPeepsWhere(sn:'williams')
        assertNotNull people3
        assertEquals people3.size(), 4
    }
}
