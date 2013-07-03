/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.stratos.cartridge.agent.registrant;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.ClusteringClient;
import org.apache.stratos.cartridge.agent.exception.CartridgeAgentException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;

/**
 * Utility method collection for handling {@link Registrant}s
 *
 * @see Registrant
 */
public class RegistrantUtil {
    private static final Log log = LogFactory.getLog(RegistrantUtil.class);
    private static boolean registrantsReloaded = false;
    /**
     * Before adding a member, we will try to verify whether we can connect to it
     *
     * @param registrant The member whose connectvity needs to be verified
     * @return true, if the member can be contacted; false, otherwise.
     */
    public static boolean isHealthy(Registrant registrant) {
        if (log.isDebugEnabled()) {
            log.debug("Trying to connect to registrant " + registrant + "...");
        }
        if(log.isDebugEnabled()) {
            log.debug("***********************************************isHealthy() method for registrant " + registrant);
        }
        String registrantRemoteHost = registrant.getRemoteHost();
        log.debug("remote host : " + registrantRemoteHost);
        if(registrantRemoteHost == null){
            registrantRemoteHost = "localhost";
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(registrantRemoteHost);
            if(log.isDebugEnabled()) {
                log.debug("***********************************************Host resolved for registrant " + registrant);
            }
        } catch (UnknownHostException e) {
            log.error("Registrant " + registrant + " is unhealthy");
            return false;
        }
        PortMapping[] portMappings = registrant.getPortMappings();

        int maxRetries = Integer.parseInt(System.getProperty("clustering.maxRetries"));

        for (int retries = maxRetries; retries > 0; retries--) {
            try {
                for (PortMapping portMapping : portMappings) {
                    int port = portMapping.getPrimaryPort();
                    if(log.isDebugEnabled()) {
                        log.debug("***********************************************primary port" + port + " registrant " + registrant);
                    }
                    if (port != -1 && port != 0) {
                        if(log.isDebugEnabled()) {
                            log.debug("***********************************************connecting to " + registrant +
                                    " re-try: " + retries);
                        }
                        SocketAddress httpSockaddr = new InetSocketAddress(addr, port);
                        new Socket().connect(httpSockaddr, 10000);
                        if(log.isDebugEnabled()) {
                            log.debug("***********************************************connected successfully to port: " + port);
                        }
                    }
                }
                return true;
            } catch (IOException e) {
				if (log.isDebugEnabled()) {
					log.debug("Error occurred.. " + e.getMessage());
				}
                String msg = e.getMessage();
                if (!msg.contains("Connection refused") && !msg.contains("connect timed out")) {
                    String msg2 = "Cannot connect to registrant " + registrant;
                    log.error(msg2, e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
        return false;
    }

    /**
     * Reload all the registrants persisted in the file system
     * @param clusteringClient ClusteringClient
     * @param configurationContext   ConfigurationContext
     * @param registrantDatabase  RegistrantDatabase
     * @throws CartridgeAgentException If reloading registrants fails
     */
    public static void reloadRegistrants(ClusteringClient clusteringClient,
                                         ConfigurationContext configurationContext,
                                         RegistrantDatabase registrantDatabase) throws CartridgeAgentException {

        if(registrantsReloaded) {
            log.info("Registrants already re-loaded, therefore not re-loading again");
            return;
        }

        File registrants = new File("registrants");
        if (!registrants.exists()) {
            log.info("Registrant information doesn't exist in the file system");
            return;
        }
        File[] files = registrants.listFiles();
        if (files == null) {
            log.error("Directory 'Registrants' is invalid");
            return;

        } else if (files.length == 0) {
            log.info("No registrant information found in the Registrants directory");
            return;
        }

        for (File file : files) {
            try {
                Registrant registrant =
                        deserializeRegistrant("registrants" + File.separator + file.getName());
                if (!registrantDatabase.containsActive(registrant)) {
                    clusteringClient.joinGroup(registrant, configurationContext);
                }
            } catch (IOException e) {
                log.error("Cannot deserialize registrant file " + file.getName(), e);
            }
        }
        registrantsReloaded = true;
    }

    private static Registrant deserializeRegistrant(String fileName) throws IOException {
        Registrant registrant = null;
        ObjectInputStream in = null;

        try {
            // Deserialize from a file
            File file = new File(fileName);
            in = new ObjectInputStream(new FileInputStream(file));
            // Deserialize the object
            registrant = (Registrant) in.readObject();
        } catch (ClassNotFoundException ignored) {
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return registrant;
    }
    
    
}
