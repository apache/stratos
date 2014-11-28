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
package org.apache.stratos.autoscaler;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.instance.context.InstanceContext;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is an object that inserted to the rules engine.
 * Holds information about a partition.
 */

public class GroupLevelPartitionContext implements Serializable {

    private static final long serialVersionUID = -2920388667345980487L;
    private static final Log log = LogFactory.getLog(GroupLevelPartitionContext.class);
    private final int PENDING_MEMBER_FAILURE_THRESHOLD = 5;
    private String partitionId;
    private String serviceName;
    private String networkPartitionId;
    private Partition partition;
    private int minimumInstanceCount = 0;
    private int pendingInstancesFailureCount = 0;
    // properties
    private Properties properties;

    // 15 mints as the default
    private long pendingInstanceExpiryTime = 900000;
    // pending instances
    private List<InstanceContext> pendingInstances;

    // 1 day as default
    private long obsoltedInstanceExpiryTime = 1 * 24 * 60 * 60 * 1000;

    // 30 mints as default
    private long terminationPendingInstanceExpiryTime = 1800000;

    // instances to be terminated
    private Map<String, InstanceContext> obsoletedInstances;

    // active instances
    private List<InstanceContext> activeInstances;

    // termination pending instances, instance is added to this when Autoscaler send grace fully shut down event
    private List<InstanceContext> terminationPendingInstances;

    //instance id: time that instance is moved to termination pending status
    private Map<String, Long> terminationPendingStartedTime;

    //Keep statistics come from CEP
    private Map<String, MemberStatsContext> instanceStatsContexts;

    //group instances kept inside a partition
    private Map<String, InstanceContext> instanceIdToInstanceContextMap;

    // for the use of tests
    public GroupLevelPartitionContext(long instanceExpiryTime) {

        this.activeInstances = new ArrayList<InstanceContext>();
        this.terminationPendingInstances = new ArrayList<InstanceContext>();
        pendingInstanceExpiryTime = instanceExpiryTime;
    }

    public GroupLevelPartitionContext(Partition partition) {
        this.setPartition(partition);
        this.minimumInstanceCount = partition.getPartitionMin();
        this.partitionId = partition.getId();
        this.pendingInstances = new ArrayList<InstanceContext>();
        this.activeInstances = new ArrayList<InstanceContext>();
        this.terminationPendingInstances = new ArrayList<InstanceContext>();
        this.obsoletedInstances = new ConcurrentHashMap<String, InstanceContext>();
        instanceStatsContexts = new ConcurrentHashMap<String, MemberStatsContext>();
        instanceIdToInstanceContextMap = new HashMap<String, InstanceContext>();


        terminationPendingStartedTime = new HashMap<String, Long>();
        // check if a different value has been set for expiryTime
        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
        pendingInstanceExpiryTime = conf.getLong(StratosConstants.PENDING_VM_MEMBER_EXPIRY_TIMEOUT, 900000);
        obsoltedInstanceExpiryTime = conf.getLong(StratosConstants.OBSOLETED_VM_MEMBER_EXPIRY_TIMEOUT, 86400000);
        if (log.isDebugEnabled()) {
            log.debug("Instance expiry time is set to: " + pendingInstanceExpiryTime);
            log.debug("Instance obsoleted expiry time is set to: " + obsoltedInstanceExpiryTime);
        }

        /*FIXME Thread th = new Thread(new PendingInstanceWatcher(this));
        th.start();
        Thread th2 = new Thread(new ObsoletedInstanceWatcher(this));
        th2.start();
        Thread th3 = new Thread(new TerminationPendingInstanceWatcher(this));
        th3.start();*/
    }

    public long getTerminationPendingStartedTimeOfInstance(String instanceId) {
        return terminationPendingStartedTime.get(instanceId);
    }

    public Map<String, InstanceContext> getInstanceIdToInstanceContextMap() {
        return instanceIdToInstanceContextMap;
    }

    public void setInstanceIdToInstanceContextMap(Map<String, InstanceContext> instanceIdToInstanceContextMap) {
        this.instanceIdToInstanceContextMap = instanceIdToInstanceContextMap;
    }

    public void addInstanceContext(InstanceContext context) {
        this.instanceIdToInstanceContextMap.put(context.getInstanceId(), context);

    }

    public List<InstanceContext> getPendingInstances() {
        return pendingInstances;
    }

    public void setPendingInstances(List<InstanceContext> pendingInstances) {
        this.pendingInstances = pendingInstances;
    }

