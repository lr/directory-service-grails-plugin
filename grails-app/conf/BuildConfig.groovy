grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

    inherits 'global'
    log 'warn'

    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        runtime 'com.unboundid:unboundid-ldapsdk:4.0.3'
    }

    plugins {
        build ':release:2.2.1', ':rest-client-builder:1.0.3', {
            export = false
        }

        //test(':spock:0.6') { export = false }
    }
}
