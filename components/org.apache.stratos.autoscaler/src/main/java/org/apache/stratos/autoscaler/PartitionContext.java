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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.pojo.MemberContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * This is an object that inserted to the rules engine.
 * Holds information about a partition.
 * @author nirmal
 *
 */

public class PartitionContext implements Serializable{

	private static final long serialVersionUID = -2920388667345980487L;
	private static final Log log = LogFactory.getLog(PartitionContext.class);
    private String partitionId;
    private String serviceName;
    private String networkPartitionId;
    private Partition partition;
    private int currentActiveMemberCount = 0;
    private int minimumMemberCount = 0;
    
    // properties
    private Properties properties;
    
    // 15 mints as the default
    private long expiryTime = 900000;
    // pending members
    private List<MemberContext> pendingMembers;

    // members to be terminated
    private List<String> obsoletedMembers;
    
    // Contains the members that CEP notified as faulty members.
    private List<String> faultyMembers;
    
    // active members
    private List<MemberContext> activeMembers;

    private Map<String, MemberStatsContext> memberStatsContexts;
    
    // for the use of tests
    public PartitionContext() {
    }
    
    public PartitionContext(Partition partition) {
        this.setPartition(partition);
        this.minimumMemberCount = partition.getPartitionMin();
        this.partitionId = partition.getId();
        this.pendingMembers = new ArrayList<MemberContext>();
        this.activeMembers = new ArrayList<MemberContext>();
        this.obsoletedMembers = new CopyOnWriteArrayList<String>(); 
        this.faultyMembers = new CopyOnWriteArrayList<String>();
        memberStatsContexts = new ConcurrentHashMap<String, MemberStatsContext>();
        Thread th = new Thread(new PendingMemberWatcher(this));
        th.start();
    }
    
    public List<MemberContext> getPendingMembers() {
        return pendingMembers;
    }
    
    public void setPendingMembers(List<MemberContext> pendingMembers) {
        this.pendingMembers = pendingMembers;
    }
    
    public List<MemberContext> getActiveMembers() {
        return activeMembers;
    }
    
    public void setActiveMembers(List<MemberContext> activeMembers) {
        this.activeMembers = activeMembers;
    }
    
    public String getPartitionId() {
        return partitionId;
    }
    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }
    public int getTotalMemberCount() {
        // live count + pending count
        return currentActiveMemberCount + pendingMembers.size();
    }

    public void incrementCurrentActiveMemberCount(int count) {

        this.currentActiveMemberCount += count;
    }
    
    public void decrementCurrentActiveMemberCount(int count) {
        this.currentActiveMemberCount -= count;
    }

    public int getMinimumMemberCount() {
        return minimumMemberCount;
    }

    public void setMinimumMemberCount(int minimumMemberCount) {
        this.minimumMemberCount = minimumMemberCount;
    }

    public Partition getPartition() {
        return partition;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }
    
    public void addPendingMember(MemberContext ctxt) {
        this.pendingMembers.add(ctxt);
    }
    
    public void removePendingMember(String memberId) {
        if (memberId == null) {
            return;
        }
        for (Iterator<MemberContext> iterator = pendingMembers.listIterator(); 
                iterator.hasNext();) {
            MemberContext pendingMember = (MemberContext) iterator.next();
            if(pendingMember == null) {
                iterator.remove();
                continue;
            }
            if(memberId.equals(pendingMember.getMemberId())){
                // member is activated
                // remove from pending list
                iterator.remove();
                // add to the activated list
                this.activeMembers.add(pendingMember);
                if (log.isDebugEnabled()) {
                    log.debug("Pending member is removed and added to the " +
                            "activated member list. Id: "+memberId);
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
    
    public void addObsoleteMember(String memberId) {
        this.obsoletedMembers.add(memberId);
    }
    
    public boolean removeObsoleteMember(String memberId) {
        return this.obsoletedMembers.remove(memberId);
    }
    
    public void addFaultyMember(String memberId) {
        this.faultyMembers.add(memberId);
    }
    
    public boolean removeFaultyMember(String memberId) {
        return this.faultyMembers.remove(memberId);
    }
    
    public List<String> getFaultyMembers() {
        return this.faultyMembers;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }
    
    public List<String> getObsoletedMembers() {
        return obsoletedMembers;
    }
        
    public void setObsoletedMembers(List<String> obsoletedMembers) {
        this.obsoletedMembers = obsoletedMembers;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public void setNetworkPartitionId(String networkPartitionId) {
        this.networkPartitionId = networkPartitionId;
    }

    
    public Map<String, MemberStatsContext> getMemberStatsContexts() {
        return memberStatsContexts;
    }

    public MemberStatsContext getMemberStatsContext(String memberId) {
        return memberStatsContexts.get(networkPartitionId);
    }

    public void addMemberStatsContext(MemberStatsContext ctxt) {
        this.memberStatsContexts.put(ctxt.getMemberId(), ctxt);
    }

    public void removeMemberStatsContext(String memberId) {
        this.memberStatsContexts.remove(memberId);
    }

    public MemberStatsContext getPartitionCtxt(String id) {
        return this.memberStatsContexts.get(id);
    }

//    public boolean memberExist(String memberId) {
//        return memberStatsContexts.containsKey(memberId);
//    }


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

    private class PendingMemberWatcher implements Runnable {
        private PartitionContext ctxt;

        public PendingMemberWatcher(PartitionContext ctxt) {
            this.ctxt = ctxt;
        }

        @Override
        public void run() {

            while (true) {
                long expiryTime = ctxt.getExpiryTime();
                List<MemberContext> pendingMembers = ctxt.getPendingMembers();
                
                synchronized (pendingMembers) {

                    for (Iterator<MemberContext> iterator = pendingMembers.listIterator(); iterator.hasNext();) {
                        MemberContext pendingMember = (MemberContext) iterator.next();

                        if (pendingMember == null) {
                            continue;
                        }
                        long pendingTime = System.currentTimeMillis() - pendingMember.getInitTime();
                        if (pendingTime >= expiryTime) {
                            iterator.remove();
                            log.info("Pending state of member: " + pendingMember.getMemberId() +
                                     " is expired. " + "Adding as an obsoleted member.");
                            // member should be terminated
                            ctxt.addObsoleteMember(pendingMember.getMemberId());
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
}
