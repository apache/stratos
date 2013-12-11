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
import org.apache.stratos.autoscaler.*;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.InvalidPartitionException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.pojo.MemberContext;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.cloud.controller.pojo.Property;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;

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

        try {
            validateExistenceOfPartions(allPartitions);
        } catch (InvalidPartitionException e) {
            String msg = "Deployment Policy is invalid. Policy name: " + deploymentPolicyName;
            log.error(msg, e);
            throw new PolicyValidationException(msg, e);
        }

        CloudControllerClient.getInstance()
                             .validatePartitionsOfPolicy(cluster.getServiceName(),
                                                         allPartitions);

        ClusterMonitor clusterMonitor =
                                        new ClusterMonitor(cluster.getClusterId(),
                                                           cluster.getServiceName(),
                                                           deploymentPolicy, policy);
        for (PartitionGroup partitionGroup: deploymentPolicy.getPartitionGroups()){

            NetworkPartitionContext networkPartitionContext
                    = PartitionManager.getInstance().getNetworkPartition(partitionGroup.getId());

            for(Partition partition: deploymentPolicy.getAllPartitions()){
                PartitionContext partitionContext = new PartitionContext(partition);

                for (Member member: cluster.getMembers()){
                    String memberId = member.getMemberId();
                    if(member.getPartitionId().equalsIgnoreCase(partition.getId())){
                        MemberContext memberContext = new MemberContext();
                        memberContext.setClusterId(member.getClusterId());
                        memberContext.setMemberId(memberId);
                        memberContext.setPartition(partition);

                        if(MemberStatus.Activated.equals(member.getStatus())){
                            partitionContext.addActiveMember(memberContext);
                        } else if(MemberStatus.Created.equals(member.getStatus()) || MemberStatus.Starting.equals(member.getStatus())){
                            partitionContext.addPendingMember(memberContext);
                        } else if(MemberStatus.Suspended.equals(member.getStatus())){
                            partitionContext.addFaultyMember(memberId);
                        }

                        partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
                    }

                }
                networkPartitionContext.addPartitionContext(partitionContext);
            }

            clusterMonitor.addNetworkPartitionCtxt(networkPartitionContext);
        }
//        if (policy != null) {
//
//            // get values from policy
//            LoadThresholds loadThresholds = policy.getLoadThresholds();
//            float averageLimit = loadThresholds.getRequestsInFlight().getAverage();
//            float gradientLimit = loadThresholds.getRequestsInFlight().getGradient();
//            float secondDerivativeLimit = loadThresholds.getRequestsInFlight().getSecondDerivative();
//
//            clusterMonitor.setRequestsInFlightGradientThreshold(gradientLimit);
//            clusterMonitor.setRequestsInFlightSecondDerivativeThreshold(secondDerivativeLimit);
//            clusterMonitor.setAverageRequestsInFlightThreshold(averageLimit);
//
//        }

        return clusterMonitor;
    }

    private static void validateExistenceOfPartions(Partition[] partitions) throws InvalidPartitionException {
        PartitionManager partitionMgr = PartitionManager.getInstance();
        for (Partition partition : partitions) {
            String partitionId = partition.getId();
            if (partitionId == null || !partitionMgr.partitionExist(partitionId)) {
                String msg =
                             "Non existing Partition defined. Partition id: " + partitionId + ". " +
                                     "Please define the partition in the partition definition file.";
                log.error(msg);
                throw new InvalidPartitionException(msg);
            }
            fillPartition(partition, partitionMgr.getPartitionById(partitionId));
        }
    }

    private static void fillPartition(Partition destPartition, Partition srcPartition) {

        if (!destPartition.isProviderSpecified()) {
            destPartition.setProvider(srcPartition.getProvider());
        }
        if (!destPartition.isPartitionMaxSpecified()) {
            destPartition.setPartitionMax(srcPartition.getPartitionMax());
        }
        if (!destPartition.isPartitionMinSpecified()) {
            destPartition.setPartitionMin(srcPartition.getPartitionMin());
        }
        if (!destPartition.isPropertiesSpecified()) {
            destPartition.setProperties(srcPartition.getProperties());
        }
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

    public static LbClusterMonitor getLbClusterMonitor(Cluster cluster) throws PolicyValidationException, PartitionValidationException {
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

           try {
               validateExistenceOfPartions(allPartitions);
           } catch (InvalidPartitionException e) {
               String msg = "Deployment Policy is invalid. Policy name: " + deploymentPolicyName;
               log.error(msg, e);
               throw new PolicyValidationException(msg, e);
           }

           CloudControllerClient.getInstance()
                                .validatePartitionsOfPolicy(cluster.getServiceName(),
                                                            allPartitions);

           LbClusterMonitor clusterMonitor =
                                           new LbClusterMonitor(cluster.getClusterId(),
                                                              cluster.getServiceName(),
                                                              deploymentPolicy, policy);
           for (PartitionGroup partitionGroup: deploymentPolicy.getPartitionGroups()){

               NetworkPartitionContext networkPartitionContext
                       = PartitionManager.getInstance().getNetworkPartition(partitionGroup.getId());
               clusterMonitor.addNetworkPartitionCtxt(networkPartitionContext);
           }
        return null;
    }
}
