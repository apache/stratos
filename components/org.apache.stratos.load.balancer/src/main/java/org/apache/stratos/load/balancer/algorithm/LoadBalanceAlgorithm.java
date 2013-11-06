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

import org.apache.stratos.messaging.domain.topology.Member;

import java.util.List;

/**
 * Defines the specification for implementing load balancing algorithms.
 */
public interface LoadBalanceAlgorithm {
    /**
     * Return algorithm name.
     *
     * @return
     */
    public String getName();

    /**
     * Apply the algorithm and return the next member.
     *
     * @param algorithmContext
     * @return
     */
    public Member getNextMember(AlgorithmContext algorithmContext);

    /**
     * Set member list of a given cluster.
     *
     * @param members
     */
    public void setMembers(List<Member> members);

    /**
     * Reset the algorithm and start from the beginning.
     *
     * @param algorithmContext
     */
    public void reset(AlgorithmContext algorithmContext);

    /**
     * Clone algorithm object.
     *
     * @return
     */
    public LoadBalanceAlgorithm clone();
}
