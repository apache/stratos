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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.StatelessKnowledgeSession;
import org.junit.Before;
import org.junit.Test;

public class TestKnowledgeBase {
    private static final Log log = LogFactory.getLog(TestKnowledgeBase.class);
    private String droolsFilePath = "src/test/resources/test-minimum-autoscaler-rule.drl";
    private KnowledgeBase kbase;
    private StatefulKnowledgeSession ksession;
    private StatelessKnowledgeSession ksession1;

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
    }
    
    @Test
    public void testMinimumRule() {
        if(kbase == null) {
            throw new IllegalArgumentException("Knowledge base is null.");
        }
        
//        ksession1 = kbase.newStatelessKnowledgeSession();
        ksession = kbase.newStatefulKnowledgeSession();
        List<String> p = new ArrayList<String>();
        p.add("aa");
        p.add("bb");
//        p.setId("pp");
//        ksession.setGlobal("pa", p);
//        ksession.setGlobal("log", log);
//        ksession.setGlobal("$manager", PolicyManager.getInstance());
//        ksession.setGlobal("$topology", TopologyManager.getTopology());
//        ksession.setGlobal("$evaluator", this);
//        ksession1.execute(p);
//        FactHandle handle = ksession.insert(p);
        ksession.insert(p);
        ksession.fireAllRules();
//        p = new Partition();
//        p.setId("3");
//        ksession.update(handle, p);
//        ksession.fireAllRules();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//        System.err.println(p.getId());
//        ksession1.execute(p);
//        ksession.insert(p);
//        ksession.execute(p);
        
    }
}