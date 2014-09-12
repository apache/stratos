/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler.util;

import org.apache.axiom.om.OMElement;
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
import org.apache.stratos.autoscaler.monitor.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.KubernetesClusterMonitor;
import org.apache.stratos.autoscaler.monitor.LbClusterMonitor;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.util.Constants;

import javax.xml.namespace.QName;

import java.util.*;

/**
 * This class contains utility methods used by Autoscaler.
 */
public class AutoscalerUtil {

    private static final Log log = LogFactory.getLog(AutoscalerUtil.class);

    private AutoscalerUtil() {

    }


    /**
     * Updates ClusterContext for given cluster
     *
     * @param cluster
     * @return ClusterMonitor - Updated ClusterContext
     * @throws PolicyValidationException
     * @throws PartitionValidationException
     */
    public static ClusterMonitor getClusterMonitor(Cluster cluster) throws PolicyValidationException, PartitionValidationException {
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

        ClusterMonitor clusterMonitor =
                                        new ClusterMonitor(cluster.getClusterId(),
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

        log.info("Cluster monitor created: "+clusterMonitor.toString());
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


	public static LbClusterMonitor getLBClusterMonitor(Cluster cluster) throws PolicyValidationException, PartitionValidationException {
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
        LbClusterMonitor clusterMonitor =
                                        new LbClusterMonitor(clusterId,
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

        log.info("LB Cluster monitor created: "+clusterMonitor.toString());
        return clusterMonitor;
    }
	
    public static KubernetesClusterMonitor getKubernetesClusterMonitor(Cluster cluster) {

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
		KubernetesClusterContext kubernetesClusterCtxt = new KubernetesClusterContext(kubernetesHostClusterID);

        KubernetesClusterMonitor kubernetesClusterMonitor = new KubernetesClusterMonitor(
        		kubernetesClusterCtxt, 
        		cluster.getClusterId(), 
        		cluster.getServiceName(), 
        		policy);
                                        
        kubernetesClusterMonitor.setStatus(ClusterStatus.Created);
        
        // find lb reference type
        if(props.containsKey(Constants.LOAD_BALANCER_REF)) {
            String value = props.getProperty(Constants.LOAD_BALANCER_REF);
            kubernetesClusterMonitor.setLbReferenceType(value);
            if(log.isDebugEnabled()) {
                log.debug("Set the lb reference type: "+value);
            }
        }
        
        // set hasPrimary property
        // hasPrimary is true if there are primary members available in that cluster
        kubernetesClusterMonitor.setHasPrimary(Boolean.parseBoolean(props.getProperty(Constants.IS_PRIMARY)));

        log.info("Kubernetes cluster monitor created: "+ kubernetesClusterMonitor.toString());
        return kubernetesClusterMonitor;
    }
    
    public static Properties getProperties(final OMElement elt) {

        Iterator<?> it = elt.getChildrenWithName(new QName(AutoScalerConstants.PROPERTY_ELEMENT));
        ArrayList<Property> propertyList = new ArrayList<Property>();

        while (it.hasNext()) {
            OMElement prop = (OMElement) it.next();

            if (prop.getAttribute(new QName(AutoScalerConstants.PROPERTY_NAME_ATTR)) == null ||
                prop.getAttribute(new QName(AutoScalerConstants.PROPERTY_VALUE_ATTR)) == null) {

                String msg =
                             "Property element's, name and value attributes should be specified. "
                                     + "Property: ";
                log.warn(msg + prop.toString());

            }

            String name =
                          prop.getAttribute(new QName(AutoScalerConstants.PROPERTY_NAME_ATTR))
                              .getAttributeValue();
            String value =
                           prop.getAttribute(new QName(AutoScalerConstants.PROPERTY_VALUE_ATTR))
                               .getAttributeValue();

            Property property = new Property();
            property.setName(name);
            property.setValue(value);
            propertyList.add(property);
        }
        
        if(propertyList.isEmpty()) {
            return null;
        }

        Property[] propertyArray = propertyList.toArray(new Property[propertyList.size()]);
        Properties properties = new Properties();
        properties.setProperties(propertyArray);
        return properties;
    }

//    public static LbClusterMonitor getLbClusterMonitor(Cluster cluster) throws PolicyValidationException, PartitionValidationException {
//        if (null == cluster) {
//               return null;
//           }
//
//           String autoscalePolicyName = cluster.getAutoscalePolicyName();
//           String deploymentPolicyName = cluster.getDeploymentPolicyName();
//
//           if (log.isDebugEnabled()) {
//               log.debug("Deployment policy name: " + deploymentPolicyName);
//               log.debug("Autoscaler policy name: " + autoscalePolicyName);
//           }
//
//           AutoscalePolicy policy =
//                                    PolicyManager.getInstance()
//                                                 .getAutoscalePolicy(autoscalePolicyName);
//           DeploymentPolicy deploymentPolicy =
//                                               PolicyManager.getInstance()
//                                                            .getDeploymentPolicy(deploymentPolicyName);
//
//           if (deploymentPolicy == null) {
//               String msg = "Deployment Policy is null. Policy name: " + deploymentPolicyName;
//               log.error(msg);
//               throw new PolicyValidationException(msg);
//           }
//
//           Partition[] allPartitions = deploymentPolicy.getAllPartitions();
//           if (allPartitions == null) {
//               String msg =
//                            "Deployment Policy's Partitions are null. Policy name: " +
//                                    deploymentPolicyName;
//               log.error(msg);
//               throw new PolicyValidationException(msg);
//           }
//
//           try {
//               validateExistenceOfPartions(allPartitions);
//           } catch (InvalidPartitionException e) {
//               String msg = "Deployment Policy is invalid. Policy name: " + deploymentPolicyName;
//               log.error(msg, e);
//               throw new PolicyValidationException(msg, e);
//           }
//
//           CloudControllerClient.getInstance()
//                                .validateDeploymentPolicy(cluster.getServiceName(),
//                                                            allPartitions);
//
//           LbClusterMonitor clusterMonitor =
//                                           new LbClusterMonitor(cluster.getClusterId(),
//                                                              cluster.getServiceName(),
//                                                              deploymentPolicy, policy);
//           for (PartitionGroup partitionGroup: deploymentPolicy.getPartitionGroups()){
//
//               NetworkPartitionContext networkPartitionContext
//                       = PartitionManager.getInstance().getNetworkPartitionLbHolder(partitionGroup.getNetworkPartitionId());
//               clusterMonitor.addNetworkPartitionCtxt(networkPartitionContext);
//           }
//        return null;
//    }
}
