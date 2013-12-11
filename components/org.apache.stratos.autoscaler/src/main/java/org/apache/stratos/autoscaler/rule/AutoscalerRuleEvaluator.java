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
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.algorithm.AutoscaleAlgorithm;
import org.apache.stratos.autoscaler.algorithm.OneAfterAnother;
import org.apache.stratos.autoscaler.algorithm.RoundRobin;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.pojo.MemberContext;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;

/**
 * This class is responsible for evaluating the current details of topology, statistics, and health
 * status against the rules set(written in Drools)
 */
public class AutoscalerRuleEvaluator {
	
	private static final Log log = LogFactory.getLog(AutoscalerRuleEvaluator.class);
	
	private static AutoscalerRuleEvaluator instance = null;
	private static final String DRL_FILE_NAME = "mincheck.drl";
	private static final String SCALING_DRL_FILE_NAME = "scaling.drl";

	private static KnowledgeBase minCheckKbase;
	private static KnowledgeBase scaleCheckKbase;


    public static double scaleUpFactor = 0.8;   //get from config
    public static double scaleDownFactor = 0.2;
    public static double scaleDownLowerRate = 0.8;

    public AutoscalerRuleEvaluator(){

        minCheckKbase = readKnowledgeBase(DRL_FILE_NAME);

        if (log.isDebugEnabled()) {
            log.debug("Minimum check rule is parsed successfully");
        }

        scaleCheckKbase = readKnowledgeBase(SCALING_DRL_FILE_NAME);

        if (log.isDebugEnabled()) {
            log.debug("Scale check rule is parsed successfully");
        }
    }

    
    
    public static FactHandle evaluateMinCheck(StatefulKnowledgeSession ksession, FactHandle handle, Object obj) {

        if (handle == null) {

            ksession.setGlobal("$evaluator", new AutoscalerRuleEvaluator());
            handle = ksession.insert(obj);
        } else {
            ksession.update(handle, obj);
        }
        ksession.fireAllRules();
        log.info("fired all rules "+obj);
        return handle;
    }


    public static FactHandle evaluateScaleCheck(StatefulKnowledgeSession ksession, FactHandle handle, Object obj) {

        if (handle == null) {
            ksession.setGlobal("$evaluator", new AutoscalerRuleEvaluator());

            handle = ksession.insert(obj);
        } else {
            ksession.update(handle, obj);
        }
        ksession.fireAllRules();
        log.info("fired all rules "+obj);
        return handle;
    }



    public StatefulKnowledgeSession getMinCheckStatefulSession() {
        StatefulKnowledgeSession ksession;
        ksession = minCheckKbase.newStatefulKnowledgeSession();
        return ksession;
    }
    public StatefulKnowledgeSession getScaleCheckStatefulSession() {
        StatefulKnowledgeSession ksession;
        ksession = scaleCheckKbase.newStatefulKnowledgeSession();
        return ksession;
    }
    
	public void delegateSpawn(PartitionContext partitionContext, String clusterId) {
		try {
		    
		    String nwPartitionId = partitionContext.getNetworkPartitionId();
		    NetworkPartitionContext ctxt = PartitionManager.getInstance().getNetworkPartition(nwPartitionId);
		    
		    Properties props = partitionContext.getProperties();
		    String value = (String) props.get(org.apache.stratos.messaging.util.Constants.LOAD_BALANCER_REF);
		    String lbClusterId;
		    
		    if(value.equals(org.apache.stratos.messaging.util.Constants.DEFAULT_LOAD_BALANCER)) {
		        lbClusterId = ctxt.getDefaultLbClusterId();
		    } else if(value.equals(org.apache.stratos.messaging.util.Constants.SERVICE_AWARE_LOAD_BALANCER)) {
		        String serviceName = partitionContext.getServiceName();
		        lbClusterId = ctxt.getLBClusterIdOfService(serviceName);
		    }
		   
                MemberContext memberContext = CloudControllerClient.getInstance()
                        .spawnAnInstance(partitionContext.getPartition(), clusterId, lbClusterId);
                if( memberContext!= null){
                    partitionContext.addPendingMember(memberContext);
                }

		} catch (Throwable e) {
			String message = "Cannot spawn an instance";
            log.error(message, e);
			throw new RuntimeException(message, e);
		}
	}

    public void delegateTerminate(Partition partition, String clusterId) {
   		log.info("terminate from partition " + partition.getId() + " cluster " + clusterId );
   	}

    public void delegateTerminate(String memberId) {
   		try {

   			CloudControllerClient.getInstance().terminate(memberId);
   		} catch (Throwable e) {
   			log.error("Cannot terminate instance", e);
   		}
   	}
	
	public void delegateTerminateAll(String clusterId) {
        try {

            CloudControllerClient.getInstance().terminateAllInstances(clusterId);
        } catch (Throwable e) {
            log.error("Cannot terminate instance", e);
        }
    }

//	public boolean delegateSpawn(Partition partition, String clusterId, int memberCountToBeIncreased) {
//		CloudControllerClient cloudControllerClient = new CloudControllerClient();
//		try {
//            int currentMemberCount = AutoscalerContext.getInstance().getClusterMonitor(clusterId).getMemberCount();
//            log.info("Current member count is " + currentMemberCount );
//
//            if(currentMemberCount < partition.getPartitionMembersMax()) {
//                AutoscalerContext.getInstance().getClusterMonitor(clusterId).increaseMemberCount(memberCountToBeIncreased);
//                cloudControllerClient.spawnInstances(partition, clusterId, memberCountToBeIncreased);
//            }
//			return true;
//		} catch (Throwable e) {
//			log.error("Cannot spawn an instance", e);
//		}
//		return false;
//	}

    private static KnowledgeBase readKnowledgeBase(String drlFileName) {
        
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        String configDir = CarbonUtils.getCarbonConfigDirPath();
        Resource resource = ResourceFactory.newFileResource(configDir + File.separator + drlFileName );
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

    public AutoscaleAlgorithm getAutoscaleAlgorithm(String partitionAlgorithm){
        AutoscaleAlgorithm autoscaleAlgorithm = null;
        if(Constants.ROUND_ROBIN_ALGORITHM_ID.equals(partitionAlgorithm)){

            autoscaleAlgorithm = new RoundRobin();
        } else if(Constants.ONE_AFTER_ANOTHER_ALGORITHM_ID.equals(partitionAlgorithm)){

            autoscaleAlgorithm = new OneAfterAnother();
        }
        return autoscaleAlgorithm;
    }

    public double getPredictedValueForNextMinute(float average, float gradient, float secondDerivative){
        double predictedValue;
//        s = u * t + 0.5 * a * t * t
        if(log.isDebugEnabled()) {
            log.debug((String.format("Calculating predicted value, gradient %s ", gradient)));
        }
        predictedValue = average + gradient + 0.5 * secondDerivative;
        return predictedValue;
    }


}
