package org.apache.stratos.autoscaler.interfaces;

import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
import org.apache.stratos.autoscaler.exception.NonExistingLBException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;

public interface AutoScalerServiceInterface {
	
	public Partition[] getAllAvailablePartitions();
	public boolean addPartition(Partition partition);
	
	public DeploymentPolicy[] getAllDeploymentPolicies();
	public boolean addDeploymentPolicy(DeploymentPolicy depPolicy) throws InvalidPolicyException;
	
	public AutoscalePolicy[] getAllAutoScalingPolicy();
	public boolean addAutoScalingPolicy(AutoscalePolicy aspolicy) throws InvalidPolicyException;
	
	public DeploymentPolicy[] getValidDeploymentPoliciesforCartridge(String cartridgeType) throws  PartitionValidationException;
	
	public Partition getPartition (String partitionId);
	public DeploymentPolicy getDeploymentPolicy (String deploymentPolicyId);
	public AutoscalePolicy getAutoscalingPolicy (String autoscalingPolicyId);
	public PartitionGroup[] getPartitionGroups (String deploymentPolicyId);	
	public Partition[] getPartitionsOfGroup(String deploymentPolicyId, String partitionGroup);
	public Partition[] getPartitionsOfDeploymentPolicy(String deploymentPolicyId);
	
	public void checkLBExistence(String clusterId) throws NonExistingLBException;
}
