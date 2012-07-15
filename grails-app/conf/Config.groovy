// configuration for plugin testing - will not be included in the plugin zip

environments {
    test {
        
        ldap.sources = [
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
        
        ldap.dit = [
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
