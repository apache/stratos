/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.cartridge.agent.registrant;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cartridge.agent.InstanceStateNotificationClientThread;
import org.wso2.carbon.cartridge.agent.exception.CartridgeAgentException;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class represents a database for {@link Registrant}s. Registrants added to this database will be
 * persisted, so that when the Cartridge Agent is restarted, the Registrants can be restored.
 *
 * @see Registrant
 */
public class RegistrantDatabase {
    private static final Log log = LogFactory.getLog(RegistrantDatabase.class);

    private List<Registrant> registrants = new CopyOnWriteArrayList<Registrant>();

    public void add(Registrant registrant) throws CartridgeAgentException {
        if (registrants.contains(registrant) && registrant.running()) {
            throw new CartridgeAgentException("Active registrant with key " +
                                              registrant.getKey() + " already exists");
        }
        synchronized (registrant) {
            if (!isAlreadyAdded(registrant)) {
                persist(registrant);
                registrants.add(registrant);
                log.info("Added registrant " + registrant);

            } else {
                log.info("Registrant " + registrant + "has been already added");
            }
        }
    }

    private void persist(Registrant registrant) throws CartridgeAgentException {
        try {
            ObjectOutput out = null;
            try {
                // Serialize to a file
                if (!new File("registrants").exists() && !new File("registrants").mkdirs()) {
                    throw new IOException("Cannot create registrants directory");
                }
                out = new ObjectOutputStream(new FileOutputStream("registrants" + File.separator +
                                                                  registrant.getKey() + ".ser"));
                out.writeObject(registrant);
                out.close();
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (IOException e) {
            log.error("Could not serialize registrant " + registrant, e);
        }
    }

    public void stopAll() {
        for (Registrant registrant : registrants) {
        	new Thread(new InstanceStateNotificationClientThread(registrant, "INACTIVE")).start();
            registrant.stop();
        }
    }

    public boolean containsActive(Registrant registrant) {
        return registrants.contains(registrant) &&
               registrants.get(registrants.indexOf(registrant)).running();
    }

    public List<Registrant> getRegistrants() {
        return Collections.unmodifiableList(registrants);
    }

    public boolean isAlreadyAdded(Registrant registrant) {

        boolean alreadyAdded = false;
        for (Registrant registrantFromDb : registrants) {
            if(registrantFromDb.getRemoteHost().equals(registrant.getRemoteHost())) {

                PortMapping[] portMappingofRegistrantOfDB = registrantFromDb.getPortMappings();
                PortMapping[] portMappingofRegistrant = registrant.getPortMappings();

                if(portMappingofRegistrant.length != portMappingofRegistrantOfDB.length) {
                    continue;

                } else {
                    alreadyAdded = checkPortMappings(registrant, registrantFromDb);
                }

            } else {
                continue;
            }
        }
        return alreadyAdded;
    }

    private boolean checkPortMappings (Registrant newRegistrant, Registrant existingRegistrant) {

        PortMapping[] portMappingsOfNewRegistrant = newRegistrant.getPortMappings();
        PortMapping[] portMappingsOfExistingRegistrant = existingRegistrant.getPortMappings();

        for (PortMapping portMappingOfNewRegistrant : portMappingsOfNewRegistrant) {
            boolean matchFound = false;
            for (PortMapping portMappingOfExistingRegistrant : portMappingsOfExistingRegistrant) {
                if(portMappingOfExistingRegistrant.equals(portMappingOfNewRegistrant)) {
                    matchFound = true;
                    break;
                }
            }
            if(!matchFound) {
                return false;
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("***********************************Found matching registrant for " + newRegistrant + " in the Registrant database");
        }
        return true;
    }
}
