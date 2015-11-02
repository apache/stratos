/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.manager.context;

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.services.DistributedObjectProvider;
import org.apache.stratos.manager.internal.ServiceReferenceHolder;
import org.apache.stratos.manager.registry.RegistryManager;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * Stratos manager context.
 */
public class StratosManagerContext implements Serializable {

    public static final String DATA_RESOURCE = "/stratos.manager/data";
    private static final String SM_CARTRIDGE_TYPE_TO_CARTIDGE_GROUPS_MAP = "SM_CARTRIDGE_TYPE_TO_CARTIDGE_GROUPS_MAP";
    private static final String SM_CARTRIDGE_TYPE_TO_APPLICATIONS_MAP = "SM_CARTRIDGE_TYPE_TO_APPLICATIONS_MAP";
    private static final String SM_CARTRIDGE_GROUP_TO_CARTIDGE_GROUPS_MAP = "SM_CARTRIDGE_GROUP_TO_CARTIDGE_GROUPS_MAP";
    private static final String SM_CARTRIDGE_GROUP_TO_APPLICATIONS_MAP = "SM_CARTRIDGE_GROUP_TO_APPLICATIONS_MAP";

    private static final String SM_CARTRIDGES_CARTRIDGEGROUPS_WRITE_LOCK = "SM_CARTRIDGES_CARTRIDGEGROUPS_WRITE_LOCK";
    private static final String SM_CARTRIDGES_APPLICATIONS_WRITE_LOCK = "SM_CARTRIDGES_APPLICATIONS_WRITE_LOCK";
    private static final String SM_CARTRIDGEGROUPS_CARTRIDGESUBGROUPS_WRITE_LOCK = "SM_CARTRIDGEGROUPS_CARTRIDGESUBGROUPS_WRITE_LOCK";
    private static final String SM_CARTRIDGEGROUPS_APPLICATIONS_WRITE_LOCK = "SM_CARTRIDGEGROUPS_APPLICATIONS_WRITE_LOCK";
    private static final Log log = LogFactory.getLog(StratosManagerContext.class);
    private static volatile StratosManagerContext instance;
    private final transient DistributedObjectProvider distributedObjectProvider;
    /**
     * Key - cartridge type
     * Value - list of cartridgeGroupNames
     */
    private Map<String, Set<String>> cartridgeTypeToCartridgeGroupsMap;

    /**
     * Key - cartridge type
     * Value - list of ApplicationNames
     */
    private Map<String, Set<String>> cartridgeTypeToApplicationsMap;

    /**
     * Key - cartridge group name
     * Value - list of cartridgeGroupNames
     */
    private Map<String, Set<String>> cartridgeGroupToCartridgeSubGroupsMap;

    /**
     * Key - cartridge group name
     * Value - list of ApplicationNames
     */
    private Map<String, Set<String>> cartridgeGroupToApplicationsMap;

    private boolean clustered;
    private boolean coordinator;

    private StratosManagerContext() {
        // Initialize clustering status
        AxisConfiguration axisConfiguration = ServiceReferenceHolder.getInstance().getAxisConfiguration();
        if ((axisConfiguration != null) && (axisConfiguration.getClusteringAgent() != null)) {
            clustered = true;
        }

        // Initialize distributed object provider
        distributedObjectProvider = ServiceReferenceHolder.getInstance().getDistributedObjectProvider();

        // Get maps from distributed object provider
        cartridgeTypeToCartridgeGroupsMap = distributedObjectProvider.getMap(SM_CARTRIDGE_TYPE_TO_CARTIDGE_GROUPS_MAP);
        cartridgeTypeToApplicationsMap = distributedObjectProvider.getMap(SM_CARTRIDGE_TYPE_TO_APPLICATIONS_MAP);
        cartridgeGroupToCartridgeSubGroupsMap = distributedObjectProvider.getMap(SM_CARTRIDGE_GROUP_TO_CARTIDGE_GROUPS_MAP);
        cartridgeGroupToApplicationsMap = distributedObjectProvider.getMap(SM_CARTRIDGE_GROUP_TO_APPLICATIONS_MAP);

        // Update context from the registry
        updateContextFromRegistry();
    }

    public static StratosManagerContext getInstance() {
        if (instance == null) {
            synchronized (StratosManagerContext.class) {
                if (instance == null) {
                    instance = new StratosManagerContext();
                }
            }
        }
        return instance;
    }

    public boolean isCoordinator() {
        return coordinator;
    }

    public void setCoordinator(boolean coordinator) {
        this.coordinator = coordinator;
    }

    public boolean isClustered() {
        return clustered;
    }

    private Lock acquireWriteLock(String object) {
        return distributedObjectProvider.acquireLock(object);
    }

    public void releaseWriteLock(Lock lock) {
        distributedObjectProvider.releaseLock(lock);
    }

    public Lock acquireCartridgesCartridgeGroupsWriteLock() {
        return acquireWriteLock(SM_CARTRIDGES_CARTRIDGEGROUPS_WRITE_LOCK);
    }

