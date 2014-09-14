/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler.kubernetes;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.exception.*;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.common.kubernetes.KubernetesGroup;
import org.apache.stratos.common.kubernetes.KubernetesHost;
import org.apache.stratos.common.kubernetes.KubernetesMaster;

import java.util.*;

/**
 * Controller class for managing Kubernetes clusters.
 */
public class KubernetesManager {
    private static final Log log = LogFactory.getLog(KubernetesManager.class);
    private static KubernetesManager instance;

    // KubernetesGroups against groupId's
    private static Map<String, KubernetesGroup> kubernetesGroupsMap = new HashMap<String, KubernetesGroup>();

    // Make the constructor private to create a singleton object
    private KubernetesManager() {
    }

    public static KubernetesManager getInstance() {
        if (instance == null) {
            synchronized (KubernetesManager.class) {
                instance = new KubernetesManager();
            }
        }
        return instance;
    }


    private void validateKubernetesGroup(KubernetesGroup kubernetesGroup) throws InvalidKubernetesGroupException {
        if (kubernetesGroup == null) {
            throw new InvalidKubernetesGroupException("Kubernetes group can not be null");
        }
        if (StringUtils.isEmpty(kubernetesGroup.getGroupId())) {
            throw new InvalidKubernetesGroupException("Kubernetes group groupId can not be empty");
        }
        if (kubernetesGroupExists(kubernetesGroup)) {
            throw new InvalidKubernetesGroupException(String.format("Kubernetes group already exists " +
                    "[id] %s", kubernetesGroup.getGroupId()));
        }
        if (kubernetesGroup.getKubernetesMaster() == null) {
            throw new InvalidKubernetesGroupException("Mandatory field Kubernetes master has not been set " +
                    "for the Kubernetes group [id] " + kubernetesGroup.getGroupId());
        }
        if (kubernetesGroup.getPortRange() == null) {
            throw new InvalidKubernetesGroupException("Mandatory field PortRange has not been set " +
                    "for the Kubernetes group [id] " + kubernetesGroup.getGroupId());
        }

        // Port range validation
        if (kubernetesGroup.getPortRange().getUpper() > AutoScalerConstants.PORT_RANGE_MAX ||
                kubernetesGroup.getPortRange().getUpper() < AutoScalerConstants.PORT_RANGE_MIN ||
                kubernetesGroup.getPortRange().getLower() > AutoScalerConstants.PORT_RANGE_MAX ||
                kubernetesGroup.getPortRange().getLower() < AutoScalerConstants.PORT_RANGE_MIN ||
                kubernetesGroup.getPortRange().getUpper() < kubernetesGroup.getPortRange().getLower()) {
            throw new InvalidKubernetesGroupException("Port range is invalid " +
                    "for the Kubernetes group [id]" + kubernetesGroup.getGroupId());
        }
        try {
            validateKubernetesMaster(kubernetesGroup.getKubernetesMaster());
            validateKubernetesHosts(kubernetesGroup.getKubernetesHosts());

            // check whether master already exists
            if (kubernetesHostExists(kubernetesGroup.getKubernetesMaster().getHostId())) {
                throw new InvalidKubernetesGroupException("Kubernetes host already exists [id] " +
                        kubernetesGroup.getKubernetesMaster().getHostId());
            }

            // Check for duplicate hostIds
            List<String> hostIds = new ArrayList<String>();
            hostIds.add(kubernetesGroup.getKubernetesMaster().getHostId());
            for (KubernetesHost kubernetesHost : kubernetesGroup.getKubernetesHosts()) {
                if (hostIds.contains(kubernetesHost.getHostId())) {
                    throw new InvalidKubernetesGroupException(
                            String.format("Kubernetes host [id] %s already defined in the request", kubernetesHost.getHostId()));
                }

                // check whether host already exists
                if (kubernetesHostExists(kubernetesHost.getHostId())) {
                    throw new InvalidKubernetesGroupException("Kubernetes host already exists [id] " +
                            kubernetesHost.getHostId());
                }

                hostIds.add(kubernetesHost.getHostId());
            }
        } catch (InvalidKubernetesHostException e) {
            throw new InvalidKubernetesGroupException(e.getMessage());
        } catch (InvalidKubernetesMasterException e) {
            throw new InvalidKubernetesGroupException(e.getMessage());
        }
    }

