/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.load.balancer.integration.tests;

import org.apache.stratos.load.balancer.algorithm.AlgorithmContext;
import org.apache.stratos.load.balancer.algorithm.RoundRobin;
import org.apache.stratos.load.balancer.cache.AlgorithmContextCache;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

/**
* Algorithm context cache test.
*/
@RunWith(JUnit4.class)
public class AlgorithmContextCacheTest {

    @Test
    public final void testAlgorithmContextCache() {
        testCurrentMemberIndex("Service1", "Cluster1", 0);
        testCurrentMemberIndex("Service1", "Cluster1", 1);
        testCurrentMemberIndex("Service1", "Cluster1", 2);
        testCurrentMemberIndex("Service1", "Cluster1", 3);
        testCurrentMemberIndex("Service1", "Cluster1", 2);
        testCurrentMemberIndex("Service1", "Cluster1", 1);
        testCurrentMemberIndex("Service1", "Cluster1", 0);
    }

    private void testCurrentMemberIndex(String serviceName, String clusterId, int index) {
        AlgorithmContextCache.putCurrentMemberIndex(serviceName, clusterId, index);
        Assert.assertEquals("Expected algorithm context member index not found", true, (AlgorithmContextCache.getCurrentMemberIndex(serviceName, clusterId) == index));
    }
}
