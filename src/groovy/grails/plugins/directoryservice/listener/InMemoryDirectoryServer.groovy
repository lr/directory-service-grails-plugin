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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.unboundid.ldap.listener.InMemoryDirectoryServer as InMemServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerSnapshot
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.schema.Schema
import com.unboundid.ldif.LDIFException
import com.unboundid.ldif.LDIFReader

/**
 * This class is a wrapper around the UnboundID InMemoryDirectoryServer.
 * The purpose of this class is to make it easy to develop and test your
 * application, as you do not have to have a fully functional directory
 * server at your disposal at all times.
 *
 * For years I have developed applications against directory servers and I
 * have usually had to connect to a real server running on our network, or go
 * through the effort of running one on my laptop. With the
 * InMemoryDirectoryServer, you can, with little effort, have your own
 * directory server for development and testing, <strong>especially for
 * testing</strong>! Creating test data in a live directory server is very
 * problematic, and this class makes it really easy to use a directory
 * server the same way you would a test database. See the test cases for this
 * project to see how useful it can be.
 *
 * If you want to use the InMemoryDirectoryServer for development mode, you can
 * start it up in <code>conf/BootStrap.groovy</code>:
 *
 * <pre>
 * import grails.plugins.directoryservice.listener.InMemoryDirectoryServer
 * ...
 * def inMemServer
 * ...
 * def init = { servletContext ->
 *    def config = grails.util.GrailsConfig.grails.plugins.directoryservice.sources['some source']
 *    inMemServer = new InMemoryDirectoryServer(
 *      "dc=someu,dc=edu",
 *       config,
 *      "path/to/some/schema.ldif",
 *      "path/to/some/data.ldif"
 *    )
 * }
 *
 * def destroy = {
 *    inMemServer?.shutDown()
 * }
 * </pre>
 *
 * Since the server runs in memory, all data in it will be lost when it is shut
 * down, so if you may want to save the contents, you can call
 * export(exportPath) before you shut down:
 *
 * <pre>
 * def destroy = {
 *    inMemServer?.export("path/to/export.ldif")
 *    inMemServer?.shutDown()
 * }
 * </pre>
 *
 * This only supports non-SSL connections, and does not support ACIs, so
 * obviously it is not to be used as a real directory server, unless you
 * protect it from the outside world.
 *
 * @author Lucas Rockwell
 * @see <a href="https://www.unboundid.com/products/ldap-sdk/docs/javadoc/com/unboundid/ldap/listener/InMemoryDirectoryServer.html">UnboundID InMemoryDirectoryServer</a>
 */
class InMemoryDirectoryServer {

    private Logger log = LoggerFactory.getLogger(getClass().name)

    private LDAPConnection conn

    private InMemServer server

    /**
     * Creates an InMemoryDirectoryServer object. If you use this, it is up to
     * you to call the setup(), and startListening() methods.
     */
    InMemoryDirectoryServer() {}

    /**
     * Creates an InMemoryDirectoryServer object using the passed in baseDN,
     * props, schema from schemaPath, and then loads the server with contents
     * of contentsPath. If everything loads properly, it calls startListening().
     *
     * The map argument should contain the following:
     *
     * <ul>
     *  <li>bindDN</li>
     *  <li>bindPassword</li>
     *  <li>port</li>
     * </ul>
     *
     * The reason it is a map is so you can just pass in one of your
     * <code>grails.plugins.directoryservice.sources</code> map objects.
     *
     * For details on schema and example data, see
     * <a href="https://github.com/lr/directory-service/tree/master/test/ldif">
     * DirectoryService test LDIF data</a>.
     *
     * @param baseDN        The baseDN of the directory server.
     * @param props         A map of properties.
     * @param schemaPath    The path to the schema that should be used in
     * the directory. This schema is merged with the standard schema which is
     * part of the UnboundID InMemoryDirectoryServer. If you do not want to
     * add any additional schema, just leave this as an empty string, or
     * {@code null}.
     * @param contentsPath  The path to the LDIF that contains the contents
     * of the directory. If you do want to import any data, you can leave this
     * as an empty string, or {@code null}. If you do not add at least the
     * domain object and one OU, you will have to add programatically.
     * @see #startListening()
     */
    InMemoryDirectoryServer(String baseDN, Map props, String schemaPath,
        String contentsPath) {

        setup(
            baseDN,
            props.bindDN,
            props.bindPassword,
            Integer.parseInt(props.port),
            schemaPath,
            contentsPath
        )
        try {
            startListening()
        }
        catch(LDAPException e) {
            log.error "Could not start InMemoryDirectoryServer listener: $e.message $props", e
        }
    }

