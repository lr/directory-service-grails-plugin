grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

    inherits 'global'
    log 'warn'

    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        runtime 'com.unboundid:unboundid-ldapsdk:2.3.6'
    }

    plugins {
        //noinspection GroovyAssignabilityCheck
        build(":release:3.0.1",
                ":rest-client-builder:2.0.3") {
            export = false
        }


        //test(':spock:0.6') { export = false }
    }
}
