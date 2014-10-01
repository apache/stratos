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

package org.apache.stratos.autoscaler.monitor;

import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.KubernetesClusterContext;
import org.apache.stratos.autoscaler.MemberStatsContext;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.NetworkPartitionLbHolder;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.util.Constants;

/*
 * Factory class for creating cluster monitors.
 */
public class ClusterMonitorFactory {
	
	private static final Log log = LogFactory.getLog(ClusterMonitorFactory.class);

	/**
	 * @param cluster the cluster to be monitored
	 * @return the created cluster monitor
	 * @throws PolicyValidationException when deployment policy is not valid
	 * @throws PartitionValidationException when partition is not valid
	 */
	public static AbstractClusterMonitor getMonitor(Cluster cluster) throws PolicyValidationException, PartitionValidationException {
		
		AbstractClusterMonitor clusterMonitor;
		if(cluster.isKubernetesCluster()){
			clusterMonitor = getDockerServiceClusterMonitor(cluster);
		} else if (cluster.isLbCluster()){
			clusterMonitor = getVMLbClusterMonitor(cluster);
		} else {
			clusterMonitor = getVMServiceClusterMonitor(cluster);
		}
		
		return clusterMonitor;
	}
	
    private static VMServiceClusterMonitor getVMServiceClusterMonitor(Cluster cluster) throws PolicyValidationException, PartitionValidationException {
        // FIXME fix the following code to correctly update
        // AutoscalerContext context = AutoscalerContext.getInstance();
        if (null == cluster) {
            return null;
        }

        String autoscalePolicyName = cluster.getAutoscalePolicyName();
        String deploymentPolicyName = cluster.getDeploymentPolicyName();

        if (log.isDebugEnabled()) {
            log.debug("Deployment policy name: " + deploymentPolicyName);
            log.debug("Autoscaler policy name: " + autoscalePolicyName);
        }

        AutoscalePolicy policy =
                                 PolicyManager.getInstance()
                                              .getAutoscalePolicy(autoscalePolicyName);
        DeploymentPolicy deploymentPolicy =
                                            PolicyManager.getInstance()
                                                         .getDeploymentPolicy(deploymentPolicyName);

        if (deploymentPolicy == null) {
            String msg = "Deployment Policy is null. Policy name: " + deploymentPolicyName;
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        Partition[] allPartitions = deploymentPolicy.getAllPartitions();
        if (allPartitions == null) {
            String msg =
                         "Deployment Policy's Partitions are null. Policy name: " +
                                 deploymentPolicyName;
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        CloudControllerClient.getInstance().validateDeploymentPolicy(cluster.getServiceName(), deploymentPolicy);

        VMServiceClusterMonitor clusterMonitor =
                                        new VMServiceClusterMonitor(cluster.getClusterId(),
                                                           cluster.getServiceName(),
                                                           deploymentPolicy, policy);
        clusterMonitor.setStatus(ClusterStatus.Created);
        
        for (PartitionGroup partitionGroup: deploymentPolicy.getPartitionGroups()){

            NetworkPartitionContext networkPartitionContext = new NetworkPartitionContext(partitionGroup.getId(),
                    partitionGroup.getPartitionAlgo(), partitionGroup.getPartitions());

            for(Partition partition: partitionGroup.getPartitions()){
                PartitionContext partitionContext = new PartitionContext(partition);
                partitionContext.setServiceName(cluster.getServiceName());
                partitionContext.setProperties(cluster.getProperties());
                partitionContext.setNetworkPartitionId(partitionGroup.getId());
                
                for (Member member: cluster.getMembers()){
                    String memberId = member.getMemberId();
                    if(member.getPartitionId().equalsIgnoreCase(partition.getId())){
                        MemberContext memberContext = new MemberContext();
                        memberContext.setClusterId(member.getClusterId());
                        memberContext.setMemberId(memberId);
                        memberContext.setPartition(partition);
                        memberContext.setProperties(convertMemberPropsToMemberContextProps(member.getProperties()));
                        
                        if(MemberStatus.Activated.equals(member.getStatus())){
                            partitionContext.addActiveMember(memberContext);
//                            networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
//                            partitionContext.incrementCurrentActiveMemberCount(1);

                        } else if(MemberStatus.Created.equals(member.getStatus()) || MemberStatus.Starting.equals(member.getStatus())){
                            partitionContext.addPendingMember(memberContext);

//                            networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
                        } else if(MemberStatus.Suspended.equals(member.getStatus())){
//                            partitionContext.addFaultyMember(memberId);
                        }
                        partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                        if(log.isInfoEnabled()){
                            log.info(String.format("Member stat context has been added: [member] %s", memberId));
                        }
                    }

                }
                networkPartitionContext.addPartitionContext(partitionContext);
                if(log.isInfoEnabled()){
                    log.info(String.format("Partition context has been added: [partition] %s",
                            partitionContext.getPartitionId()));
                }
            }

            clusterMonitor.addNetworkPartitionCtxt(networkPartitionContext);
            if(log.isInfoEnabled()){
                log.info(String.format("Network partition context has been added: [network partition] %s",
                            networkPartitionContext.getId()));
            }
        }
        
        
        // find lb reference type
        java.util.Properties props = cluster.getProperties();
        
        if(props.containsKey(Constants.LOAD_BALANCER_REF)) {
            String value = props.getProperty(Constants.LOAD_BALANCER_REF);
            clusterMonitor.setLbReferenceType(value);
            if(log.isDebugEnabled()) {
                log.debug("Set the lb reference type: "+value);
            }
        }
        
        // set hasPrimary property
        // hasPrimary is true if there are primary members available in that cluster
        clusterMonitor.setHasPrimary(Boolean.parseBoolean(cluster.getProperties().getProperty(Constants.IS_PRIMARY)));

        log.info("VMServiceClusterMonitor created: "+clusterMonitor.toString());
        return clusterMonitor;
    }
    
    private static Properties convertMemberPropsToMemberContextProps(
			java.util.Properties properties) {
    	Properties props = new Properties();
    	for (Map.Entry<Object, Object> e : properties.entrySet()	) {
			Property prop = new Property();
			prop.setName((String)e.getKey());
			prop.setValue((String)e.getValue());
			props.addProperties(prop);
		}    	
		return props;
	}


	private static VMLbClusterMonitor getVMLbClusterMonitor(Cluster cluster) throws PolicyValidationException, PartitionValidationException {
        // FIXME fix the following code to correctly update
        // AutoscalerContext context = AutoscalerContext.getInstance();
        if (null == cluster) {
            return null;
        }

        String autoscalePolicyName = cluster.getAutoscalePolicyName();
        String deploymentPolicyName = cluster.getDeploymentPolicyName();

        if (log.isDebugEnabled()) {
            log.debug("Deployment policy name: " + deploymentPolicyName);
            log.debug("Autoscaler policy name: " + autoscalePolicyName);
        }

        AutoscalePolicy policy =
                                 PolicyManager.getInstance()
                                              .getAutoscalePolicy(autoscalePolicyName);
        DeploymentPolicy deploymentPolicy =
                                            PolicyManager.getInstance()
                                                         .getDeploymentPolicy(deploymentPolicyName);

        if (deploymentPolicy == null) {
            String msg = "Deployment Policy is null. Policy name: " + deploymentPolicyName;
            log.error(msg);
            throw new PolicyValidationException(msg);
        }

        String clusterId = cluster.getClusterId();
        VMLbClusterMonitor clusterMonitor =
                                        new VMLbClusterMonitor(clusterId,
                                                           cluster.getServiceName(),
                                                           deploymentPolicy, policy);
        clusterMonitor.setStatus(ClusterStatus.Created);
        // partition group = network partition context
        for (PartitionGroup partitionGroup : deploymentPolicy.getPartitionGroups()) {

            NetworkPartitionLbHolder networkPartitionLbHolder =
                                                              PartitionManager.getInstance()
                                                                              .getNetworkPartitionLbHolder(partitionGroup.getId());
//                                                              PartitionManager.getInstance()
//                                                                              .getNetworkPartitionLbHolder(partitionGroup.getId());
            // FIXME pick a random partition
            Partition partition =
                                  partitionGroup.getPartitions()[new Random().nextInt(partitionGroup.getPartitions().length)];
            PartitionContext partitionContext = new PartitionContext(partition);
            partitionContext.setServiceName(cluster.getServiceName());
            partitionContext.setProperties(cluster.getProperties());
            partitionContext.setNetworkPartitionId(partitionGroup.getId());
            partitionContext.setMinimumMemberCount(1);//Here it hard codes the minimum value as one for LB cartridge partitions

            NetworkPartitionContext networkPartitionContext = new NetworkPartitionContext(partitionGroup.getId(),
                    partitionGroup.getPartitionAlgo(), partitionGroup.getPartitions()) ;
            for (Member member : cluster.getMembers()) {
                String memberId = member.getMemberId();
                if (member.getNetworkPartitionId().equalsIgnoreCase(networkPartitionContext.getId())) {
                    MemberContext memberContext = new MemberContext();
                    memberContext.setClusterId(member.getClusterId());
                    memberContext.setMemberId(memberId);
                    memberContext.setPartition(partition);

                    if (MemberStatus.Activated.equals(member.getStatus())) {
                        partitionContext.addActiveMember(memberContext);
//                        networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
//                        partitionContext.incrementCurrentActiveMemberCount(1);
                    } else if (MemberStatus.Created.equals(member.getStatus()) ||
                               MemberStatus.Starting.equals(member.getStatus())) {
                        partitionContext.addPendingMember(memberContext);
//                        networkPartitionContext.increaseMemberCountOfPartition(partition.getNetworkPartitionId(), 1);
                    } else if (MemberStatus.Suspended.equals(member.getStatus())) {
//                        partitionContext.addFaultyMember(memberId);
                    }

                    partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                    if(log.isInfoEnabled()){
                        log.info(String.format("Member stat context has been added: [member] %s", memberId));
                    }
                }

            }
            networkPartitionContext.addPartitionContext(partitionContext);
            
            // populate lb cluster id in network partition context.
            java.util.Properties props = cluster.getProperties();

            // get service type of load balanced cluster
            String loadBalancedServiceType = props.getProperty(Constants.LOAD_BALANCED_SERVICE_TYPE);
            
            if(props.containsKey(Constants.LOAD_BALANCER_REF)) {
                String value = props.getProperty(Constants.LOAD_BALANCER_REF);
                
                if (value.equals(org.apache.stratos.messaging.util.Constants.DEFAULT_LOAD_BALANCER)) {
                    networkPartitionLbHolder.setDefaultLbClusterId(clusterId);

                } else if (value.equals(org.apache.stratos.messaging.util.Constants.SERVICE_AWARE_LOAD_BALANCER)) {
                    String serviceName = cluster.getServiceName();
                    // TODO: check if this is correct
                    networkPartitionLbHolder.addServiceLB(serviceName, clusterId);

                    if (loadBalancedServiceType != null && !loadBalancedServiceType.isEmpty()) {
                        networkPartitionLbHolder.addServiceLB(loadBalancedServiceType, clusterId);
                        if (log.isDebugEnabled()) {
                            log.debug("Added cluster id " + clusterId + " as the LB cluster id for service type " + loadBalancedServiceType);
                        }
                    }
                }
            }

            clusterMonitor.addNetworkPartitionCtxt(networkPartitionContext);
        }

        log.info("VMLbClusterMonitor created: "+clusterMonitor.toString());
        return clusterMonitor;
    }
	
    private static DockerServiceClusterMonitor getDockerServiceClusterMonitor(Cluster cluster) {

    	if (null == cluster) {
            return null;
        }

        String autoscalePolicyName = cluster.getAutoscalePolicyName();
        if (log.isDebugEnabled()) {
            log.debug("Autoscaler policy name: " + autoscalePolicyName);
        }

        AutoscalePolicy policy = PolicyManager.getInstance().getAutoscalePolicy(autoscalePolicyName);
        java.util.Properties props = cluster.getProperties();
        String kubernetesHostClusterID = props.getProperty(StratosConstants.KUBERNETES_CLUSTER_ID);
		KubernetesClusterContext kubernetesClusterCtxt = new KubernetesClusterContext(kubernetesHostClusterID, 
				cluster.getClusterId());

        DockerServiceClusterMonitor dockerClusterMonitor = new DockerServiceClusterMonitor(
        		kubernetesClusterCtxt, 
        		cluster.getClusterId(), 
        		cluster.getServiceName(), 
        		policy);
                                        
        dockerClusterMonitor.setStatus(ClusterStatus.Created);
        
		for (Member member : cluster.getMembers()) {
			String memberId = member.getMemberId();
			String clusterId = member.getClusterId();
			MemberContext memberContext = new MemberContext();
			memberContext.setMemberId(memberId);
			memberContext.setClusterId(clusterId);

			if (MemberStatus.Activated.equals(member.getStatus())) {
				dockerClusterMonitor.getKubernetesClusterCtxt().addActiveMember(memberContext);
			} else if (MemberStatus.Created.equals(member.getStatus())
					|| MemberStatus.Starting.equals(member.getStatus())) {
				dockerClusterMonitor.getKubernetesClusterCtxt().addPendingMember(memberContext);
			}
		}

        // find lb reference type
        if(props.containsKey(Constants.LOAD_BALANCER_REF)) {
            String value = props.getProperty(Constants.LOAD_BALANCER_REF);
            dockerClusterMonitor.setLbReferenceType(value);
            if(log.isDebugEnabled()) {
                log.debug("Set the lb reference type: "+value);
            }
        }
        
        log.info("KubernetesServiceClusterMonitor created: "+ dockerClusterMonitor.toString());
        return dockerClusterMonitor;
    }
}
