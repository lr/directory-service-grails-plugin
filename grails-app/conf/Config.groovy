environments {
    test {

        // There is nothing running on my dev machine on
        // port 11389, so by doing this we are always testing
        // failover! Other, over-the-wire servers have been tested
        // as well.
        grails.plugins.directoryservice.sources = [
            directory:[
                address: 'localhost , localhost',
                port: ' 11389 ,33389',
                useSSL: false,
                trustSSLCert: true,
                followReferrals: true,
                useConnectionPool: true,
                initialConnections: 5,
                maxConnections: 50,
                bindDN: 'cn=Directory Manager',
                bindPassword: 'password'
            ],
            ad:[
                address: 'localhost',
                port: '33268',
                useSSL: false,
                trustSSLCert: true,
                followReferrals: false,
                bindDN: 'cn=AD Manager',
                bindPassword: 'password'
            ],
            directoryAnonymous:[
                address: 'localhost',
                port: '33389',
                useSSL: false,
                trustSSLCert: true,
                followReferrals: true
            ],
            directoryAnonymousWithPool:[
                address: 'localhost',
                port: '33389',
                useSSL: false,
                trustSSLCert: true,
                followReferrals: true,
                useConnectionPool: true,
                initialConnections: 5,
                maxConnections: 50
            ]
        ]

        // Since we are testing failover above, we need another
        // Map of source details for the InMemoryServer. This is
        // just made up for our tests, and is not part of the API
        // for this project.
        grails.plugins.directoryservice.sourcesForInMemoryServer = [
            directory:[
                address: 'localhost',
                port: '33389',
                useSSL: false,
                trustSSLCert: true,
                bindDN: 'cn=Directory Manager',
                bindPassword: 'password'
            ],
            ad:[
                address: 'localhost',
                port: '33268',
                useSSL: false,
                trustSSLCert: true,
                bindDN: 'cn=AD Manager',
                bindPassword: 'password'
            ]
        ]

        grails.plugins.directoryservice.dit = [
            'ou=people,dc=someu,dc=edu':[
                singular: 'person',
                plural: 'people',
                rdnAttribute: 'uid',
                source: 'directory',
                attributes: ['*', '+']
            ],
            'ou=departments,dc=someu,dc=edu':[
                singular: 'department',
                plural: 'departments',
                rdnAttribute: 'ou',
                source: 'directory'
            ],
            'ou=groups,dc=someu,dc=edu':[
                singular: 'group',
                plural: 'groups',
                rdnAttribute: 'cn',
                source: 'directory'
            ],
            'ou=personnes,dc=someu,dc=edu':[
                singular: 'personne',
                plural: 'personnes',
                rdnAttribute: 'uid',
                source: 'directory'
            ],
            'ou=accounts,dc=someu,dc=edu':[
                singular: 'account',
                plural: 'accounts',
                rdnAttribute: 'cn',
                source: 'ad'
            ],
            'ou=Economics,ou=accounts,dc=someu,dc=edu':[
                singular: 'economist',
                plural: 'economists',
                rdnAttribute: 'cn',
                source: 'ad'
            ],
            'ou=People,dc=someu,dc=edu':[
                singular: 'peep',
                plural: 'peeps',
                rdnAttribute: 'uid',
                source: 'directoryAnonymous',
                attributes: ['cn', 'sn', 'creatorsName']
            ],
            'ou=PEople,dc=someu,dc=edu':[
                singular: 'anonPeep',
                plural: 'anonPeeps',
                rdnAttribute: 'uid',
                source: 'directoryAnonymousWithPool',
                attributes: ['cn', 'sn', 'creatorsName']
            ]
        ]

    }
}

log4j = {
    error 'org.codehaus.groovy.grails',
          'org.springframework',
          'org.hibernate',
          'net.sf.ehcache.hibernate'
    
    warn 'grails.plugins.directoryservice'
}
