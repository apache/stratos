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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.drools.KnowledgeBase;
import org.drools.builder.*;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestRulesPackaged {
    private static final Log log = LogFactory.getLog(TestRulesPackaged.class);
    private String minCheckDrlFilePath = "../../products/stratos/modules/distribution/src/main/conf/drools/mincheck.drl";
    private String scalingDrlFilePath = "../../products/stratos/modules/distribution/src/main/conf/drools/scaling.drl";
    private String terminateAllDrlFilePath = "../../products/stratos/modules/distribution/src/main/conf/drools/terminateall.drl";
    private KnowledgeBase kbase;
    private StatefulKnowledgeSession ksession;

    @Test
    public void testMinCheckDroolsFile() {
        parseDroolsFile(minCheckDrlFilePath);
    }

    @Test
    public void testScalingDroolsFile() {
        parseDroolsFile(scalingDrlFilePath);
    }

    @Test
    public void testTerminateAllDroolsFile() {
        parseDroolsFile(terminateAllDrlFilePath);
    }

    private void parseDroolsFile(String droolsFilePath) {
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
    }
}
