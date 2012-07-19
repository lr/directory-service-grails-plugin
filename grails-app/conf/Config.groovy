// configuration for plugin testing - will not be included in the plugin zip

environments {
    test {
        
        // There is nothing running on my dev machine on
        // port 11389, so by doing this we are always testing
        // failover! Other, over-the-wire servers have been tested
        // as well.
        ds.sources = [
            'directory':[
                'address': 'localhost , localhost',
                'port': ' 11389 ,33389',
                'useSSL': false,
                'trustSSLCert': true,
                'followReferrals': true,
                'bindDN': 'cn=Directory Manager',
                'bindPassword': 'password'
            ],
            'ad':[
                'address': 'localhost',
                'port': '33268',
                'useSSL': false,
                'trustSSLCert': true,
                'followReferrals': false,
                'bindDN': 'cn=AD Manager',
                'bindPassword': 'password'
            ],
            'directoryAnonymous':[
                'address': 'localhost',
                'port': '33389',
                'useSSL': false,
                'trustSSLCert': true,
                'followReferrals': true
            ]
        ]
        
        // Since we are testing failover above, we need another
        // Map of source details for the InMemoryServer. This is
        // just made up for our tests, and is not part of the API
        // for this project.
        ds.sourcesForInMemoryServer = [
            'directory':[
                'address':'localhost',
                'port':'33389',
                'useSSL':false,
                'trustSSLCert':true,
                'bindDN':'cn=Directory Manager',
                'bindPassword':'password'
            ],
            'ad':[
                'address':'localhost',
                'port':'33268',
                'useSSL':false,
                'trustSSLCert':true,
                'bindDN':'cn=AD Manager',
                'bindPassword':'password'
            ]
        ]
        
        ds.dit = [
            'ou=people,dc=someu,dc=edu':[
                'singular':'person',
                'plural':'people',
                'rdnAttribute':'uid',
                'source':'directory'
            ],
            'ou=departments,dc=someu,dc=edu':[
                'singular':'department',
                'plural':'departments',
                'rdnAttribute':'ou',
                'source':'directory'
            ],
            'ou=groups,dc=someu,dc=edu':[
                'singluar':'group',
                'plural':'groups',
                'rdnAttribute':'cn',
                'source':'directory'
            ],
            'ou=personnes,dc=someu,dc=edu':[
                'singluar':'personne',
                'plural':'personnes',
                'rdnAttribute':'uid',
                'source':'directory'
            ],
            'ou=accounts,dc=someu,dc=edu':[
                'singular':'account',
                'plural':'accounts',
                'rdnAttribute':'cn',
                'source':'ad'
            ],
            'ou=Economics,ou=accounts,dc=someu,dc=edu':[
                'singular':'economist',
                'plural':'economists',
                'rdnAttribute':'cn',
                'source':'ad'
            ],
            'ou=People,dc=someu,dc=edu':[
                'singular':'peep',
                'plural':'peeps',
                'rdnAttribute':'uid',
                'source':'directoryAnonymous'
            ],
        ]
        
    }
}

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'

    warn   'org.mortbay.log'
}
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"
