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

import com.unboundid.ldap.listener.InMemoryDirectoryServer as InMemServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.schema.Schema
import com.unboundid.ldif.LDIFException
import com.unboundid.ldif.LDIFReader

import org.apache.log4j.Logger

/**
 *
 *
 *
 * @author Lucas Rockwell
 */
class InMemoryDirectoryServer {
    
    Logger log = Logger.getLogger(InMemoryDirectoryServer.class)
    
    private LDAPConnection conn
    
    private InMemServer server
    
    public InMemoryDirectoryServer() {}
    
    public InMemoryDirectoryServer(String baseDN, Map props, String schemaPath,
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
            log.error "Could not start InMemoryServer listener: ${e.getMessage()}"
        }
        
    }
    
    public void setup(String baseDN, String bindDN, String bindPassword,
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
            log.error "Could not create InMemoryDirectoryServer: ${e.getMessage()}"
        }
    }
    
    public void startListening() {
        server?.startListening()
    }
    
    public void shutDown() {
        server?.shutDown(true)
    }

    public int listenPort() {
        return server.getListenPort()
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
    public Schema schemaEntryFromLDIF(String path) {
        LDIFReader ldifReader = new LDIFReader(path)
        
        // Schema should have one entry, so that is what we are going
        // to assume here.
        try {
            Entry entry = ldifReader.readEntry()
            return new Schema(entry)
        }
        catch(LDIFException e) {
            log.error "Could not parse the ldif: ${e.getMessage()}"
            return new Schema(new Entry("cn=schema"))
        }
        catch(IOException e) {
            log.error "Could not read the provided file (${schemaLDIF}): ${e.getMessage()}"
            return new Schema(new Entry("cn=schema"))
        }
        finally {
            ldifReader.close()
        }
        
    }
    
}