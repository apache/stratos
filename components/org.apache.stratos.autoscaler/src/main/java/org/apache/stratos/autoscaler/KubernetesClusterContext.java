/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.policy.model.LoadAverage;
import org.apache.stratos.autoscaler.policy.model.MemoryConsumption;
import org.apache.stratos.autoscaler.policy.model.RequestsInFlight;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.common.constants.StratosConstants;

/*
 * It holds the runtime data of a kubernetes service cluster
 */
public class KubernetesClusterContext extends AbstractClusterContext {

    private static final long serialVersionUID = 808741789615481596L;
    private static final Log log = LogFactory.getLog(KubernetesClusterContext.class);

    private String kubernetesClusterId;
    private String serviceName;

    private int minReplicas;
    private int maxReplicas;
    private int currentReplicas;
    private float RequiredReplicas;

    private AutoscalePolicy autoscalePolicy;

    // it will tell whether the startContainers() method succeed or not for the 1st time
    // we should call startContainers() only once
    private boolean isServiceClusterCreated = false;

    // properties
    private Properties properties;

    // 15 mints as the default
    private long pendingMemberExpiryTime;
    // pending members
    private List<MemberContext> pendingMembers;

    // active members
    private List<MemberContext> activeMembers;

    // 1 day as default
    private long obsoltedMemberExpiryTime = 1*24*60*60*1000;

    // members to be terminated
    private Map<String, MemberContext> obsoletedMembers;

    // termination pending members, member is added to this when Autoscaler send grace fully shut down event
    private List<MemberContext> terminationPendingMembers;

    //Keep statistics come from CEP
    private Map<String, MemberStatsContext> memberStatsContexts;

    //Following information will keep events details
    private RequestsInFlight requestsInFlight;
    private MemoryConsumption memoryConsumption;
    private LoadAverage loadAverage;

    //boolean values to keep whether the requests in flight parameters are reset or not
    private boolean rifReset = false, averageRifReset = false,
            gradientRifReset = false, secondDerivativeRifRest = false;
    //boolean values to keep whether the memory consumption parameters are reset or not
    private boolean memoryConsumptionReset = false, averageMemoryConsumptionReset = false,
            gradientMemoryConsumptionReset = false, secondDerivativeMemoryConsumptionRest = false;
    //boolean values to keep whether the load average parameters are reset or not
    private boolean loadAverageReset = false, averageLoadAverageReset = false,
            gradientLoadAverageReset = false, secondDerivativeLoadAverageRest = false;

    public KubernetesClusterContext(String kubernetesClusterId, String clusterId, String serviceId, AutoscalePolicy autoscalePolicy,
                                    int minCount, int maxCount) {

        super(clusterId, serviceId);
        this.kubernetesClusterId = kubernetesClusterId;
        this.minReplicas = minCount;
        this.maxReplicas = maxCount;
        this.pendingMembers = new ArrayList<MemberContext>();
        this.activeMembers = new ArrayList<MemberContext>();
        this.terminationPendingMembers = new ArrayList<MemberContext>();
        this.obsoletedMembers = new ConcurrentHashMap<String, MemberContext>();
        this.memberStatsContexts = new ConcurrentHashMap<String, MemberStatsContext>();
        this.requestsInFlight = new RequestsInFlight();
        this.loadAverage = new LoadAverage();
        this.memoryConsumption = new MemoryConsumption();
        this.autoscalePolicy = autoscalePolicy;

        // check if a different value has been set for expiryTime
        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
        pendingMemberExpiryTime = conf.getLong(StratosConstants.PENDING_CONTAINER_MEMBER_EXPIRY_TIMEOUT, 300000);
        obsoltedMemberExpiryTime = conf.getLong(StratosConstants.OBSOLETED_CONTAINER_MEMBER_EXPIRY_TIMEOUT, 3600000);
        if (log.isDebugEnabled()) {
        	log.debug("Member expiry time is set to: " + pendingMemberExpiryTime);
        	log.debug("Member obsoleted expiry time is set to: " + obsoltedMemberExpiryTime);
        }

        Thread th = new Thread(new PendingMemberWatcher(this));
        th.start();
        Thread th2 = new Thread(new ObsoletedMemberWatcher(this));
        th2.start();
    }