    public Lock acquireCartridgesApplicationsWriteLock() {
        return acquireWriteLock(SM_CARTRIDGES_APPLICATIONS_WRITE_LOCK);
    }

    public Lock acquireCartridgeGroupsCartridgeSubGroupsWriteLock() {
        return acquireWriteLock(SM_CARTRIDGEGROUPS_CARTRIDGESUBGROUPS_WRITE_LOCK);
    }

    public Lock acquireCartridgeGroupsApplicationsWriteLock() {
        return acquireWriteLock(SM_CARTRIDGEGROUPS_APPLICATIONS_WRITE_LOCK);
    }

    public void addUsedCartridgesInCartridgeGroups(String cartridgeGroupName, String[] cartridgeNames) {
        if (cartridgeNames == null) {
            return;
        }

        for (String cartridgeName : cartridgeNames) {
            Set<String> cartridgeGroupNames = null;
            if (cartridgeTypeToCartridgeGroupsMap.containsKey(cartridgeName)) {
                cartridgeGroupNames = cartridgeTypeToCartridgeGroupsMap.get(cartridgeName);
            } else {
                cartridgeGroupNames = new HashSet<String>();
                cartridgeTypeToCartridgeGroupsMap.put(cartridgeName, cartridgeGroupNames);
            }
            cartridgeGroupNames.add(cartridgeGroupName);
        }
    }

    public void removeUsedCartridgesInCartridgeGroups(String cartridgeGroupName, String[] cartridgeNames) {
        if (cartridgeNames == null) {
            return;
        }

        for (String cartridgeName : cartridgeNames) {
            Set<String> cartridgeGroupNames = null;
            if (cartridgeTypeToCartridgeGroupsMap.containsKey(cartridgeName)) {
                cartridgeGroupNames = cartridgeTypeToCartridgeGroupsMap.get(cartridgeName);
                // Remove current cartridge group name
                cartridgeGroupNames.remove(cartridgeGroupName);
                // Remove entry if there are no more cartridge group names for that cartridge type
                if (cartridgeGroupNames.isEmpty()) {
                    cartridgeGroupNames = null;
                    cartridgeTypeToCartridgeGroupsMap.remove(cartridgeName);
                }
            }
        }
    }

