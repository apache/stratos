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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.pojo.MemberContext;


/**
 * This is an object that inserted to the rules engine.
 * Holds information about a partition.
 * @author nirmal
 *
 */
public class PartitionContext {

    private static final Log log = LogFactory.getLog(PartitionContext.class);
    private String partitionId;
    private Partition partition;
    private int currentMemberCount = 0;
    private int minimumMemberCount = 0;
    // 5 mints as the default
    private long expiryTime = 300000;
    // pending members
    private List<MemberContext> pendingMembers;

    // members to be terminated
    private List<String> obsoletedMembers;
    
    // active members
    private List<MemberContext> activeMembers;
    
    public PartitionContext(Partition partition) {
        this.setPartition(partition);
        this.partitionId = partition.getId();
        this.pendingMembers = new ArrayList<MemberContext>();
        this.activeMembers = new ArrayList<MemberContext>();
        this.obsoletedMembers = new CopyOnWriteArrayList<String>(); 
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
    public int getCurrentMemberCount() {
        // live count + pending count
        return currentMemberCount + pendingMembers.size();
    }
    public void incrementCurrentMemberCount(int count) {
        this.currentMemberCount += count;
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
    
    public void removePendingMember(MemberContext ctxt) {
        this.pendingMembers.remove(ctxt);
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