    public String getKubernetesClusterID() {
        return kubernetesClusterId;
    }

    public void setKubernetesClusterID(String kubernetesClusterId) {
        this.kubernetesClusterId = kubernetesClusterId;
    }

    public List<MemberContext> getPendingMembers() {
        return pendingMembers;
    }

    public void setPendingMembers(List<MemberContext> pendingMembers) {
        this.pendingMembers = pendingMembers;
    }

    public int getActiveMemberCount() {
        return activeMembers.size();
    }

    public void setActiveMembers(List<MemberContext> activeMembers) {
        this.activeMembers = activeMembers;
    }

    public int getMinReplicas() {
        return minReplicas;
    }

    public void setMinReplicas(int minReplicas) {
        this.minReplicas = minReplicas;
    }

    public int getMaxReplicas() {
        return maxReplicas;
    }

    public void setMaxReplicas(int maxReplicas) {
        this.maxReplicas = maxReplicas;
    }

    public int getCurrentReplicas() {
        return currentReplicas;
    }

    public void setCurrentReplicas(int currentReplicas) {
        this.currentReplicas = currentReplicas;
    }

    public void addPendingMember(MemberContext ctxt) {
        this.pendingMembers.add(ctxt);
    }

    public boolean removePendingMember(String id) {
        if (id == null) {
            return false;
        }
        for (Iterator<MemberContext> iterator = pendingMembers.iterator(); iterator.hasNext(); ) {
            MemberContext pendingMember = (MemberContext) iterator.next();
            if (id.equals(pendingMember.getMemberId())) {
                iterator.remove();
                return true;
            }

        }

        return false;
    }

