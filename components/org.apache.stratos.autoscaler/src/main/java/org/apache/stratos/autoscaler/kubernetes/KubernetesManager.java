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
 * Controller class for managing Kubernetes groups.
 */
public class KubernetesManager {
    private static final Log log = LogFactory.getLog(KubernetesManager.class);
    private static KubernetesManager instance;

    // KubernetesGroups against groupId's
    private static Map<String, KubernetesGroup> kubernetesGroupsMap = new HashMap<String, KubernetesGroup>();

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
            throw new InvalidKubernetesGroupException("Kubernetes group group id can not be empty");
        }
        if (kubernetesGroupExist(kubernetesGroup.getGroupId())) {
            throw new InvalidKubernetesGroupException(String.format("Kubernetes group already exists " +
                    "[id] %s", kubernetesGroup.getGroupId()));
        }
        if (kubernetesGroup.getKubernetesMaster() == null) {
            throw new InvalidKubernetesGroupException("Mandatory field Kubernetes master has not be set " +
                    "for the Kubernetes group [id] " + kubernetesGroup.getGroupId());
        }
        if (kubernetesGroup.getPortRange() == null) {
            throw new InvalidKubernetesGroupException("Mandatory field PortRange has not been set " +
                    "for the Kubernetes group [id] " + kubernetesGroup.getGroupId());
        }

        // Port range should contain more than one valid port
        if (kubernetesGroup.getPortRange().getUpper() > AutoScalerConstants.PORT_RANGE_MAX ||
                kubernetesGroup.getPortRange().getUpper() < AutoScalerConstants.PORT_RANGE_MIN ||
                kubernetesGroup.getPortRange().getLower() > AutoScalerConstants.PORT_RANGE_MAX ||
                kubernetesGroup.getPortRange().getLower() < AutoScalerConstants.PORT_RANGE_MIN ||
                kubernetesGroup.getPortRange().getUpper() < kubernetesGroup.getPortRange().getLower()) {
            throw new InvalidKubernetesGroupException("Port range is invalid " +
                    "for the Kubernetes group " + kubernetesGroup.getGroupId());
        }
        try {
            validateKubernetesMaster(kubernetesGroup.getKubernetesMaster());
            validateKubernetesHosts(kubernetesGroup.getKubernetesHosts());

            // check whether master already exists
            if (kubernetesHostExists(kubernetesGroup.getKubernetesMaster().getHostId())) {
                throw new InvalidKubernetesGroupException("Kubernetes master already defined [id] " +
                        kubernetesGroup.getKubernetesMaster().getHostId());
            }

            // Check for duplicate hostIds
            List<String> hostIds = new ArrayList<String>();
            hostIds.add(kubernetesGroup.getKubernetesMaster().getHostId());
            for (KubernetesHost kubernetesHost : kubernetesGroup.getKubernetesHosts()) {
                if (hostIds.contains(kubernetesHost.getHostId())) {
                    throw new InvalidKubernetesGroupException("Kubernetes host already defined [id] " + kubernetesHost.getHostId());
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
            throw new InvalidKubernetesHostException("Kubernetes host host id can not be empty");
        }
        if (kubernetesHost.getHostIpAddress() == null) {
            throw new InvalidKubernetesHostException("Mandatory field Kubernetes host IP address has not been set " +
                    "for the Kubernetes host [hostId] " + kubernetesHost.getHostId());
        }
    }

    private void validateKubernetesMaster(KubernetesMaster kubernetesMaster) throws InvalidKubernetesMasterException {
        if (kubernetesMaster == null) {
            throw new InvalidKubernetesMasterException("Kubernetes master can not be null");
        }
        if (StringUtils.isEmpty(kubernetesMaster.getHostId())) {
            throw new InvalidKubernetesMasterException("Kubernetes master host id can not be empty");
        }
        if (kubernetesMaster.getHostIpAddress() == null) {
            throw new InvalidKubernetesMasterException("Mandatory field Kubernetes master IP address has not been set " +
                    "for the Kubernetes master [hostId] " + kubernetesMaster.getHostId());
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
            RegistryManager.getInstance().persistKubernetesGroup(kubernetesGroup);
            addKubernetesGroupToInformationModel(kubernetesGroup);
            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes group deployed successfully: [id] %s", kubernetesGroup.getGroupId()));
            }
            return true;
        } catch (Exception e) {
            throw new InvalidKubernetesGroupException(e.getMessage(), e);
        }
    }

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
            kubernetesGroupStored.setKubernetesHosts(kubernetesHostArrayList.toArray(kubernetesHostArray));

            // Persist the new KubernetesHost wrapped under KubernetesGroup object
            RegistryManager.getInstance().persistKubernetesGroup(kubernetesGroupStored);

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes host deployed successfully: [id] %s", kubernetesGroupStored.getGroupId()));
            }
            return true;
        } catch (Exception e) {
            throw new InvalidKubernetesHostException(e.getMessage(), e);
        }
    }

    public synchronized boolean updateKubernetesMaster(KubernetesMaster kubernetesMaster)
            throws InvalidKubernetesMasterException, NonExistingKubernetesMasterException {

        validateKubernetesMaster(kubernetesMaster);
        try {
            KubernetesGroup kubernetesGroupStored = getKubernetesGroupContainingHost(kubernetesMaster.getHostId());
            kubernetesGroupStored.setKubernetesMaster(kubernetesMaster);

            // Persist the new KubernetesHost wrapped under KubernetesGroup object
            RegistryManager.getInstance().persistKubernetesGroup(kubernetesGroupStored);
            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes master updated successfully: [id] %s", kubernetesMaster.getHostId()));
            }
        } catch (Exception e) {
            throw new InvalidKubernetesMasterException(e.getMessage(), e);
        }
        return false;
    }

    public synchronized boolean updateKubernetesHost(KubernetesHost kubernetesHost)
            throws InvalidKubernetesHostException, NonExistingKubernetesHostException {
        validateKubernetesHost(kubernetesHost);
        try {
            KubernetesGroup kubernetesGroupStored = getKubernetesGroupContainingHost(kubernetesHost.getHostId());

            for (int i = 0; i < kubernetesGroupStored.getKubernetesHosts().length; i++) {
                if (kubernetesGroupStored.getKubernetesHosts()[i].getHostId().equals(kubernetesHost.getHostId())) {
                    kubernetesGroupStored.getKubernetesHosts()[i] = kubernetesHost;

                    // Persist the new KubernetesHost wrapped under KubernetesGroup object
                    RegistryManager.getInstance().persistKubernetesGroup(kubernetesGroupStored);
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

    public synchronized boolean removeKubernetesGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException {
        try {
            KubernetesGroup kubernetesGroupStored = getKubernetesGroup(kubernetesGroupId);

            // Persist the new KubernetesHost wrapped under KubernetesGroup object
            RegistryManager.getInstance().removeKubernetesGroup(kubernetesGroupStored);

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes group removed successfully: [id] %s", kubernetesGroupId));
            }
        } catch (Exception e) {
            throw new NonExistingKubernetesGroupException(e.getMessage(), e);
        }
        return false;
    }

    public synchronized boolean removeKubernetesHost(String kubernetesHostId) throws NonExistingKubernetesHostException {
        try {
            KubernetesGroup kubernetesGroupStored = getKubernetesGroupContainingHost(kubernetesHostId);

            List<KubernetesHost> kubernetesHostList = new ArrayList<KubernetesHost>();
            for (KubernetesHost kubernetesHost : kubernetesGroupStored.getKubernetesHosts()){
                if (!kubernetesHost.getHostId().equals(kubernetesHostId)){
                    kubernetesHostList.add(kubernetesHost);
                }
            }
            KubernetesHost[] kubernetesHostsArray = new KubernetesHost[kubernetesHostList.size()];
            kubernetesHostList.toArray(kubernetesHostsArray);
            kubernetesGroupStored.setKubernetesHosts(kubernetesHostsArray);

            // Persist the updated KubernetesGroup object
            RegistryManager.getInstance().persistKubernetesGroup(kubernetesGroupStored);

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

    private boolean kubernetesGroupExist(String groupId) {
        return kubernetesGroupsMap.containsKey(groupId);
    }

    private boolean kubernetesHostExists(String hostId) {
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

    private KubernetesHost getKubernetesHost(String hostId) throws NonExistingKubernetesHostException{
        if (StringUtils.isEmpty(hostId)) {
            return null;
        }
        for (KubernetesGroup kubernetesGroup : kubernetesGroupsMap.values()) {
            for (KubernetesHost kubernetesHost : kubernetesGroup.getKubernetesHosts()) {
                if (kubernetesHost.getHostId().equals(hostId)) {
                    return kubernetesHost;
                }
            }
        }
        throw new NonExistingKubernetesHostException("Kubernetes host not found for id: " + hostId);
    }

    public KubernetesHost[] getKubernetesHostsInGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException{
        if (StringUtils.isEmpty(kubernetesGroupId)) {
            throw new NonExistingKubernetesGroupException("Cannot find for empty group id");
        }
        for (KubernetesGroup kubernetesGroup : kubernetesGroupsMap.values()) {
            if (kubernetesGroup.getGroupId().equals(kubernetesGroupId)){
                return kubernetesGroup.getKubernetesHosts();
            }
        }
        throw new NonExistingKubernetesGroupException("Kubernetes group not found for group id: " + kubernetesGroupId);
    }

    public KubernetesMaster getKubernetesMaster(String hostId) throws NonExistingKubernetesMasterException {
        if (StringUtils.isEmpty(hostId)) {
            throw new NonExistingKubernetesMasterException("Cannot find for empty host id");
        }
        for (KubernetesGroup kubernetesGroup : kubernetesGroupsMap.values()) {
            if (kubernetesGroup.getKubernetesMaster().getHostId().equals(hostId)) {
                return kubernetesGroup.getKubernetesMaster();
            }
        }
        throw new NonExistingKubernetesMasterException("Kubernetes master not found for id: " + hostId);
    }

    public KubernetesMaster getKubernetesMasterInGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException {
        if (StringUtils.isEmpty(kubernetesGroupId)) {
            throw new NonExistingKubernetesGroupException("Cannot find for empty group id");
        }
        for (KubernetesGroup kubernetesGroup : kubernetesGroupsMap.values()) {
            if (kubernetesGroup.getGroupId().equals(kubernetesGroupId)) {
                return kubernetesGroup.getKubernetesMaster();
            }
        }
        throw new NonExistingKubernetesGroupException("Kubernetes master not found for group id: " + kubernetesGroupId);
    }

    public KubernetesGroup getKubernetesGroup(String groupId) throws NonExistingKubernetesGroupException {
        if (StringUtils.isEmpty(groupId)) {
            throw new NonExistingKubernetesGroupException("Cannot find for empty group id");
        }
        for (KubernetesGroup kubernetesGroup : kubernetesGroupsMap.values()) {
            if (kubernetesGroup.getGroupId().equals(groupId)) {
                return kubernetesGroup;
            }
        }
        throw new NonExistingKubernetesGroupException("Kubernetes group not found for id: " + groupId);
    }

    private KubernetesGroup getKubernetesGroupContainingHost(String hostId) throws NonExistingKubernetesGroupException{
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

    public KubernetesGroup[] getKubernetesGroups(){
        return kubernetesGroupsMap.values().toArray(new KubernetesGroup[kubernetesGroupsMap.size()]);
    }
}
