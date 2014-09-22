package org.apache.stratos.autoscaler.monitor;

import org.apache.stratos.autoscaler.KubernetesClusterContext;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.common.enums.ClusterType;

public abstract class ContainerClusterMonitor extends AbstractClusterMonitor {

	private KubernetesClusterContext kubernetesClusterCtxt;
	protected AutoscalePolicy autoscalePolicy;
	
	protected ContainerClusterMonitor(String clusterId, String serviceId, ClusterType clusterType, 
			KubernetesClusterContext kubernetesClusterContext,
			AutoscalerRuleEvaluator autoscalerRuleEvaluator, AutoscalePolicy autoscalePolicy){
		
		super(clusterId, serviceId, clusterType, autoscalerRuleEvaluator);
		this.kubernetesClusterCtxt = kubernetesClusterContext;
		this.autoscalePolicy = autoscalePolicy;
	}
    
	public KubernetesClusterContext getKubernetesClusterCtxt() {
		return kubernetesClusterCtxt;
	}

	public void setKubernetesClusterCtxt(
			KubernetesClusterContext kubernetesClusterCtxt) {
		this.kubernetesClusterCtxt = kubernetesClusterCtxt;
	}
	
	public AutoscalePolicy getAutoscalePolicy() {
		return autoscalePolicy;
	}

	public void setAutoscalePolicy(AutoscalePolicy autoscalePolicy) {
		this.autoscalePolicy = autoscalePolicy;
	}
}