    public int getActiveInstanceCount() {
        return activeInstances.size();
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }

    public int getMinimumInstanceCount() {
        return minimumInstanceCount;
    }

    public void setMinimumInstanceCount(int minimumInstanceCount) {
        this.minimumInstanceCount = minimumInstanceCount;
    }

    public Partition getPartition() {
        return partition;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    public void addPendingInstance(InstanceContext ctxt) {
        this.pendingInstances.add(ctxt);
    }

    public boolean removePendingInstance(String id) {
        if (id == null) {
            return false;
        }
        synchronized (pendingInstances) {
            for (Iterator<InstanceContext> iterator = pendingInstances.iterator(); iterator.hasNext(); ) {
                InstanceContext pendingInstance = (InstanceContext) iterator.next();
                if (id.equals(pendingInstance.getInstanceId())) {
                    iterator.remove();
                    return true;
                }

            }
        }

        return false;
    }

    public void movePendingInstanceToActiveInstances(String instanceId) {
        if (instanceId == null) {
            return;
        }
        synchronized (pendingInstances) {
            Iterator<InstanceContext> iterator = pendingInstances.listIterator();
            while (iterator.hasNext()) {
                InstanceContext pendingInstance = iterator.next();
                if (pendingInstance == null) {
                    iterator.remove();
                    continue;
                }
                if (instanceId.equals(pendingInstance.getInstanceId())) {
                    // instance is activated
                    // remove from pending list
                    iterator.remove();
                    // add to the activated list
                    this.activeInstances.add(pendingInstance);
                    pendingInstancesFailureCount = 0;
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Instance is removed and added to the " +
                                "activated Instance list. [Instance Id] %s", instanceId));
                    }
                    break;
                }
            }
        }
    }

    public boolean activeInstanceAvailable(String instanceId) {
        for (InstanceContext activeInstance : activeInstances) {
            if (instanceId.equals(activeInstance.getInstanceId())) {
                return true;
            }
        }
        return false;
    }

    public boolean pendingInstanceAvailable(String instanceId) {

        for (InstanceContext pendingInstance : pendingInstances) {
            if (instanceId.equals(pendingInstance.getInstanceId())) {
                return true;
            }
        }
        return false;
    }

    public void moveActiveInstanceToTerminationPendingInstances(String instanceId) {
        if (instanceId == null) {
            return;
        }
        synchronized (activeInstances) {
            Iterator<InstanceContext> iterator = activeInstances.listIterator();
            while (iterator.hasNext()) {
                InstanceContext activeInstance = iterator.next();
                if (activeInstance == null) {
                    iterator.remove();
                    continue;
                }
                if (instanceId.equals(activeInstance.getInstanceId())) {
                    // instance is activated
                    // remove from pending list
                    iterator.remove();
                    // add to the activated list
                    this.terminationPendingInstances.add(activeInstance);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Active instance is removed and added to the " +
                                "termination pending instance list. [Instance Id] %s", instanceId));
                    }
                    break;
                }
            }
        }
    }

    /**
     * Removes the {@link org.apache.stratos.messaging.domain.instance.context.InstanceContext} object mapping
     * to the specified instance id from the specified InstanceContext collection
     *
     * @param iterator   The {@link java.util.Iterator} for the collection containing
     *                   {@link org.apache.stratos.messaging.domain.instance.context.InstanceContext}
     *                   objects
     * @param instanceId Instance Id {@link String} for the
     *                   {@link org.apache.stratos.messaging.domain.instance.context.InstanceContext}
     *                   to be removed
     * @return {@link org.apache.stratos.messaging.domain.instance.context.InstanceContext} object if
     * object found and removed, null if otherwise.
     */
    private InstanceContext removeInstanceFrom(Iterator<InstanceContext> iterator, String instanceId) {
        while (iterator.hasNext()) {
            InstanceContext activeInstance = iterator.next();
            if (activeInstance == null) {
                iterator.remove();
                continue;
            }
            if (instanceId.equals(activeInstance.getInstanceId())) {
                iterator.remove();
                return activeInstance;
            }
        }

        return null;
    }

    /**
     * Check the instance lists for the provided instance ID and move the instance to the obsolete list
     *
     * @param ctxt The instance ID of the instance to search
     *//*
    public void moveInstanceToObsoleteList(String instanceId) {
        if (instanceId == null) {
            return;
        }

        // check active instance list
        Iterator<InstanceContext> activeInstanceIterator = activeInstances.listIterator();
        InstanceContext removedInstance = this.removeInstanceFrom(activeInstanceIterator, instanceId);
        if (removedInstance != null) {
            this.addObsoleteInstance(removedInstance);
            removedInstance.setObsoleteInitTime(System.currentTimeMillis());
            if (log.isDebugEnabled()) {
                log.debug(String.format("Active instance is removed and added to the " +
                        "obsolete instance list. [Instance Id] %s", instanceId));
            }

            return;
        }

        // check pending instance list
        Iterator<InstanceContext> pendingInstanceIterator = pendingInstances.listIterator();
        removedInstance = this.removeInstanceFrom(pendingInstanceIterator, instanceId);
        if (removedInstance != null) {
            this.addObsoleteInstance(removedInstance);
            removedInstance.setObsoleteInitTime(System.currentTimeMillis());
            if (log.isDebugEnabled()) {
                log.debug(String.format("Pending instance is removed and added to the " +
                        "obsolete instance list. [Instance Id] %s", instanceId));
            }

            return;
        }

        // check termination pending instance list
        Iterator<InstanceContext> terminationPendingInstancesIterator = terminationPendingInstances.listIterator();
        removedInstance = this.removeInstanceFrom(terminationPendingInstancesIterator, instanceId);
        if (removedInstance != null) {
            this.addObsoleteInstance(removedInstance);
            removedInstance.setObsoleteInitTime(System.currentTimeMillis());
            if (log.isDebugEnabled()) {
                log.debug(String.format("Termination Pending instance is removed and added to the " +
                        "obsolete instance list. [Instance Id] %s", instanceId));
            }
        }
    }
*/
    public void addActiveInstance(InstanceContext ctxt) {
        this.activeInstances.add(ctxt);
    }

    public void removeActiveInstance(InstanceContext ctxt) {
        this.activeInstances.remove(ctxt);
    }

    public boolean removeTerminationPendingInstance(String instanceId) {
        boolean terminationPendingInstanceAvailable = false;
        synchronized (terminationPendingInstances) {
            for (InstanceContext instanceContext : terminationPendingInstances) {
                if (instanceContext.getInstanceId().equals(instanceId)) {
                    terminationPendingInstanceAvailable = true;
                    terminationPendingInstances.remove(instanceContext);
                    break;
                }
            }
        }
        return terminationPendingInstanceAvailable;
    }

    public long getObsoltedInstanceExpiryTime() {
        return obsoltedInstanceExpiryTime;
    }

    public void setObsoltedInstanceExpiryTime(long obsoltedInstanceExpiryTime) {
        this.obsoltedInstanceExpiryTime = obsoltedInstanceExpiryTime;
    }

    public void addObsoleteInstance(InstanceContext ctxt) {
        this.obsoletedInstances.put(ctxt.getInstanceId(), ctxt);
    }

    public boolean removeObsoleteInstance(String instanceId) {
        if (this.obsoletedInstances.remove(instanceId) == null) {
            return false;
        }
        return true;
    }

    public long getPendingInstanceExpiryTime() {
        return pendingInstanceExpiryTime;
    }

    public void setPendingInstanceExpiryTime(long pendingInstanceExpiryTime) {
        this.pendingInstanceExpiryTime = pendingInstanceExpiryTime;
    }

    public Map<String, InstanceContext> getObsoletedInstances() {
        return obsoletedInstances;
    }

    public void setObsoletedInstances(Map<String, InstanceContext> obsoletedInstances) {
        this.obsoletedInstances = obsoletedInstances;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public void setNetworkPartitionId(String networkPartitionId) {
        this.networkPartitionId = networkPartitionId;
    }

    public Map<String, MemberStatsContext> getInstanceStatsContexts() {
        return instanceStatsContexts;
    }

    public MemberStatsContext getInstanceStatsContext(String instanceId) {
        return instanceStatsContexts.get(instanceId);
    }

    public void addInstanceStatsContext(MemberStatsContext ctxt) {
        this.instanceStatsContexts.put(ctxt.getInstanceId(), ctxt);
    }

    public void removeInstanceStatsContext(String instanceId) {
        this.instanceStatsContexts.remove(instanceId);
    }

    public MemberStatsContext getPartitionCtxt(String id) {
        return this.instanceStatsContexts.get(id);
    }

    public Properties getProperties() {
        return properties;
    }

//    public boolean instanceExist(String instanceId) {
//        return instanceStatsContexts.containsKey(instanceId);
//    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<InstanceContext> getTerminationPendingInstances() {
        return terminationPendingInstances;
    }

    public void setTerminationPendingInstances(List<InstanceContext> terminationPendingInstances) {
        this.terminationPendingInstances = terminationPendingInstances;
    }

    public int getTotalInstanceCount() {

        return activeInstances.size() + pendingInstances.size() + terminationPendingInstances.size();
    }

    public int getNonTerminatedInstanceCount() {
        return activeInstances.size() + pendingInstances.size();
    }

    public List<InstanceContext> getActiveInstances() {
        return activeInstances;
    }

    public void setActiveInstances(List<InstanceContext> activeInstances) {
        this.activeInstances = activeInstances;
    }

    public boolean removeActiveInstanceById(String instanceId) {
        boolean removeActiveInstance = false;
        synchronized (activeInstances) {
            Iterator<InstanceContext> iterator = activeInstances.listIterator();
            while (iterator.hasNext()) {
                InstanceContext instanceContext = iterator.next();
                if (instanceId.equals(instanceContext.getInstanceId())) {
                    iterator.remove();
                    removeActiveInstance = true;

                    break;
                }
            }
        }
        return removeActiveInstance;
    }

    public boolean activeInstanceExist(String instanceId) {

        for (InstanceContext instanceContext : activeInstances) {
            if (instanceId.equals(instanceContext.getInstanceId())) {
                return true;
            }
        }
        return false;
    }

    public int getAllInstanceForTerminationCount() {
        int count = activeInstances.size() + pendingInstances.size() + terminationPendingInstances.size();
        if (log.isDebugEnabled()) {
            log.debug("PartitionContext:getAllInstanceForTerminationCount:size:" + count);
        }
        return count;
    }

    // Map<String, InstanceStatsContext> getInstanceStatsContexts().keySet()
    public Set<String> getAllInstanceForTermination() {

        List<InstanceContext> merged = new ArrayList<InstanceContext>();


        merged.addAll(activeInstances);
        merged.addAll(pendingInstances);
        merged.addAll(terminationPendingInstances);

        Set<String> results = new HashSet<String>(merged.size());

        for (InstanceContext ctx : merged) {
            results.add(ctx.getInstanceId());
        }


        if (log.isDebugEnabled()) {
            log.debug("PartitionContext:getAllInstanceForTermination:size:" + results.size());
        }

        //InstanceContext x = new InstanceContext();
        //x.getInstanceId()

        return results;
    }

    public void movePendingTerminationInstanceToObsoleteInstances(String instanceId) {

        log.info("Starting the moving of termination pending to obsolete for [instance] " + instanceId);
        if (instanceId == null) {
            return;
        }
        Iterator<InstanceContext> iterator = terminationPendingInstances.listIterator();
        while (iterator.hasNext()) {
            InstanceContext terminationPendingInstance = iterator.next();
            if (terminationPendingInstance == null) {
                iterator.remove();
                continue;
            }
            if (instanceId.equals(terminationPendingInstance.getInstanceId())) {

                log.info("Found termination pending instance and trying to move [instance] " + instanceId + " to obsolete list");
                // instance is pending termination
                // remove from pending termination list
                iterator.remove();
                // add to the obsolete list
                this.obsoletedInstances.put(instanceId, terminationPendingInstance);

                terminationPendingStartedTime.put(instanceId, System.currentTimeMillis());

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Termination pending instance is removed and added to the " +
                            "obsolete instance list. [Instance Id] %s", instanceId));
                }
                break;
            }
        }
    }

    public InstanceContext getPendingTerminationInstance(String instanceId) {
        for (InstanceContext instanceContext : terminationPendingInstances) {
            if (instanceId.equals(instanceContext.getInstanceId())) {
                return instanceContext;
            }
        }
        return null;
    }

    public long getTerminationPendingInstanceExpiryTime() {
        return terminationPendingInstanceExpiryTime;
    }

    public void movePendingInstanceToObsoleteInstances(String instanceId) {
        if (instanceId == null) {
            return;
        }
        Iterator<InstanceContext> iterator = pendingInstances.listIterator();
        while (iterator.hasNext()) {
            InstanceContext pendingInstance = iterator.next();
            if (pendingInstance == null) {
                iterator.remove();
                continue;
            }
            if (instanceId.equals(pendingInstance.getInstanceId())) {

                // remove from pending list
                iterator.remove();
                // add to the obsolete list
                this.obsoletedInstances.put(instanceId, pendingInstance);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Pending instance is removed and added to the " +
                            "obsolete instance list. [Instance Id] %s", instanceId));
                }
                break;
            }
        }

    }

    /*private class PendingInstanceWatcher implements Runnable {
        private ParentComponentLevelPartitionContext ctxt;

        public PendingInstanceWatcher(ParentComponentLevelPartitionContext ctxt) {
            this.ctxt = ctxt;
        }

        @Override
        public void run() {

            while (true) {
                long expiryTime = ctxt.getPendingInstanceExpiryTime();
                List<InstanceContext> pendingInstances = ctxt.getPendingInstances();

                synchronized (pendingInstances) {
                    Iterator<InstanceContext> iterator = pendingInstances.listIterator();
                    while ( iterator.hasNext()) {
                        InstanceContext pendingInstance = iterator.next();

                        if (pendingInstance == null) {
                            continue;
                        }
                        long pendingTime = System.currentTimeMillis() - pendingInstance.getInitTime();
                        if (pendingTime >= expiryTime) {


                            iterator.remove();
                            log.info("Pending state of instance: " + pendingInstance.getInstanceId() +
                                     " is expired. " + "Adding as an obsoleted instance.");
                            // instance should be terminated
                            ctxt.addObsoleteInstance(pendingInstance);
                            pendingInstancesFailureCount++;
                            if( pendingInstancesFailureCount > PENDING_MEMBER_FAILURE_THRESHOLD){
                                setPendingInstanceExpiryTime(expiryTime * 2);//Doubles the expiry time after the threshold of failure exceeded
                                //TODO Implement an alerting system: STRATOS-369
                            }
                        }
                    }
                }

                try {
                    // TODO find a constant
                    Thread.sleep(15000);
                } catch (InterruptedException ignore) {
                }
            }
        }

    }
*/
    /*private class ObsoletedInstanceWatcher implements Runnable {
        private ParentComponentLevelPartitionContext ctxt;

        public ObsoletedInstanceWatcher(ParentComponentLevelPartitionContext ctxt) {
            this.ctxt = ctxt;
        }

        @Override
        public void run() {
            while (true) {
                long obsoltedInstanceExpiryTime = ctxt.getObsoltedInstanceExpiryTime();
                Map<String, InstanceContext> obsoletedInstances = ctxt.getObsoletedInstances();

                Iterator<Entry<String, InstanceContext>> iterator = obsoletedInstances.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<String, InstanceContext> pairs = iterator.next();
                    InstanceContext obsoleteInstance = (InstanceContext) pairs.getValue();
                    if (obsoleteInstance == null) {
                        continue;
                    }
                    long obsoleteTime = System.currentTimeMillis() - obsoleteInstance.getInitTime();
                    if (obsoleteTime >= obsoltedInstanceExpiryTime) {
                        iterator.remove();
                    }
                }
                try {
                    // TODO find a constant
                    Thread.sleep(15000);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }*/

    /**
     * This thread is responsible for moving instance to obsolete list if pending termination timeout happens
     */
    private class TerminationPendingInstanceWatcher implements Runnable {
        private GroupLevelPartitionContext ctxt;

        public TerminationPendingInstanceWatcher(GroupLevelPartitionContext ctxt) {
            this.ctxt = ctxt;
        }

        @Override
        public void run() {

            while (true) {
                long terminationPendingInstanceExpiryTime = ctxt.getTerminationPendingInstanceExpiryTime();

                Iterator<InstanceContext> iterator = ctxt.getTerminationPendingInstances().listIterator();
                while (iterator.hasNext()) {

                    InstanceContext terminationPendingInstance = iterator.next();
                    if (terminationPendingInstance == null) {
                        continue;
                    }
                    long terminationPendingTime = System.currentTimeMillis()
                            - ctxt.getTerminationPendingStartedTimeOfInstance(terminationPendingInstance.getInstanceId());
                    if (terminationPendingTime >= terminationPendingInstanceExpiryTime) {
                        log.info("Moving [instance] " + terminationPendingInstance.getInstanceId() + partitionId);
                        iterator.remove();
                        obsoletedInstances.put(terminationPendingInstance.getInstanceId(), terminationPendingInstance);
                    }
                }
                try {
                    // TODO find a constant
                    Thread.sleep(15000);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }
}
