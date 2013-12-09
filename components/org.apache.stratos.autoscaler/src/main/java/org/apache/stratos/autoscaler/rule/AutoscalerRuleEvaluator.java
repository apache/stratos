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
import org.apache.stratos.autoscaler.ClusterMonitor;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.algorithm.AutoscaleAlgorithm;
import org.apache.stratos.autoscaler.algorithm.OneAfterAnother;
import org.apache.stratos.autoscaler.algorithm.PartitionGroupOneAfterAnother;
import org.apache.stratos.autoscaler.algorithm.RoundRobin;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.pojo.MemberContext;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
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
	
	private static AutoscalerRuleEvaluator instance = null;
	private static final String DRL_FILE_NAME = "autoscaler.drl";
	private Map<String, ClusterMonitor> monitors;
	private static KnowledgeBase kbase;

	private AutoscalerRuleEvaluator() {
        try {
            kbase = readKnowledgeBase();
            setMonitors(new HashMap<String, ClusterMonitor>());
        } catch (Exception e) {
            log.error("Rule evaluate error", e);
        }
    }
    
    
    public static FactHandle evaluate(StatefulKnowledgeSession ksession, FactHandle handle, Object obj) {

        if (handle == null) {

            handle = ksession.insert(obj);
        } else {
            ksession.update(handle, obj);
        }
        ksession.fireAllRules();
        log.info("fired all rules "+obj);
        return handle;
    }

    public void addMonitor(ClusterMonitor monitor) {
        monitors.put(monitor.getClusterId(), monitor);
    }
    
    public ClusterMonitor getMonitor(String clusterId) {
        return monitors.get(clusterId);
    }
    
    public ClusterMonitor removeMonitor(String clusterId) {
        return monitors.remove(clusterId);
    }

    public StatefulKnowledgeSession getStatefulSession() {
        StatefulKnowledgeSession ksession;
        ksession = kbase.newStatefulKnowledgeSession();
//        ksession.setGlobal("$partitions", ctxt.getPartitionsOfThisCluster());
//        ksession.setGlobal("log", log);
//        ksession.setGlobal("$manager", PolicyManager.getInstance());
//        ksession.setGlobal("$topology", TopologyManager.getTopology());
//        ksession.setGlobal("$evaluator", this);
        return ksession;
//        ksession.insert(clusterCtxt);
//        ksession.fireAllRules();
    }
    
	public static MemberContext delegateSpawn(Partition partition, String clusterId) {
		try {
//            int currentMemberCount = AutoscalerContext.getInstance().getClusterContext(clusterId).getMemberCount();

//            if(currentMemberCount < partition.getPartitionMembersMax())       {
//                AutoscalerContext.getInstance().getClusterContext(clusterId).increaseMemberCount(1);
    			return CloudControllerClient.getInstance().spawnAnInstance(partition, clusterId);

		} catch (Throwable e) {
			String message = "Cannot spawn an instance";
            log.error(message, e);
			throw new RuntimeException(message, e);
		}
	}

	public static void delegateTerminate(String memberId) {
		try {

			CloudControllerClient.getInstance().terminate(memberId);
		} catch (Throwable e) {
			log.error("Cannot terminate instance", e);
		}
	}
	
	public static void delegateTerminateAll(String clusterId) {
        try {

            CloudControllerClient.getInstance().terminateAllInstances(clusterId);
        } catch (Throwable e) {
            log.error("Cannot terminate instance", e);
        }
    }

//	public boolean delegateSpawn(Partition partition, String clusterId, int memberCountToBeIncreased) {
//		CloudControllerClient cloudControllerClient = new CloudControllerClient();
//		try {
//            int currentMemberCount = AutoscalerContext.getInstance().getClusterContext(clusterId).getMemberCount();
//            log.info("Current member count is " + currentMemberCount );
//
//            if(currentMemberCount < partition.getPartitionMembersMax()) {
//                AutoscalerContext.getInstance().getClusterContext(clusterId).increaseMemberCount(memberCountToBeIncreased);
//                cloudControllerClient.spawnInstances(partition, clusterId, memberCountToBeIncreased);
//            }
//			return true;
//		} catch (Throwable e) {
//			log.error("Cannot spawn an instance", e);
//		}
//		return false;
//	}

    public static AutoscalerRuleEvaluator getInstance() {
        if (instance == null) {
            synchronized (AutoscalerRuleEvaluator.class){
                if (instance == null) {
                    instance = new AutoscalerRuleEvaluator();
                }
            }
        }
        return instance;
    }
    
    private KnowledgeBase readKnowledgeBase() throws Exception {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        String configDir = CarbonUtils.getCarbonConfigDirPath();
        Resource resource = ResourceFactory.newFileResource(configDir + File.separator + DRL_FILE_NAME);
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

    public Partition getNextScaleUpPartition(String clusterID)
    {
    	return new PartitionGroupOneAfterAnother().getNextScaleUpPartition(clusterID);
    }
    
    public Partition getNextScaleDownPartition(String clusterID)
    {
    	return new PartitionGroupOneAfterAnother().getNextScaleDownPartition(clusterID);
    }


    public Map<String, ClusterMonitor> getMonitors() {
        return monitors;
    }


    public void setMonitors(Map<String, ClusterMonitor> monitors) {
        this.monitors = monitors;
    }
}