    /**
     * Sets up the InMemoryDirectoryServer using the passed in baseDN,
     * bindDN, bindDN, loads the schema from the passed in schemaPath, and
     * then loads the content from the passed in contentsPath.
     *
     * For details on schema and example data, see
     * <a href="https://github.com/lr/directory-service/tree/master/test/ldif">
     * DirectoryService test LDIF data</a>.
     *
     * @param baseDN        The baseDN of the directory server.
     * @param bindDN        The DN to use for authenticated binds.
     * @param bindPassword  The password that goes with the bindDN.
     * @param schemaPath    The path to the schema that should be used in
     * the directory. This schema is merged with the standard schema which is
     * part of the UnboundID InMemoryDirectoryServer. If you do not want to
     * add any additional schema, just leave this as an empty string, or
     * {@code null}.
     * @param contentsPath  The path to the LDIF that contains the contents
     * of the directory. If you do want to import any data, you can leave this
     * as an empty string, or {@code null}. If you do not add at least the
     * domain object and one OU, you will have to add programatically.
     */
    void setup(String baseDN, String bindDN, String bindPassword,
        int port, String schemaPath, String contentsPath) {

        try {
            InMemoryDirectoryServerConfig dirConfig =
                new InMemoryDirectoryServerConfig(baseDN)

            // Using the baseDN as the name, could be anything.
            InMemoryListenerConfig listenConfig =
                InMemoryListenerConfig.createLDAPConfig(baseDN, port)

            dirConfig.addAdditionalBindCredentials(bindDN, bindPassword)

            // Import schema if schemaPath is not blank/null.
            if (schemaPath) {
                dirConfig.setSchema(Schema.mergeSchemas(
                    schemaEntryFromLDIF(schemaPath), dirConfig.getSchema()))
            }

            dirConfig.setListenerConfigs(listenConfig)

            // Create the server
            server = new InMemServer(dirConfig)

            // Import data if contentsPath is not blank/null.
            if (contentsPath) {
                server.importFromLDIF(true, contentsPath)
            }
        }
        catch (LDAPException e) {
            log.error "Could not create InMemoryDirectoryServer: $e.message", e
        }
    }

    /**
     * If the UnboundID InMemoryDirectoryServer has been set up, it calls
     * startListening() on that object.
     */
    void startListening() {
        server?.startListening()
    }

    /**
     * If the UnboundID InMemoryDirectoryServer has been set up, it calls
     * shutDown(true) on that object.
     */
    void shutDown() {
        server?.shutDown(true)
    }

    /**
     * If the UnboundID InMemoryDirectoryServer has been set up, it calls
     * getListenPort() on that object.
     *
     * @return The port on which the InMemoryDirectoryServer is listening.
     */
    int listenPort() {
        return server?.getListenPort()
    }

    /**
     * If the UnboundID InMemoryDirectoryServer has been set up, it calls
     * exportToLDIF(exportPath, exportGeneratedAttrs, false).
     *
     * @param exportPath            The file path to which you should export
     * the data.
     * @param excludeGeneratedAttrs  Whether or not to export any attributes
     * which have been generated by the server, like creatorsName, entryUUID,
     * etc. The default is {@code true}.
     */
    void export(String exportPath, Boolean excludeGeneratedAttrs=true) {
        server?.exportToLDIF(exportPath, excludeGeneratedAttrs, false)
    }

    /**
     * Reads the schema entry from the provided LDIF file, and turns it into a
     * {@code Schema} object. It assumes that there is only one entry in the
     * provided LDIF file, as schema is contained in one entry. If there is more
     * than one entry in the file, only the first one will be read.
     *
     * If it encounters an error while processing, it creates an empty schema
     * entry with the DN set to {@code cn=schema}.
     *
     * @param path        The path to the LDIF file which contains an LDAP
     * schema. It must not be {@code null}.
     * @return A {@code Schema} object.
     */
    Schema schemaEntryFromLDIF(String path) {
        LDIFReader ldifReader = new LDIFReader(path)

        // Schema should have one entry, so that is what we are going
        // to assume here.
        try {
            Entry entry = ldifReader.readEntry()
            return new Schema(entry)
        }
        catch(LDIFException e) {
            log.error "Could not parse the ldif: $e.message", e
            return new Schema(new Entry("cn=schema"))
        }
        catch(IOException e) {
            log.error "Could not read the provided file (${schemaLDIF}): $e.message", e
            return new Schema(new Entry("cn=schema"))
        }
        finally {
            ldifReader.close()
        }
    }

    InMemoryDirectoryServerSnapshot createSnapshot() {
        return server.createSnapshot()
    }

    def restoreSnapshot(InMemoryDirectoryServerSnapshot snapshot) {
        server.restoreSnapshot(snapshot)
    }
}
