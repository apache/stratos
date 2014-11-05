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
import org.apache.stratos.autoscaler.NetworkPartitionLbHolder;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.common.constants.StratosConstants;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.Properties;

/**
 * This class is responsible for evaluating the current details of topology, statistics, and health
 * status against the rules set(written in Drools)
 */
public class AutoscalerRuleEvaluator {
	
	private static final Log log = LogFactory.getLog(AutoscalerRuleEvaluator.class);

	//vm drool files as default
	private String minCheckDroolFileName = StratosConstants.CONTAINER_MIN_CHECK_DROOL_FILE;
	private String obsoleteCheckDroolFileName = StratosConstants.CONTAINER_OBSOLETE_CHECK_DROOL_FILE;
	private String scaleCheckDroolFileName = StratosConstants.CONTAINER_SCALE_CHECK_DROOL_FILE;
	private String terminateAllDroolFileName = "terminateall.drl";

	private static KnowledgeBase minCheckKbase;
	private static KnowledgeBase obsoleteCheckKbase;
	private static KnowledgeBase scaleCheckKbase;
	@SuppressWarnings("unused")
	private static KnowledgeBase terminateAllKbase;

    public AutoscalerRuleEvaluator(String minCheckDroolFileName, String obsoleteCheckDroolFileName, String scaleCheckDroolFileName){
    	
    	if (minCheckDroolFileName != null && !minCheckDroolFileName.isEmpty()) {
    		this.minCheckDroolFileName = minCheckDroolFileName;
		}

    	if (obsoleteCheckDroolFileName != null && !obsoleteCheckDroolFileName.isEmpty()) {
    		this.obsoleteCheckDroolFileName = obsoleteCheckDroolFileName;
		}
    	
    	if (scaleCheckDroolFileName != null && !scaleCheckDroolFileName.isEmpty()) {
    		this.scaleCheckDroolFileName = scaleCheckDroolFileName;
		}

        minCheckKbase = readKnowledgeBase(this.minCheckDroolFileName);

        if (log.isDebugEnabled()) {
            log.debug("Minimum check rule is parsed successfully : " + this.minCheckDroolFileName);
        }
        
        obsoleteCheckKbase = readKnowledgeBase(this.obsoleteCheckDroolFileName);

        if (log.isDebugEnabled()) {
            log.debug("Obsolete check rule is parsed successfully : " + this.obsoleteCheckDroolFileName);
        }

        scaleCheckKbase = readKnowledgeBase(this.scaleCheckDroolFileName);

        if (log.isDebugEnabled()) {
            log.debug("Scale check rule is parsed successfully : " + this.scaleCheckDroolFileName);
        }
        
        terminateAllKbase = readKnowledgeBase(this.terminateAllDroolFileName);

        if (log.isDebugEnabled()) {
            log.debug("Terminate all rule is parsed successfully : " + this.terminateAllDroolFileName);
        }
    }
    
    public static FactHandle evaluateMinCheck(StatefulKnowledgeSession ksession, FactHandle handle, Object obj) {
        if (handle == null) {
            ksession.setGlobal("$delegator", new RuleTasksDelegator());
            handle = ksession.insert(obj);
        } else {
            ksession.update(handle, obj);
        }
        ksession.fireAllRules();
        if(log.isDebugEnabled()){
            log.debug(String.format("Minimum check executed for : %s ", obj));
        }
        return handle;
    }
    
    public static FactHandle evaluateObsoleteCheck(StatefulKnowledgeSession ksession, FactHandle handle, Object obj) {
        if (handle == null) {
            ksession.setGlobal("$delegator", new RuleTasksDelegator());
            handle = ksession.insert(obj);
        } else {
            ksession.update(handle, obj);
        }
        ksession.fireAllRules();
        if(log.isDebugEnabled()){
            log.debug(String.format("Obsolete check executed for : %s ", obj));
        }
        return handle;
    }

    public static FactHandle evaluateScaleCheck(StatefulKnowledgeSession ksession, FactHandle handle, Object obj) {
        if (handle == null) {
            ksession.setGlobal("$delegator", new RuleTasksDelegator());
            handle = ksession.insert(obj);
        } else {
            ksession.update(handle, obj);
        }
        ksession.fireAllRules();
        if(log.isDebugEnabled()){
            log.debug(String.format("Scale check executed for : %s ", obj));
        }
        return handle;
    }

