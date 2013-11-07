class DirectoryServiceGrailsPlugin {
    // the plugin version
    def version = "0.9.0"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Directory Service Plugin" // Headline display name of the plugin
    def author = "Lucas Rockwell"
    def authorEmail = "lr@lucasrockwell.com"
    def description = '''\
Grails Plugin which makes interacting with directory (LDAP) servers a breeze.
'''

    // URL to the plugin's documentation
    def documentation = "http://lr.github.com/directory-service/"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "Apache2"

    // Any additional developers beyond the author specified above.
    def developers = [ [ name: "Lucas Rockwell", email: "lr@lucasrockwell.com" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "GitHub", url: "https://github.com/lr/directory-service/issues" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://github.com/lr/directory-service/" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