    public boolean isCartridgeIncludedInCartridgeGroups(String cartridgeName) {
        if (cartridgeTypeToCartridgeGroupsMap.containsKey(cartridgeName)) {
            if (!cartridgeTypeToCartridgeGroupsMap.get(cartridgeName).isEmpty()) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void addUsedCartridgesInApplications(String applicationName, String[] cartridgeNames) {
        if (cartridgeNames == null) {
            return;
        }

        for (String cartridgeName : cartridgeNames) {
            Set<String> applicationNames = null;
            if (cartridgeTypeToApplicationsMap.containsKey(cartridgeName)) {
                applicationNames = cartridgeTypeToApplicationsMap.get(cartridgeName);
            } else {
                applicationNames = new HashSet<String>();
                cartridgeTypeToApplicationsMap.put(cartridgeName, applicationNames);
            }
            applicationNames.add(applicationName);
        }
    }

    public void removeUsedCartridgesInApplications(String applicationName, String[] cartridgeNames) {
        if (cartridgeNames == null) {
            return;
        }

        for (String cartridgeName : cartridgeNames) {
            Set<String> applicationNames = null;
            if (cartridgeTypeToApplicationsMap.containsKey(cartridgeName)) {
                applicationNames = cartridgeTypeToApplicationsMap.get(cartridgeName);
                // Remove current application name
                applicationNames.remove(applicationName);
                // Remove entry if there are no more cartridge group names for that cartridge type
                if (applicationNames.isEmpty()) {
                    applicationNames = null;
                    cartridgeTypeToApplicationsMap.remove(cartridgeName);
                }
            }
        }
    }

    public boolean isCartridgeIncludedInApplications(String cartridgeName) {
        if (cartridgeTypeToApplicationsMap.containsKey(cartridgeName)) {
            if (!cartridgeTypeToApplicationsMap.get(cartridgeName).isEmpty()) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void addUsedCartridgeGroupsInCartridgeSubGroups(String cartridgeSubGroupName, String[] cartridgeGroupNames) {
        if (cartridgeGroupNames == null) {
            return;
        }

        for (String cartridgeGroupName : cartridgeGroupNames) {
            Set<String> cartridgeSubGroupNames = null;
            if (cartridgeGroupToCartridgeSubGroupsMap.containsKey(cartridgeGroupName)) {
                cartridgeSubGroupNames = cartridgeGroupToCartridgeSubGroupsMap.get(cartridgeGroupName);
            } else {
                cartridgeSubGroupNames = new HashSet<String>();
                cartridgeGroupToCartridgeSubGroupsMap.put(cartridgeSubGroupName, cartridgeSubGroupNames);
            }
            cartridgeSubGroupNames.add(cartridgeGroupName);
        }
    }

    public void removeUsedCartridgeGroupsInCartridgeSubGroups(String cartridgeSubGroupName, String[] cartridgeGroupNames) {
        if (cartridgeGroupNames == null) {
            return;
        }

        for (String cartridgeGroupName : cartridgeGroupNames) {
            Set<String> cartridgeSubGroupNames = null;
            if (cartridgeGroupToCartridgeSubGroupsMap.containsKey(cartridgeGroupName)) {
                cartridgeSubGroupNames = cartridgeGroupToCartridgeSubGroupsMap.get(cartridgeGroupName);
                // Remove current cartridge group name
                cartridgeSubGroupNames.remove(cartridgeGroupName);
                // Remove entry if there are no more cartridge group names for that cartridge type
                if (cartridgeSubGroupNames.isEmpty()) {
                    cartridgeSubGroupNames = null;
                    cartridgeGroupToCartridgeSubGroupsMap.remove(cartridgeGroupName);
                }
            }
        }
    }

    public boolean isCartridgeGroupIncludedInCartridgeSubGroups(String cartridgeGroupName) {
        if (cartridgeGroupToCartridgeSubGroupsMap.containsKey(cartridgeGroupName)) {
            if (!cartridgeGroupToCartridgeSubGroupsMap.get(cartridgeGroupName).isEmpty()) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void addUsedCartridgeGroupsInApplications(String applicationName, String[] cartridgeGroupNames) {
        if (cartridgeGroupNames == null) {
            return;
        }

        for (String cartridgeGroupName : cartridgeGroupNames) {
            Set<String> applicationNames = null;
            if (cartridgeGroupToApplicationsMap.containsKey(cartridgeGroupName)) {
                applicationNames = cartridgeGroupToApplicationsMap.get(cartridgeGroupName);
            } else {
                applicationNames = new HashSet<String>();
                cartridgeGroupToApplicationsMap.put(cartridgeGroupName, applicationNames);
            }
            applicationNames.add(applicationName);
        }
    }

    public void removeUsedCartridgeGroupsInApplications(String applicationName, String[] cartridgeGroupNames) {
        if (cartridgeGroupNames == null) {
            return;
        }

        for (String cartridgeGroupName : cartridgeGroupNames) {
            Set<String> applicationNames = null;
            if (cartridgeGroupToApplicationsMap.containsKey(cartridgeGroupName)) {
                applicationNames = cartridgeGroupToApplicationsMap.get(cartridgeGroupName);
                // Remove current application name
                applicationNames.remove(applicationName);
                // Remove entry if there are no more cartridge group names for that cartridge type
                if (applicationNames.isEmpty()) {
                    applicationNames = null;
                    cartridgeGroupToApplicationsMap.remove(cartridgeGroupName);
                }
            }
        }
    }

    public boolean isCartridgeGroupIncludedInApplications(String cartridgeGroupName) {
        if (cartridgeGroupToApplicationsMap.containsKey(cartridgeGroupName)) {
            if (!cartridgeGroupToApplicationsMap.get(cartridgeGroupName).isEmpty()) {
                return true;
            }
            return false;
        }
        return false;
    }

    private void updateContextFromRegistry() {
        if ((!isClustered()) || (isCoordinator())) {
            try {
                Object dataObj = RegistryManager.getInstance().read(DATA_RESOURCE);
                if (dataObj != null) {
                    if (dataObj instanceof StratosManagerContext) {
                        StratosManagerContext serializedObj = (StratosManagerContext) dataObj;

                        copyMap(serializedObj.cartridgeTypeToCartridgeGroupsMap, cartridgeTypeToCartridgeGroupsMap);
                        copyMap(serializedObj.cartridgeTypeToApplicationsMap, cartridgeTypeToApplicationsMap);
                        copyMap(serializedObj.cartridgeGroupToCartridgeSubGroupsMap, cartridgeGroupToCartridgeSubGroupsMap);
                        copyMap(serializedObj.cartridgeGroupToApplicationsMap, cartridgeGroupToApplicationsMap);

                        if (log.isDebugEnabled()) {
                            log.debug("Stratos Manager context is read from the registry");
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Stratos Manager context could not be found in the registry");
                        }
                    }
                }
            } catch (Exception e) {
                String msg = "Unable to read Stratos Manager context from the registry. " +
                        "Hence, any historical data will not be reflected";
                log.warn(msg, e);
            }
        }
    }

    private void copyMap(Map sourceMap, Map destinationMap) {
        for (Object key : sourceMap.keySet()) {
            destinationMap.put(key, sourceMap.get(key));
        }
    }

    public void persist() {
        if ((!isClustered()) || (isCoordinator())) {
            try {
                RegistryManager.getInstance().persist(DATA_RESOURCE, this);
            } catch (RegistryException e) {
                log.error("Could not persist cloud controller context in registry", e);
            }
        }
    }
}