    public static FactHandle evaluateTerminateAll(StatefulKnowledgeSession ksession, FactHandle handle, Object obj) {
        if (handle == null) {
            ksession.setGlobal("$delegator", new RuleTasksDelegator());
            handle = ksession.insert(obj);
        } else {
            ksession.update(handle, obj);
        }
        ksession.fireAllRules();
        if(log.isDebugEnabled()){
            log.debug(String.format("Terminate all check executed for : %s ", obj));
        }
        return handle;
    }
    
    public static FactHandle evaluateTerminateDependency(StatefulKnowledgeSession ksession, FactHandle handle, Object obj) {
    	if(log.isDebugEnabled()){
            log.debug(String.format("Terminate dependency check executing for : %s ", obj));
        }
        if (handle == null) {
            ksession.setGlobal("$delegator", new RuleTasksDelegator());
            handle = ksession.insert(obj);
        } else {
            ksession.update(handle, obj);
        }
        if(log.isDebugEnabled()){
            log.debug(String.format("Terminate dependency check firing rules for : %s ", ksession));
        }
        ksession.fireAllRules();
        if(log.isDebugEnabled()){
            log.debug(String.format("Terminate dependency check executed for : %s ", obj));
        }
        return handle;
    }

    public StatefulKnowledgeSession getMinCheckStatefulSession() {
        StatefulKnowledgeSession ksession;
        ksession = minCheckKbase.newStatefulKnowledgeSession();
        ksession.setGlobal("log", RuleLog.getInstance());
        return ksession;
    }
    
    public StatefulKnowledgeSession getObsoleteCheckStatefulSession() {
        StatefulKnowledgeSession ksession;
        ksession = obsoleteCheckKbase.newStatefulKnowledgeSession();
        ksession.setGlobal("log", RuleLog.getInstance());
        return ksession;
    }
    
    public StatefulKnowledgeSession getScaleCheckStatefulSession() {
        StatefulKnowledgeSession ksession;
        ksession = scaleCheckKbase.newStatefulKnowledgeSession();
        ksession.setGlobal("log", RuleLog.getInstance());
        return ksession;
    }
    
    public StatefulKnowledgeSession getTerminateAllStatefulSession() {
        StatefulKnowledgeSession ksession;
        ksession = scaleCheckKbase.newStatefulKnowledgeSession();
        ksession.setGlobal("log", RuleLog.getInstance());
        return ksession;
    }

    public static String getLbClusterId(PartitionContext partitionContext, String nwpartitionId) {
        Properties props = partitionContext.getProperties();
        String value =
                       (String) props.get(org.apache.stratos.messaging.util.Constants.LOAD_BALANCER_REF);

        if (value == null){
            return null;
        }

        String lbClusterId = null;

        NetworkPartitionLbHolder networkPartitionLbHolder = PartitionManager.getInstance().getNetworkPartitionLbHolder(nwpartitionId);
        if (value.equals(org.apache.stratos.messaging.util.Constants.DEFAULT_LOAD_BALANCER)) {
            lbClusterId = networkPartitionLbHolder.getDefaultLbClusterId();
        } else if (value.equals(org.apache.stratos.messaging.util.Constants.SERVICE_AWARE_LOAD_BALANCER)) {
            String serviceName = partitionContext.getServiceName();
            lbClusterId = networkPartitionLbHolder.getLBClusterIdOfService(serviceName);
        }
        return lbClusterId;
    }

    private static KnowledgeBase readKnowledgeBase(String drlFileName) {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        String configDir = CarbonUtils.getCarbonConfigDirPath();
        String droolsDir = configDir + File.separator + StratosConstants.DROOLS_DIR_NAME;
        Resource resource = ResourceFactory.newFileResource(droolsDir + File.separator + drlFileName);
		kbuilder.add(resource, ResourceType.DRL);
        KnowledgeBuilderErrors errors = kbuilder.getErrors();
        if (errors.size() > 0) {
            for (KnowledgeBuilderError error: errors) {
                log.error(error.getMessage());
            }
            throw new IllegalArgumentException("Could not parse knowledge.");
        }
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        return kbase;
    }
}
