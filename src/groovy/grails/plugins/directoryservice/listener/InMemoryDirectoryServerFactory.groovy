/*
 * Copyright 2012 Lucas Rockwell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugins.directoryservice.listener
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired

/**
 * This class is a factory wrapper for the InMemoryDirectoryServer
 *
 * It allows you to define your server in resources.groovy instead of running it from Bootstrap.groovy
 *
 * in resources.groovy you could do:
 *
 * <pre>
 *     if(Environment.current == Environment.TEST) {
 *              inMemServer(InMemoryDirectoryServerFactory) {
 *              baseDN = 'dc=someu,dc=edu'
 *              configurationName = 'grails.plugins.directoryservice.sourcesForInMemoryServer.directory'
 *              schemaPath = 'test/ldif/schema/directory-schema.ldif'
 *              contentsPath = 'test/ldif/directory.ldif'
 *          }
 *     }
 * </pre>
 *
 * It will start the server on context start and shut it down when Grails shuts down.
 */
class InMemoryDirectoryServerFactory implements FactoryBean<InMemoryDirectoryServer>, InitializingBean, DisposableBean{

    @Autowired
    GrailsApplication grailApplication

    String baseDN
    String configurationName
    String schemaPath
    String contentsPath

    private InMemoryDirectoryServer server

    @Override
    InMemoryDirectoryServer getObject() throws Exception {
        return server
    }


    @Override
    Class<?> getObjectType() {
        return InMemoryDirectoryServer
    }

    @Override
    boolean isSingleton() {
        return true
    }

    @Override
    void destroy() throws Exception {
        server.shutDown()
    }

    @Override
    void afterPropertiesSet() throws Exception {
        def path = configurationName.split(/\./) as List

        def configuration = resolveConfiguration(grailApplication.config, path)
        server = new InMemoryDirectoryServer(baseDN, configuration, schemaPath, contentsPath)
    }

    private def resolveConfiguration(Map map, List<String> path) {
        def root = map.get(path[0])
        if(path.size() > 1) {
            return resolveConfiguration(root, path[1..-1])
        }
        return root
    }

}
