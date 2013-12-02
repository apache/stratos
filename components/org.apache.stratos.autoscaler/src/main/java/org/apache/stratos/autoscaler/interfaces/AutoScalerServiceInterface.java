package org.apache.stratos.autoscaler.interfaces;

import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;

public interface AutoScalerServiceInterface {
	public Partition[] getAllAvailablePartitions();
	
	public DeploymentPolicy[] getAllDeploymentPolicies();
	
	public AutoscalePolicy[] getAllAutoScalingPolicy();
}
