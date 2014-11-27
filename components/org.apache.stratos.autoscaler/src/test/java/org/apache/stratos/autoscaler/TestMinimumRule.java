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
import org.apache.stratos.cloud.controller.stub.pojo.*;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class TestMinimumRule {
    private static final Log log = LogFactory.getLog(TestMinimumRule.class);
    private String droolsFilePath = "src/test/resources/test-minimum-autoscaler-rule.drl";
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

        conf = ConfUtil.getInstance("src/test/resources/autoscaler.xml").getConfiguration();
    }

    @Test
    public void testMinimumRule() {
        if(kbase == null) {
            throw new IllegalArgumentException("Knowledge base is null.");
        }
        
        assertEquals(false, TestDelegator.isMinRuleFired());
        
        ksession = kbase.newStatefulKnowledgeSession();
        ksession.setGlobal("clusterId", "lb.cluster.1");
        ksession.setGlobal("lbRef", null);
        ClusterLevelPartitionContext p = new ClusterLevelPartitionContext(conf.getLong("autoscaler.member.expiryTimeout", 900000));
        p.setPendingMembers(new ArrayList<MemberContext>());
        p.setMinimumMemberCount(1);
        ksession.insert(p);
        ksession.fireAllRules();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        assertEquals(true, TestDelegator.isMinRuleFired());
        
    }
    
    public static String get() {
        return "null";
    }
}