    private void validateKubernetesHosts(KubernetesHost[] kubernetesHosts) throws InvalidKubernetesHostException {
        if (kubernetesHosts == null || kubernetesHosts.length == 0) {
            return;
        }
        for (KubernetesHost kubernetesHost : kubernetesHosts) {
            validateKubernetesHost(kubernetesHost);
        }
    }

    private void validateKubernetesHost(KubernetesHost kubernetesHost) throws InvalidKubernetesHostException {
        if (kubernetesHost == null) {
            throw new InvalidKubernetesHostException("Kubernetes host can not be null");
        }
        if (StringUtils.isEmpty(kubernetesHost.getHostId())) {
            throw new InvalidKubernetesHostException("Kubernetes host id can not be empty");
        }
        if (kubernetesHost.getHostIpAddress() == null) {
            throw new InvalidKubernetesHostException("Mandatory field Kubernetes host IP address has not been set " +
                    "for [hostId] " + kubernetesHost.getHostId());
        }
    }

    private void validateKubernetesMaster(KubernetesMaster kubernetesMaster) throws InvalidKubernetesMasterException {
        try {
            validateKubernetesHost(kubernetesMaster);
        } catch (InvalidKubernetesHostException e) {
            throw new InvalidKubernetesMasterException(e.getMessage());
        }
    }


