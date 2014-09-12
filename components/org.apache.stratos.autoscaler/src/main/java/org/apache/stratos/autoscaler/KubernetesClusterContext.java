package org.apache.stratos.autoscaler;

import java.io.Serializable;

public class KubernetesClusterContext implements Serializable{
	
	private static final long serialVersionUID = 808741789615481596L;
	String kubernetesClusterID;
	
	public KubernetesClusterContext(String kubernetesClusterID){
		this.kubernetesClusterID = kubernetesClusterID;
	}
	
	public String getKubernetesClusterID() {
		return kubernetesClusterID;
	}
	public void setKubernetesClusterID(String kubernetesClusterID) {
		this.kubernetesClusterID = kubernetesClusterID;
	}
}