    public void movePendingMemberToActiveMembers(String memberId) {
        if (memberId == null) {
            return;
        }
        Iterator<MemberContext> iterator = pendingMembers.listIterator();
        while (iterator.hasNext()) {
            MemberContext pendingMember = iterator.next();
            if (pendingMember == null) {
                iterator.remove();
                continue;
            }
            if (memberId.equals(pendingMember.getMemberId())) {
                // member is activated
                // remove from pending list
                iterator.remove();
                // add to the activated list
                this.activeMembers.add(pendingMember);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Pending member is removed and added to the "
                            + "activated member list. [Member Id] %s",
                            memberId));
                }
                break;
            }
        }
    }

    public void addActiveMember(MemberContext ctxt) {
        this.activeMembers.add(ctxt);
    }

    public void removeActiveMember(MemberContext ctxt) {
        this.activeMembers.remove(ctxt);
    }

    public long getPendingMemberExpiryTime() {
        return pendingMemberExpiryTime;
    }

    public void setPendingMemberExpiryTime(long pendingMemberExpiryTime) {
        this.pendingMemberExpiryTime = pendingMemberExpiryTime;
    }

    public Map<String, MemberStatsContext> getMemberStatsContexts() {
        return memberStatsContexts;
    }

    public MemberStatsContext getMemberStatsContext(String memberId) {
        return memberStatsContexts.get(memberId);
    }

    public void addMemberStatsContext(MemberStatsContext ctxt) {
        this.memberStatsContexts.put(ctxt.getMemberId(), ctxt);
    }

    public void removeMemberStatsContext(String memberId) {
        this.memberStatsContexts.remove(memberId);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<MemberContext> getActiveMembers() {
        return activeMembers;
    }

    public boolean removeActiveMemberById(String memberId) {
        boolean removeActiveMember = false;
        synchronized (activeMembers) {
            Iterator<MemberContext> iterator = activeMembers.listIterator();
            while (iterator.hasNext()) {
                MemberContext memberContext = iterator.next();
                if (memberId.equals(memberContext.getMemberId())) {
                    iterator.remove();
                    removeActiveMember = true;

                    break;
                }
            }
        }
        return removeActiveMember;
    }

    public boolean activeMemberExist(String memberId) {

        for (MemberContext memberContext : activeMembers) {
            if (memberId.equals(memberContext.getMemberId())) {
                return true;
            }
        }
        return false;
    }

    public AutoscalePolicy getAutoscalePolicy() {
        return autoscalePolicy;
    }

    public float getRequiredReplicas() {
        return RequiredReplicas;
    }

    public void setRequiredReplicas(float requiredReplicas) {
        RequiredReplicas = requiredReplicas;
    }

    private class PendingMemberWatcher implements Runnable {
        private KubernetesClusterContext ctxt;

        public PendingMemberWatcher(KubernetesClusterContext ctxt) {
            this.ctxt = ctxt;
        }

        @Override
        public void run() {

            while (true) {
                long expiryTime = ctxt.getPendingMemberExpiryTime();
                List<MemberContext> pendingMembers = ctxt.getPendingMembers();

                synchronized (pendingMembers) {
                    Iterator<MemberContext> iterator = pendingMembers
                            .listIterator();
                    while (iterator.hasNext()) {
                        MemberContext pendingMember = iterator.next();

                        if (pendingMember == null) {
                            continue;
                        }
                        long pendingTime = System.currentTimeMillis()
                                           - pendingMember.getInitTime();
                        if (pendingTime >= expiryTime) {
                        	iterator.remove();
                        	log.info("Pending state of member: " + pendingMember.getMemberId() +
                                    " is expired. " + "Adding as an obsoleted member.");
                        	ctxt.addObsoleteMember(pendingMember);
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
    
    private class ObsoletedMemberWatcher implements Runnable {
        private KubernetesClusterContext ctxt;

        public ObsoletedMemberWatcher(KubernetesClusterContext ctxt) {
            this.ctxt = ctxt;
        }

        @Override
        public void run() {
            while (true) {

                long obsoltedMemberExpiryTime = ctxt.getObsoltedMemberExpiryTime();
                Map<String, MemberContext> obsoletedMembers = ctxt.getObsoletedMembers();
                Iterator<Entry<String, MemberContext>> iterator = obsoletedMembers.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<String, MemberContext> pairs = iterator.next();
                    MemberContext obsoleteMember = (MemberContext) pairs.getValue();
                    if (obsoleteMember == null) {
                        continue;
                    }
                    long obsoleteTime = System.currentTimeMillis() - obsoleteMember.getInitTime();
                    if (obsoleteTime >= obsoltedMemberExpiryTime) {
                        iterator.remove();
                        log.info("Obsolete state of member: " + obsoleteMember.getMemberId() +
                                " is expired. " + "Removing from obsolete member list");
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

    public float getAverageRequestsInFlight() {
        return requestsInFlight.getAverage();
    }

    public void setAverageRequestsInFlight(float averageRequestsInFlight) {
        requestsInFlight.setAverage(averageRequestsInFlight);
        averageRifReset = true;
        if (secondDerivativeRifRest && gradientRifReset) {
            rifReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Requests in flights stats are reset, "
                                        + "ready to do scale check [kub cluster] %s", this.kubernetesClusterId));
            }
        }
    }

    public float getRequestsInFlightSecondDerivative() {
        return requestsInFlight.getSecondDerivative();
    }

    public void setRequestsInFlightSecondDerivative(
            float requestsInFlightSecondDerivative) {
        requestsInFlight.setSecondDerivative(requestsInFlightSecondDerivative);
        secondDerivativeRifRest = true;
        if (averageRifReset && gradientRifReset) {
            rifReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check "
                                        + "[kub cluster] %s", this.kubernetesClusterId));
            }
        }
    }

    public float getRequestsInFlightGradient() {
        return requestsInFlight.getGradient();
    }

    public void setRequestsInFlightGradient(float requestsInFlightGradient) {
        requestsInFlight.setGradient(requestsInFlightGradient);
        gradientRifReset = true;
        if (secondDerivativeRifRest && averageRifReset) {
            rifReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check "
                                        + "[kub cluster] %s", this.kubernetesClusterId));
            }
        }
    }

    public boolean isRifReset() {
        return rifReset;
    }

    public void setRifReset(boolean rifReset) {
        this.rifReset = rifReset;
        this.averageRifReset = rifReset;
        this.gradientRifReset = rifReset;
        this.secondDerivativeRifRest = rifReset;
    }

    public float getAverageMemoryConsumption() {
        return memoryConsumption.getAverage();
    }

    public void setAverageMemoryConsumption(float averageMemoryConsumption) {
        memoryConsumption.setAverage(averageMemoryConsumption);
        averageMemoryConsumptionReset = true;
        if (secondDerivativeMemoryConsumptionRest
            && gradientMemoryConsumptionReset) {
            memoryConsumptionReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Memory consumption stats are reset, ready to do scale check "
                                        + "[kub cluster] %s", this.kubernetesClusterId));
            }
        }
    }

    public float getMemoryConsumptionSecondDerivative() {
        return memoryConsumption.getSecondDerivative();
    }

    public void setMemoryConsumptionSecondDerivative(
            float memoryConsumptionSecondDerivative) {
        memoryConsumption
                .setSecondDerivative(memoryConsumptionSecondDerivative);
        secondDerivativeMemoryConsumptionRest = true;
        if (averageMemoryConsumptionReset && gradientMemoryConsumptionReset) {
            memoryConsumptionReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Memory consumption stats are reset, ready to do scale check "
                                        + "[kub cluster] %s", this.kubernetesClusterId));
            }
        }
    }

    public float getMemoryConsumptionGradient() {
        return memoryConsumption.getGradient();
    }

    public void setMemoryConsumptionGradient(float memoryConsumptionGradient) {
        memoryConsumption.setGradient(memoryConsumptionGradient);
        gradientMemoryConsumptionReset = true;
        if (secondDerivativeMemoryConsumptionRest
            && averageMemoryConsumptionReset) {
            memoryConsumptionReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Memory consumption stats are reset, ready to do scale check "
                                        + "[kub cluster] %s", this.kubernetesClusterId));
            }
        }
    }

    public boolean isMemoryConsumptionReset() {
        return memoryConsumptionReset;
    }

    public void setMemoryConsumptionReset(boolean memoryConsumptionReset) {
        this.memoryConsumptionReset = memoryConsumptionReset;
        this.averageMemoryConsumptionReset = memoryConsumptionReset;
        this.gradientMemoryConsumptionReset = memoryConsumptionReset;
        this.secondDerivativeMemoryConsumptionRest = memoryConsumptionReset;
    }


    public float getAverageLoadAverage() {
        return loadAverage.getAverage();
    }

    public void setAverageLoadAverage(float averageLoadAverage) {
        loadAverage.setAverage(averageLoadAverage);
        averageLoadAverageReset = true;
        if (secondDerivativeLoadAverageRest && gradientLoadAverageReset) {
            loadAverageReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Load average stats are reset, ready to do scale check "
                                        + "[kub cluster] %s", this.kubernetesClusterId));
            }
        }
    }

    public float getLoadAverageSecondDerivative() {
        return loadAverage.getSecondDerivative();
    }

    public void setLoadAverageSecondDerivative(float loadAverageSecondDerivative) {
        loadAverage.setSecondDerivative(loadAverageSecondDerivative);
        secondDerivativeLoadAverageRest = true;
        if (averageLoadAverageReset && gradientLoadAverageReset) {
            loadAverageReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Load average stats are reset, ready to do scale check "
                                        + "[kub cluster] %s", this.kubernetesClusterId));
            }
        }
    }

    public float getLoadAverageGradient() {
        return loadAverage.getGradient();
    }

    public void setLoadAverageGradient(float loadAverageGradient) {
        loadAverage.setGradient(loadAverageGradient);
        gradientLoadAverageReset = true;
        if (secondDerivativeLoadAverageRest && averageLoadAverageReset) {
            loadAverageReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Load average stats are reset, ready to do scale check "
                                        + "[kub cluster] %s", this.kubernetesClusterId));
            }
        }
    }

    public boolean isLoadAverageReset() {
        return loadAverageReset;
    }

    public void setLoadAverageReset(boolean loadAverageReset) {
        this.loadAverageReset = loadAverageReset;
        this.averageLoadAverageReset = loadAverageReset;
        this.gradientLoadAverageReset = loadAverageReset;
        this.secondDerivativeLoadAverageRest = loadAverageReset;
    }
    
    public void moveActiveMemberToTerminationPendingMembers(String memberId) {
        if (memberId == null) {
            return;
        }
        Iterator<MemberContext> iterator = activeMembers.listIterator();
        while ( iterator.hasNext()) {
            MemberContext activeMember = iterator.next();
            if(activeMember == null) {
                iterator.remove();
                continue;
            }
            if(memberId.equals(activeMember.getMemberId())){
                // member is activated
                // remove from pending list
                iterator.remove();
                // add to the activated list
                this.terminationPendingMembers.add(activeMember);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Active member is removed and added to the " +
                            "termination pending member list. [Member Id] %s", memberId));
                }
                break;
            }
        }
    }
    
    public boolean removeTerminationPendingMember(String memberId) {
        boolean terminationPendingMemberAvailable = false;
        for (MemberContext memberContext: terminationPendingMembers){
            if(memberContext.getMemberId().equals(memberId)){
                terminationPendingMemberAvailable = true;
                terminationPendingMembers.remove(memberContext);
                break;
            }
        }
        return terminationPendingMemberAvailable;
    }
    
    public long getObsoltedMemberExpiryTime() {
        return obsoltedMemberExpiryTime;
    }
    
    public void setObsoltedMemberExpiryTime(long obsoltedMemberExpiryTime) {
        this.obsoltedMemberExpiryTime = obsoltedMemberExpiryTime;
    }
    
    public void addObsoleteMember(MemberContext ctxt) {
        this.obsoletedMembers.put(ctxt.getMemberId(), ctxt);
    }
    
    public boolean removeObsoleteMember(String memberId) {
        if(this.obsoletedMembers.remove(memberId) == null) {
            return false;
        }
        return true;
    }
    
    public Map<String, MemberContext> getObsoletedMembers() {
        return obsoletedMembers;
    }
        
    public void setObsoletedMembers(Map<String, MemberContext> obsoletedMembers) {
        this.obsoletedMembers = obsoletedMembers;
    }

    public MemberStatsContext getPartitionCtxt(String id) {
        return this.memberStatsContexts.get(id);
    }

    public List<MemberContext> getTerminationPendingMembers() {
        return terminationPendingMembers;
    }

    public void setTerminationPendingMembers(List<MemberContext> terminationPendingMembers) {
        this.terminationPendingMembers = terminationPendingMembers;
    }

    public int getTotalMemberCount() {
        return activeMembers.size() + pendingMembers.size() + terminationPendingMembers.size();
    }

    public int getNonTerminatedMemberCount() {
        return activeMembers.size() + pendingMembers.size() + terminationPendingMembers.size();
    }

	public String getClusterId() {
		return clusterId;
	}

	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}

	public boolean isServiceClusterCreated() {
		return isServiceClusterCreated;
	}

	public void setServiceClusterCreated(boolean isServiceClusterCreated) {
		this.isServiceClusterCreated = isServiceClusterCreated;
	}
}