    /**
     * Register a new KubernetesGroup in AutoScaler.
     */
    public synchronized boolean addNewKubernetesGroup(KubernetesGroup kubernetesGroup)
            throws InvalidKubernetesGroupException {

        validateKubernetesGroup(kubernetesGroup);
        try {
            validateKubernetesEndPointViaCloudController(kubernetesGroup.getKubernetesMaster());

            // Persist the KubernetesGroup object in registry space
            RegistryManager.getInstance().persistKubernetesGroup(kubernetesGroup);

            // Add to information model
            addKubernetesGroupToInformationModel(kubernetesGroup);
            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes group deployed successfully: [id] %s, [description] %s",
                        kubernetesGroup.getGroupId(), kubernetesGroup.getDescription()));
            }
            return true;
        } catch (Exception e) {
            throw new InvalidKubernetesGroupException(e.getMessage(), e);
        }
    }

    /**
     * Register a new KubernetesHost to an existing KubernetesGroup.
     */
    public synchronized boolean addNewKubernetesHost(String kubernetesGroupId, KubernetesHost kubernetesHost)
            throws InvalidKubernetesHostException, NonExistingKubernetesGroupException {

        if (StringUtils.isEmpty(kubernetesGroupId) || kubernetesHost == null) {
            return false;
        }

        validateKubernetesHost(kubernetesHost);
        try {
            KubernetesGroup kubernetesGroupStored = getKubernetesGroup(kubernetesGroupId);

            if (kubernetesHostExists(kubernetesHost.getHostId())) {
                throw new InvalidKubernetesHostException("Kubernetes host already exists: [id] " + kubernetesHost.getHostId());
            }
            ArrayList<KubernetesHost> kubernetesHostArrayList = new
                    ArrayList<KubernetesHost>(Arrays.asList(kubernetesGroupStored.getKubernetesHosts()));
            kubernetesHostArrayList.add(kubernetesHost);
            KubernetesHost[] kubernetesHostArray = new KubernetesHost[kubernetesHostArrayList.size()];

            // TODO - Fix updating logic. Implement registry hierarchical structure
            KubernetesGroup clonedObj = SerializationUtils.clone(kubernetesGroupStored);
            clonedObj.setKubernetesHosts(kubernetesHostArrayList.toArray(kubernetesHostArray));

            // Persist the new KubernetesHost wrapped under KubernetesGroup object
            RegistryManager.getInstance().persistKubernetesGroup(clonedObj);

            // Update information model
            kubernetesGroupStored.setKubernetesHosts(kubernetesHostArrayList.toArray(kubernetesHostArray));

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes host deployed successfully: [id] %s", kubernetesGroupStored.getGroupId()));
            }
            return true;
        } catch (Exception e) {
            throw new InvalidKubernetesHostException(e.getMessage(), e);
        }
    }

    /**
     * Update an existing Kubernetes master
     */
    public synchronized boolean updateKubernetesMaster(KubernetesMaster kubernetesMaster)
            throws InvalidKubernetesMasterException, NonExistingKubernetesMasterException {

        validateKubernetesMaster(kubernetesMaster);
        try {
            KubernetesGroup kubernetesGroupStored = getKubernetesGroupContainingHost(kubernetesMaster.getHostId());

            // TODO - Fix updating logic. Implement registry hierarchical structure
            KubernetesGroup clonedObj = SerializationUtils.clone(kubernetesGroupStored);
            clonedObj.setKubernetesMaster(kubernetesMaster);

            // Persist the new KubernetesHost wrapped under KubernetesGroup object
            RegistryManager.getInstance().persistKubernetesGroup(clonedObj);

            // Update information model
            kubernetesGroupStored.setKubernetesMaster(kubernetesMaster);
            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes master updated successfully: [id] %s", kubernetesMaster.getHostId()));
            }
        } catch (Exception e) {
            throw new InvalidKubernetesMasterException(e.getMessage(), e);
        }
        return false;
    }

    /**
     * Update an existing Kubernetes host
     */
    public synchronized boolean updateKubernetesHost(KubernetesHost kubernetesHost)
            throws InvalidKubernetesHostException, NonExistingKubernetesHostException {
        validateKubernetesHost(kubernetesHost);
        try {
            KubernetesGroup kubernetesGroupStored = getKubernetesGroupContainingHost(kubernetesHost.getHostId());

            for (int i = 0; i < kubernetesGroupStored.getKubernetesHosts().length; i++) {
                if (kubernetesGroupStored.getKubernetesHosts()[i].getHostId().equals(kubernetesHost.getHostId())) {

                    // TODO - Fix updating logic. Implement registry hierarchical structure
                    KubernetesGroup clonedObj = SerializationUtils.clone(kubernetesGroupStored);
                    clonedObj.getKubernetesHosts()[i] = kubernetesHost;

                    // Persist the new KubernetesHost wrapped under KubernetesGroup object
                    RegistryManager.getInstance().persistKubernetesGroup(clonedObj);

                    // Update the information model
                    kubernetesGroupStored.getKubernetesHosts()[i] = kubernetesHost;

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Kubernetes host updated successfully: [id] %s", kubernetesHost.getHostId()));
                    }

                    return true;
                }
            }
        } catch (Exception e) {
            throw new InvalidKubernetesHostException(e.getMessage(), e);
        }
        return false;
    }

    /**
     * Remove a registered Kubernetes group from registry
     */
    public synchronized boolean removeKubernetesGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException {
        try {
            KubernetesGroup kubernetesGroupStored = getKubernetesGroup(kubernetesGroupId);

            // Persist the new KubernetesHost wrapped under KubernetesGroup object
            RegistryManager.getInstance().removeKubernetesGroup(kubernetesGroupStored);

            // Remove entry from information model
            kubernetesGroupsMap.remove(kubernetesGroupId);

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes group removed successfully: [id] %s", kubernetesGroupId));
            }
        } catch (Exception e) {
            throw new NonExistingKubernetesGroupException(e.getMessage(), e);
        }
        return false;
    }

    /**
     * Remove a registered Kubernetes host from registry
     */
    public synchronized boolean removeKubernetesHost(String kubernetesHostId) throws NonExistingKubernetesHostException {
        try {
            KubernetesGroup kubernetesGroupStored = getKubernetesGroupContainingHost(kubernetesHostId);

            List<KubernetesHost> kubernetesHostList = new ArrayList<KubernetesHost>();
            for (KubernetesHost kubernetesHost : kubernetesGroupStored.getKubernetesHosts()) {
                if (!kubernetesHost.getHostId().equals(kubernetesHostId)) {
                    kubernetesHostList.add(kubernetesHost);
                }
            }
            KubernetesHost[] kubernetesHostsArray = new KubernetesHost[kubernetesHostList.size()];
            kubernetesHostList.toArray(kubernetesHostsArray);

            // TODO - Fix updating logic. Implement registry hierarchical structure
            KubernetesGroup clonedObj = SerializationUtils.clone(kubernetesGroupStored);
            clonedObj.setKubernetesHosts(kubernetesHostsArray);

            // Persist the updated KubernetesGroup object
            RegistryManager.getInstance().persistKubernetesGroup(clonedObj);

            // Update information model
            kubernetesGroupStored.setKubernetesHosts(kubernetesHostsArray);

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes host removed successfully: [id] %s", kubernetesHostId));
            }
        } catch (Exception e) {
            throw new NonExistingKubernetesHostException(e.getMessage(), e);
        }
        return false;
    }

    private void addKubernetesGroupToInformationModel(KubernetesGroup kubernetesGroup) {
        kubernetesGroupsMap.put(kubernetesGroup.getGroupId(), kubernetesGroup);
    }

    private void validateKubernetesEndPointViaCloudController(KubernetesMaster kubernetesMaster)
            throws KubernetesEndpointValidationException {
        // TODO
    }

    public boolean kubernetesGroupExists(KubernetesGroup kubernetesGroup) {
        return kubernetesGroupsMap.containsKey(kubernetesGroup.getGroupId());
    }

    public boolean kubernetesHostExists(String hostId) {
        if (StringUtils.isEmpty(hostId)) {
            return false;
        }
        for (KubernetesGroup kubernetesGroup : kubernetesGroupsMap.values()) {
            for (KubernetesHost kubernetesHost : kubernetesGroup.getKubernetesHosts()) {
                if (kubernetesHost.getHostId().equals(hostId)) {
                    return true;
                }
            }
            if (kubernetesGroup.getKubernetesMaster().getHostId().equals(hostId)) {
                return true;
            }
        }
        return false;
    }

    public KubernetesHost[] getKubernetesHostsInGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException {
        if (StringUtils.isEmpty(kubernetesGroupId)) {
            throw new NonExistingKubernetesGroupException("Cannot find for empty group id");
        }

        KubernetesGroup kubernetesGroup = kubernetesGroupsMap.get(kubernetesGroupId);
        if (kubernetesGroup != null) {
            return kubernetesGroup.getKubernetesHosts();
        }
        throw new NonExistingKubernetesGroupException("Kubernetes group not found for group id: " + kubernetesGroupId);
    }

    public KubernetesMaster getKubernetesMasterInGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException {
        if (StringUtils.isEmpty(kubernetesGroupId)) {
            throw new NonExistingKubernetesGroupException("Cannot find for empty group id");
        }
        KubernetesGroup kubernetesGroup = kubernetesGroupsMap.get(kubernetesGroupId);
        if (kubernetesGroup != null) {
            return kubernetesGroup.getKubernetesMaster();
        }
        throw new NonExistingKubernetesGroupException("Kubernetes master not found for group id: " + kubernetesGroupId);
    }

    public KubernetesGroup getKubernetesGroup(String groupId) throws NonExistingKubernetesGroupException {
        if (StringUtils.isEmpty(groupId)) {
            throw new NonExistingKubernetesGroupException("Cannot find for empty group id");
        }
        KubernetesGroup kubernetesGroup = kubernetesGroupsMap.get(groupId);
        if (kubernetesGroup != null) {
            return kubernetesGroup;
        }
        throw new NonExistingKubernetesGroupException("Kubernetes group not found for id: " + groupId);
    }

    public KubernetesGroup getKubernetesGroupContainingHost(String hostId) throws NonExistingKubernetesGroupException {
        if (StringUtils.isEmpty(hostId)) {
            return null;
        }
        for (KubernetesGroup kubernetesGroup : kubernetesGroupsMap.values()) {
            if (kubernetesGroup.getKubernetesMaster().getHostId().equals(hostId)) {
                return kubernetesGroup;
            }
            for (KubernetesHost kubernetesHost : kubernetesGroup.getKubernetesHosts()) {
                if (kubernetesHost.getHostId().equals(hostId)) {
                    return kubernetesGroup;
                }
            }
        }
        throw new NonExistingKubernetesGroupException("Kubernetes group not found containing host id: " + hostId);
    }

    public KubernetesGroup[] getKubernetesGroups() {
        return kubernetesGroupsMap.values().toArray(new KubernetesGroup[kubernetesGroupsMap.size()]);
    }
}
