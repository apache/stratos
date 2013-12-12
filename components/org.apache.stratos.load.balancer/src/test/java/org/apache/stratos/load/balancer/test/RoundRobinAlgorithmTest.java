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
package org.apache.stratos.load.balancer.test;

import org.apache.stratos.load.balancer.algorithm.AlgorithmContext;
import org.apache.stratos.load.balancer.algorithm.RoundRobin;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

/**
 * Round robin algorithm test.
 */
@RunWith(JUnit4.class)
public class RoundRobinAlgorithmTest {

    @Test
    public final void testRoundRobinAlgorithm() {
        List<Member> members = new ArrayList<Member>();
        Member member = new Member("service1", "cluster1", "np1", "p1", "m1");
        member.setStatus(MemberStatus.Activated);
        members.add(member);

        member = new Member("service1", "cluster1", "np1", "p1", "m2");
        member.setStatus(MemberStatus.Activated);
        members.add(member);

        member = new Member("service1", "cluster1", "np1", "p1", "m3");
        member.setStatus(MemberStatus.Activated);
        members.add(member);

        RoundRobin algorithm = new RoundRobin();
        algorithm.setMembers(members);

        AlgorithmContext algorithmContext = new AlgorithmContext("service1", "cluster1");
        Member nextMember = algorithm.getNextMember(algorithmContext);
        Assert.assertEquals("Expected member not found", true, "m1".equals(nextMember.getMemberId()));

        nextMember = algorithm.getNextMember(algorithmContext);
        Assert.assertEquals("Expected member not found", true, "m2".equals(nextMember.getMemberId()));

        nextMember = algorithm.getNextMember(algorithmContext);
        Assert.assertEquals("Expected member not found", true, "m3".equals(nextMember.getMemberId()));

        nextMember = algorithm.getNextMember(algorithmContext);
        Assert.assertEquals("Expected member not found", true, "m1".equals(nextMember.getMemberId()));
    }
}
