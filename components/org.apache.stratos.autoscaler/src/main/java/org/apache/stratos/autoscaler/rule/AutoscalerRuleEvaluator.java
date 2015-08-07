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

package org.apache.stratos.autoscaler.rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.constants.StratosConstants;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for evaluating the current details of topology, statistics, and health
 * status against the rules set(written in Drools)
 */
public class AutoscalerRuleEvaluator {

    private static final Log log = LogFactory.getLog(AutoscalerRuleEvaluator.class);
    private static volatile AutoscalerRuleEvaluator instance;
    private Map<String, KnowledgeBase> knowledgeBases;

    private AutoscalerRuleEvaluator() {
        knowledgeBases = new HashMap<String, KnowledgeBase>();
        parseAndBuildKnowledgeBaseForDroolsFile(StratosConstants.OBSOLETE_CHECK_DROOL_FILE);
        parseAndBuildKnowledgeBaseForDroolsFile(StratosConstants.SCALE_CHECK_DROOL_FILE);
        parseAndBuildKnowledgeBaseForDroolsFile(StratosConstants.MIN_CHECK_DROOL_FILE);
        parseAndBuildKnowledgeBaseForDroolsFile(StratosConstants.MAX_CHECK_DROOL_FILE);
        parseAndBuildKnowledgeBaseForDroolsFile(StratosConstants.DEPENDENT_SCALE_CHECK_DROOL_FILE);


    }

    public static AutoscalerRuleEvaluator getInstance() {
        if (instance == null) {
            synchronized (AutoscalerRuleEvaluator.class) {
                if (instance == null) {
                    instance = new AutoscalerRuleEvaluator();
                }
            }
        }
        return instance;
    }

    private static KnowledgeBase readKnowledgeBase(String drlFileName) {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        String configDir = CarbonUtils.getCarbonConfigDirPath();
        String droolsDir = configDir + File.separator + StratosConstants.DROOLS_DIR_NAME;
        Resource resource = ResourceFactory.newFileResource(droolsDir + File.separator + drlFileName);
        kbuilder.add(resource, ResourceType.DRL);
        KnowledgeBuilderErrors errors = kbuilder.getErrors();
        if (errors.size() > 0) {
            for (KnowledgeBuilderError error : errors) {
                log.error(error.getMessage());
            }
            throw new IllegalArgumentException("Could not parse knowledge");
        }
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        return kbase;
    }

    public void parseAndBuildKnowledgeBaseForDroolsFile(String drlFileName) {
        KnowledgeBase knowledgeBase = readKnowledgeBase(drlFileName);
        if (knowledgeBase == null) {
            log.error("Knowledge base couldn't be read for" +
                    " [drool-file] " + drlFileName);
        } else {
            knowledgeBases.put(drlFileName, knowledgeBase);
            if (log.isDebugEnabled()) {
                log.debug("Drools file is parsed successfully:" +
                        " [ file-name] " + drlFileName);
            }
        }
    }

    public StatefulKnowledgeSession getStatefulSession(String drlFileName) {
        StatefulKnowledgeSession ksession;
        ksession = knowledgeBases.get(drlFileName).newStatefulKnowledgeSession();
        ksession.setGlobal("log", RuleLog.getInstance());
        return ksession;
    }
}
