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
//    private int currentActiveMemberCount = 0;
    private int minimumMemberCount = 0;
    private int pendingMembersFailureCount = 0;
    private final int PENDING_MEMBER_FAILURE_THRESHOLD = 5;

    // properties
    private Properties properties;
    
    // 15 mints as the default
    private long expiryTime = 900000;
    // pending members
    private List<MemberContext> pendingMembers;

    // members to be terminated
    private List<String> obsoletedMembers;
    
    // Contains the members that CEP notified as faulty members.
//    private List<String> faultyMembers;
    
    // active members
    private List<MemberContext> activeMembers;

    // termination pending members, member is added to this when Autoscaler send grace fully shut down event
    private List<MemberContext> terminationPendingMembers;

    //Keep statistics come from CEP
    private Map<String, MemberStatsContext> memberStatsContexts;
    private int nonTerminatedMemberCount;
//    private int totalMemberCount;

    // for the use of tests
    public PartitionContext() {

        this.activeMembers = new ArrayList<MemberContext>();
        this.terminationPendingMembers = new ArrayList<MemberContext>();
        // check if a different value has been set for expiryTime
        long memberExpiryInterval = getMemberExpiryInterval();
        if (memberExpiryInterval != -1) {
            setExpiryTime(memberExpiryInterval);
            log.info("Member expiry interval set as " + memberExpiryInterval);
        }
    }
    
    public PartitionContext(Partition partition) {
        this.setPartition(partition);
        this.minimumMemberCount = partition.getPartitionMin();
        this.partitionId = partition.getId();
        this.pendingMembers = new ArrayList<MemberContext>();
        this.activeMembers = new ArrayList<MemberContext>();
        this.terminationPendingMembers = new ArrayList<MemberContext>();
        this.obsoletedMembers = new CopyOnWriteArrayList<String>();
//        this.faultyMembers = new CopyOnWriteArrayList<String>();
        memberStatsContexts = new ConcurrentHashMap<String, MemberStatsContext>();

        // check if a different value has been set for expiryTime
        long memberExpiryInterval = getMemberExpiryInterval();
        if (memberExpiryInterval != -1) {
            setExpiryTime(memberExpiryInterval);
            log.info("Member expiry interval set as " + memberExpiryInterval);
        }

        Thread th = new Thread(new PendingMemberWatcher(this));
        th.start();
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
    
    public String getPartitionId() {
        return partitionId;
    }
    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }
//    public int getTotalMemberCount() {
//        // live count + pending count
//        return currentActiveMemberCount + pendingMembers.size();
//    }

//    public void incrementCurrentActiveMemberCount(int count) {
//
//        this.currentActiveMemberCount += count;
//    }
    
//    public void decrementCurrentActiveMemberCount(int count) {
//        this.currentActiveMemberCount -= count;
//    }

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
    
    public void movePendingMemberToActiveMembers(String memberId) {
        if (memberId == null) {
            return;
        }
        Iterator<MemberContext> iterator = pendingMembers.listIterator();
        while (iterator.hasNext()) {
            MemberContext pendingMember = iterator.next();
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
                pendingMembersFailureCount = 0;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Pending member is removed and added to the " +
                            "activated member list. [Member Id] %s",memberId));
                }
                break;
            }
        }
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
    
    public void addActiveMember(MemberContext ctxt) {
        this.activeMembers.add(ctxt);
    }
    
    public void removeActiveMember(MemberContext ctxt) {
        this.activeMembers.remove(ctxt);
    }

    public boolean removeTerminationPendingMember(String memberId) {
        boolean terminationPendingMemberAvailable = false;
        for (MemberContext memberContext: activeMembers){
            if(memberContext.getMemberId().equals(memberId)){
                terminationPendingMemberAvailable =true;
                activeMembers.remove(memberContext);
                break;
            }
        }
        return terminationPendingMemberAvailable;
    }
    
    public void addObsoleteMember(String memberId) {
        this.obsoletedMembers.add(memberId);
    }
    
    public boolean removeObsoleteMember(String memberId) {
        return this.obsoletedMembers.remove(memberId);
    }
//
//    public void addFaultyMember(String memberId) {
//        this.faultyMembers.add(memberId);
//    }
//
//    public boolean removeFaultyMember(String memberId) {
//        return this.faultyMembers.remove(memberId);
//    }
//
//    public List<String> getFaultyMembers() {
//        return this.faultyMembers;
//    }

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
        return memberStatsContexts.get(memberId);
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
        return activeMembers.size() + pendingMembers.size();
    }

    public void removeActiveMemberById(String memberId) {

        synchronized (activeMembers) {
            Iterator<MemberContext> iterator = activeMembers.listIterator();
            while (iterator.hasNext()) {
                MemberContext memberContext = iterator.next();

                if(memberId.equals(memberContext.getMemberId())){

                    iterator.remove();
                    break;
                }
            }
        }
    }

    public boolean activeMemberExist(String memberId) {

        for (MemberContext memberContext: activeMembers) {
            if(memberId.equals(memberContext.getMemberId())){
                return true;
            }
        }
        return false;
    }

    private long getMemberExpiryInterval () {

        // if expiry time has been set in startup scrip with 'member.expiry.interval', use that
        String memberExpiryInterval = System.getProperty(Constants.MEMBER_EXPIRY_INTERVAL);
        long memberExpiryIntervalLongVal = -1;

        if (memberExpiryInterval != null) {

            try {
                memberExpiryIntervalLongVal = Long.parseLong(memberExpiryInterval);

            } catch (NumberFormatException e) {
                log.warn("Invalid value specified for [member.expiry.interval] in the startup script, default value of 15 mins will be used");
                return -1;
            }

            if (memberExpiryIntervalLongVal <= 0) {
                log.warn("Invalid value specified for [member.expiry.interval] in the startup script, default value of 15 mins will be used");
                return -1;
            }
        }

        return memberExpiryIntervalLongVal;
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
                    Iterator<MemberContext> iterator = pendingMembers.listIterator();
                    while ( iterator.hasNext()) {
                        MemberContext pendingMember = iterator.next();

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
                            pendingMembersFailureCount++;
                            if( pendingMembersFailureCount > PENDING_MEMBER_FAILURE_THRESHOLD){
                                setExpiryTime(expiryTime * 2);//Doubles the expiry time after the threshold of failure exceeded
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
}
