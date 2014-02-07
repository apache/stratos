/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestObsoletedMemberRule {
    private static final Log log = LogFactory.getLog(TestObsoletedMemberRule.class);
    private String droolsFilePath = "src/test/resources/test-terminating-obsoleted-members-rule.drl";
    private KnowledgeBase kbase;
    private StatefulKnowledgeSession ksession;
    private XMLConfiguration conf;

    @Before
    public void setUp() {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        Resource resource = ResourceFactory.newFileResource(droolsFilePath);
        kbuilder.add(resource, ResourceType.DRL);
        KnowledgeBuilderErrors errors = kbuilder.getErrors();
        if (errors.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (KnowledgeBuilderError error : errors) {
                sb.append(error.getMessage());
            }
            if(sb.length() > 0) {
                log.error(sb.toString());
            }
            throw new IllegalArgumentException(String.format("Could not parse drools file: %s", droolsFilePath));
        }
        
        kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        log.info("Knowledge base has been set up.");

        conf = ConfUtil.getInstance("src/test/resources/autoscaler.xml").getConfiguration();
    }
    
    @Test
    public void testOneObsoletedMemberCase() {
        
        // reset helper class
        TestDelegator.setObsoletedMembers(new ArrayList<String>());
        
        if(kbase == null) {
            throw new IllegalArgumentException("Knowledge base is null.");
        }
        ksession = kbase.newStatefulKnowledgeSession();
        PartitionContext p = new PartitionContext(conf.getLong("autoscaler.member.expiryTimeout", 900000));
        p.setObsoletedMembers(new CopyOnWriteArrayList<String>());
        String memberId = "member1";
        p.addObsoleteMember(memberId);
        ksession.insert(p);
        ksession.fireAllRules();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        assertEquals(1, TestDelegator.getObsoletedMembers().size());
        assertEquals(memberId, TestDelegator.getObsoletedMembers().get(0));
        
    }
    
    @Test
    public void testMoreThanOneObsoletedMemberCase() {
        
        // reset helper class
        TestDelegator.setObsoletedMembers(new ArrayList<String>());
        
        if(kbase == null) {
            throw new IllegalArgumentException("Knowledge base is null.");
        }

        ksession = kbase.newStatefulKnowledgeSession();
        PartitionContext p = new PartitionContext(conf.getLong("autoscaler.member.expiryTimeout", 900000));
        p.setObsoletedMembers(new CopyOnWriteArrayList<String>());
        String memberId1 = "member1";
        String memberId2 = "member2";
        String memberId3 = "member3";
        
        p.addObsoleteMember(memberId1);
        p.addObsoleteMember(memberId2);
        
        FactHandle handle = ksession.insert(p);
        ksession.fireAllRules();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        assertEquals(2, TestDelegator.getObsoletedMembers().size());
        
        assertEquals(0, p.getObsoletedMembers().size());
        
        assertNotEquals(TestDelegator.getObsoletedMembers().get(0), TestDelegator.getObsoletedMembers().get(1));
        
        boolean check0thPosition = memberId1.equals(TestDelegator.getObsoletedMembers().get(0)) ||
                memberId2.equals(TestDelegator.getObsoletedMembers().get(0));
        assertEquals(true, check0thPosition);
        
        boolean check1stPosition = memberId1.equals(TestDelegator.getObsoletedMembers().get(1)) ||
                memberId2.equals(TestDelegator.getObsoletedMembers().get(2));
        assertEquals(true, check1stPosition);
        
        // reset helper class
        TestDelegator.setObsoletedMembers(new ArrayList<String>());
        
        p.addObsoleteMember(memberId3);
        ksession.update(handle, p);
        ksession.fireAllRules();
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        assertEquals(1, TestDelegator.getObsoletedMembers().size());
        assertEquals(memberId3, TestDelegator.getObsoletedMembers().get(0));
        
        
    }
    
    public static String get() {
        return "null";
    }
}