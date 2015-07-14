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

    private static volatile StratosManagerContext instance;

    private static final String SM_CARTRIDGE_TYPE_TO_CARTIDGE_GROUPS_MAP = "SM_CARTRIDGE_TYPE_TO_CARTIDGE_GROUPS_MAP";
    private static final String SM_CARTRIDGE_TYPE_TO_APPLICATIONS_MAP = "SM_CARTRIDGE_TYPE_TO_APPLICATIONS_MAP";
    private static final String SM_CARTRIDGE_GROUP_TO_CARTIDGE_GROUPS_MAP = "SM_CARTRIDGE_GROUP_TO_CARTIDGE_GROUPS_MAP";
    private static final String SM_CARTRIDGE_GROUP_TO_APPLICATIONS_MAP = "SM_CARTRIDGE_GROUP_TO_APPLICATIONS_MAP";

    private static final String SM_CARTRIDGES_CARTRIDGEGROUPS_WRITE_LOCK = "SM_CARTRIDGES_CARTRIDGEGROUPS_WRITE_LOCK";
    private static final String SM_CARTRIDGES_APPLICATIONS_WRITE_LOCK = "SM_CARTRIDGES_APPLICATIONS_WRITE_LOCK";
    private static final String SM_CARTRIDGEGROUPS_CARTRIDGESUBGROUPS_WRITE_LOCK = "SM_CARTRIDGEGROUPS_CARTRIDGESUBGROUPS_WRITE_LOCK";
    private static final String SM_CARTRIDGEGROUPS_APPLICATIONS_WRITE_LOCK = "SM_CARTRIDGEGROUPS_APPLICATIONS_WRITE_LOCK";

    public static final String DATA_RESOURCE = "/stratos.manager/data";

    private final transient DistributedObjectProvider distributedObjectProvider;
    private static final Log log = LogFactory.getLog(StratosManagerContext.class);

    /**
     * Key - cartridge type uuid
     * Value - list of cartridgeGroupNames uuid
     */
    private Map<String,Set<String>> cartridgeTypeToCartridgeGroupsMap;

    /**
     * Key - cartridge type uuid
     * Value - list of ApplicationNames uuid
     */
    private Map<String, Set<String>> cartridgeTypeToApplicationsMap;

    /**
     * Key - cartridge group name uuid
     * Value - list of cartridgeGroupNames uuid
     */
    private Map<String, Set<String>> cartridgeGroupToCartridgeSubGroupsMap;

    /**
     * Key - cartridge group name uuid
     * Value - list of ApplicationNames uuid
     */
    private Map<String, Set<String>> cartridgeGroupToApplicationsMap;

    private boolean clustered;
    private boolean coordinator;

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

    public void setCoordinator(boolean coordinator) {
        this.coordinator = coordinator;
    }

    public boolean isCoordinator() {
        return coordinator;
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

    public void addUsedCartridgesInCartridgeGroups(String cartridgeGroupUuid, String[] cartridgeUuids) {
        if (cartridgeUuids == null) {
            return;
        }

        for (String cartridgeNameUuid : cartridgeUuids) {
            Set<String> cartridgeGroupNames = null;
            if (cartridgeTypeToCartridgeGroupsMap.containsKey(cartridgeNameUuid)) {
                cartridgeGroupNames = cartridgeTypeToCartridgeGroupsMap.get(cartridgeNameUuid);
            } else {
                cartridgeGroupNames = new HashSet<String>();
                cartridgeTypeToCartridgeGroupsMap.put(cartridgeNameUuid, cartridgeGroupNames);
            }
            cartridgeGroupNames.add(cartridgeGroupUuid);
        }
    }

    public void removeUsedCartridgesInCartridgeGroups(String cartridgeGroupNameUuid, String[] cartridgeNamesUuid) {
        if (cartridgeNamesUuid == null) {
            return;
        }

        for (String cartridgeNameUuid : cartridgeNamesUuid) {
            Set<String> cartridgeGroupNames = null;
            if (cartridgeTypeToCartridgeGroupsMap.containsKey(cartridgeNameUuid)) {
                cartridgeGroupNames = cartridgeTypeToCartridgeGroupsMap.get(cartridgeNameUuid);
                // Remove current cartridge group name
                cartridgeGroupNames.remove(cartridgeGroupNameUuid);
                // Remove entry if there are no more cartridge group names for that cartridge type
                if (cartridgeGroupNames.isEmpty()) {
                    cartridgeGroupNames = null;
                    cartridgeTypeToCartridgeGroupsMap.remove(cartridgeNameUuid);
                }
            }
        }
    }

    public boolean isCartridgeIncludedInCartridgeGroups(String cartridgeNameUuid) {
        if (cartridgeTypeToCartridgeGroupsMap.containsKey(cartridgeNameUuid)) {
            if (!cartridgeTypeToCartridgeGroupsMap.get(cartridgeNameUuid).isEmpty()) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void addUsedCartridgesInApplications(String applicationNameUuid, String[] cartridgeNamesUuid) {
        if (cartridgeNamesUuid == null) {
            return;
        }

        for (String cartridgeName : cartridgeNamesUuid) {
            Set<String> applicationNames = null;
            if (cartridgeTypeToApplicationsMap.containsKey(cartridgeName)) {
                applicationNames = cartridgeTypeToApplicationsMap.get(cartridgeName);
            } else {
                applicationNames = new HashSet<String>();
                cartridgeTypeToApplicationsMap.put(cartridgeName, applicationNames);
            }
            applicationNames.add(applicationNameUuid);
        }
    }

    public void removeUsedCartridgesInApplications(String applicationNameUuid, String[] cartridgeNamesUuid) {
        if (cartridgeNamesUuid == null) {
            return;
        }

        for (String cartridgeNameUuid : cartridgeNamesUuid) {
            Set<String> applicationNames = null;
            if (cartridgeTypeToApplicationsMap.containsKey(cartridgeNameUuid)) {
                applicationNames = cartridgeTypeToApplicationsMap.get(cartridgeNameUuid);
                // Remove current application name
                applicationNames.remove(applicationNameUuid);
                // Remove entry if there are no more cartridge group names for that cartridge type
                if (applicationNames.isEmpty()) {
                    applicationNames = null;
                    cartridgeTypeToApplicationsMap.remove(cartridgeNameUuid);
                }
            }
        }
    }

    public boolean isCartridgeIncludedInApplications(String cartridgeNameUuid) {
        if (cartridgeTypeToApplicationsMap.containsKey(cartridgeNameUuid)) {
            if (!cartridgeTypeToApplicationsMap.get(cartridgeNameUuid).isEmpty()) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void addUsedCartridgeGroupsInCartridgeSubGroups(String cartridgeSubGroupNameUuid, String[] cartridgeGroupNamesUuid) {
        if (cartridgeGroupNamesUuid == null) {
            return;
        }

        for (String cartridgeGroupNameUuid : cartridgeGroupNamesUuid) {
            Set<String> cartridgeSubGroupNames = null;
            if (cartridgeGroupToCartridgeSubGroupsMap.containsKey(cartridgeGroupNameUuid)) {
                cartridgeSubGroupNames = cartridgeGroupToCartridgeSubGroupsMap.get(cartridgeGroupNameUuid);
            } else {
                cartridgeSubGroupNames = new HashSet<String>();
                cartridgeGroupToCartridgeSubGroupsMap.put(cartridgeSubGroupNameUuid, cartridgeSubGroupNames);
            }
            cartridgeSubGroupNames.add(cartridgeGroupNameUuid);
        }
    }

    public void removeUsedCartridgeGroupsInCartridgeSubGroups(String cartridgeSubGroupNameUuid, String[] cartridgeGroupNamesUuid) {
        if (cartridgeGroupNamesUuid == null) {
            return;
        }

        for (String cartridgeGroupNameUuid : cartridgeGroupNamesUuid) {
            Set<String> cartridgeSubGroupNames = null;
            if (cartridgeGroupToCartridgeSubGroupsMap.containsKey(cartridgeGroupNameUuid)) {
                cartridgeSubGroupNames = cartridgeGroupToCartridgeSubGroupsMap.get(cartridgeGroupNameUuid);
                // Remove current cartridge group name
                cartridgeSubGroupNames.remove(cartridgeGroupNameUuid);
                // Remove entry if there are no more cartridge group names for that cartridge type
                if (cartridgeSubGroupNames.isEmpty()) {
                    cartridgeSubGroupNames = null;
                    cartridgeGroupToCartridgeSubGroupsMap.remove(cartridgeGroupNameUuid);
                }
            }
        }
    }

    public boolean isCartridgeGroupIncludedInCartridgeSubGroups(String cartridgeGroupNameUuid) {
        if (cartridgeGroupToCartridgeSubGroupsMap.containsKey(cartridgeGroupNameUuid)) {
            if (!cartridgeGroupToCartridgeSubGroupsMap.get(cartridgeGroupNameUuid).isEmpty()) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void addUsedCartridgeGroupsInApplications(String applicationNameUuid, String[] cartridgeGroupNamesUuid) {
        if (cartridgeGroupNamesUuid == null) {
            return;
        }

        for (String cartridgeGroupNameUuid : cartridgeGroupNamesUuid) {
            Set<String> applicationNames = null;
            if (cartridgeGroupToApplicationsMap.containsKey(cartridgeGroupNameUuid)) {
                applicationNames = cartridgeGroupToApplicationsMap.get(cartridgeGroupNameUuid);
            } else {
                applicationNames = new HashSet<String>();
                cartridgeGroupToApplicationsMap.put(cartridgeGroupNameUuid, applicationNames);
            }
            applicationNames.add(applicationNameUuid);
        }
    }

    public void removeUsedCartridgeGroupsInApplications(String applicationNameUuid, String[] cartridgeGroupNamesUuid) {
        if (cartridgeGroupNamesUuid == null) {
            return;
        }

        for (String cartridgeGroupNameUuid : cartridgeGroupNamesUuid) {
            Set<String> applicationNames = null;
            if (cartridgeGroupToApplicationsMap.containsKey(cartridgeGroupNameUuid)) {
                applicationNames = cartridgeGroupToApplicationsMap.get(cartridgeGroupNameUuid);
                // Remove current application name
                applicationNames.remove(applicationNameUuid);
                // Remove entry if there are no more cartridge group names for that cartridge type
                if (applicationNames.isEmpty()) {
                    applicationNames = null;
                    cartridgeGroupToApplicationsMap.remove(cartridgeGroupNameUuid);
                }
            }
        }
    }

    public boolean isCartridgeGroupIncludedInApplications(String cartridgeGroupNameUuid) {
        if (cartridgeGroupToApplicationsMap.containsKey(cartridgeGroupNameUuid)) {
            if (!cartridgeGroupToApplicationsMap.get(cartridgeGroupNameUuid).isEmpty()) {
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
