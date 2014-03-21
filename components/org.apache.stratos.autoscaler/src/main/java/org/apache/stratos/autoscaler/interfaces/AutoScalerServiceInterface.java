package org.apache.stratos.autoscaler.interfaces;

import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.InvalidPartitionException;
import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
import org.apache.stratos.autoscaler.exception.NonExistingLBException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;

public interface AutoScalerServiceInterface {
	
	public Partition[] getAllAvailablePartitions();
	public boolean addPartition(Partition partition) throws InvalidPartitionException;
	
	public DeploymentPolicy[] getAllDeploymentPolicies();
	public boolean addDeploymentPolicy(DeploymentPolicy depPolicy) throws InvalidPolicyException;
	
	public AutoscalePolicy[] getAllAutoScalingPolicy();
	public boolean addAutoScalingPolicy(AutoscalePolicy aspolicy) throws InvalidPolicyException;
	
	public DeploymentPolicy[] getValidDeploymentPoliciesforCartridge(String cartridgeType);
	
	public Partition getPartition (String partitionId);
	public DeploymentPolicy getDeploymentPolicy (String deploymentPolicyId);
	public AutoscalePolicy getAutoscalingPolicy (String autoscalingPolicyId);
	public PartitionGroup[] getPartitionGroups (String deploymentPolicyId);	
	public Partition[] getPartitionsOfGroup(String deploymentPolicyId, String partitionGroup);
	public Partition[] getPartitionsOfDeploymentPolicy(String deploymentPolicyId);
	
	/**
	 * Check existence of a lb cluster in network partitions of a given policy.
	 * @param lbClusterId 
	 * @param deploymentPolicyId
	 * @throws NonExistingLBException if the lb cluster id cannot be found in any of the network partitions.
	 */
	public void checkLBExistenceAgainstPolicy(String lbClusterId, String deploymentPolicyId) throws NonExistingLBException;
	
	/**
	 * Check the existence of default lb in network partitions of a given policy.
	 * @param deploymentPolicyId 
	 * @return true if the LB exists in all the network partitions of this policy,
	 * false if a LB couldn't find even in one network partition.
	 */
	public boolean checkDefaultLBExistenceAgainstPolicy(String deploymentPolicyId);
	
	/**
	 * Check the existence of per cluster lb in network partitions of a given policy.
	 * @param clusterId cluster id of the service cluster which requires a dedicated LB.
	 * @param deploymentPolicyId 
	 * @return true if the cluster based LB exists in all the network partitions of this policy,
     * false if a LB couldn't find even in one network partition.
	 */
	public boolean checkClusterLBExistenceAgainstPolicy(String clusterId, String deploymentPolicyId);
	
	/**
     * Check the existence of per service lb in network partitions of a given policy.
     * @param serviceName service name of the service cluster which requires a dedicated LB.
     * @param deploymentPolicyId 
     * @return true if the service based LB exists in all the network partitions of this policy,
     * false if a LB couldn't find even in one network partition.
     */
	public boolean checkServiceLBExistenceAgainstPolicy(String serviceName, String deploymentPolicyId);
}
