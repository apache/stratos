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

package org.apache.stratos.load.balancer.algorithm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Member;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is the implementation of the round robin load balancing algorithm. It simply iterates
 * through the endpoint list one by one for until an active endpoint is found.
 */
public class RoundRobin implements LoadBalanceAlgorithm {
    private static final Log log = LogFactory.getLog(RoundRobin.class);

    private List<Member> members;
    private final Lock lock = new ReentrantLock();

    @Override
    public String getName() {
        return "Round Robin";
    }

    @Override
    public Member getNextMember(AlgorithmContext algorithmContext) {
        if (members.size() == 0) {
            return null;
        }
        Member current = null;
        lock.lock();
        try {
            int currentMemberIndex = algorithmContext.getCurrentMemberIndex();
            if (currentMemberIndex >= members.size()) {
                currentMemberIndex = 0;
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Searching for next member: [service] %s [cluster]: %s [member-count]: %d [current-index] %d", algorithmContext.getServiceName(), algorithmContext.getClusterId(), members.size(), currentMemberIndex));
            }
            int index = members.size();
            do {
                current = members.get(currentMemberIndex);
                if (currentMemberIndex == members.size() - 1) {
                    currentMemberIndex = 0;
                } else {
                    currentMemberIndex++;
                }
                index--;
            } while ((!current.isActive()) && index > 0);
            algorithmContext.setCurrentMemberIndex(currentMemberIndex);
        } finally {
            lock.unlock();
        }
        return current;
    }

    @Override
    public void setMembers(List<Member> members) {
        this.members = members;
    }

    @Override
    public void reset(AlgorithmContext algorithmContext) {
        synchronized (algorithmContext) {
            algorithmContext.setCurrentMemberIndex(0);
            if (log.isDebugEnabled()) {
                log.debug("Round robin load balance algorithm was reset");
            }
        }
    }

    public LoadBalanceAlgorithm clone() {
        return new RoundRobin();
    }
}
