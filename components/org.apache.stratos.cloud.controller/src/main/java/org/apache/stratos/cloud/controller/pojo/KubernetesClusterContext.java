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
package org.apache.stratos.cloud.controller.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;

/**
 * Holds information about a Kubernetes Cluster.
 * @author nirmal
 *
 */
public class KubernetesClusterContext implements Serializable {

	private static final long serialVersionUID = -802025758806195791L;
	private static final Log log = LogFactory.getLog(KubernetesClusterContext.class);
	
	// id of the Kubernetes cluster
    private String kubernetesClusterId;
    // available host port range, delimited by a hyphen
    private String hostPortRange;
    // kubernetes master ip
    private String masterIp;
    // available list of ports
    private List<Integer> availableHostPorts;
    // kubernetes client API instance
    private transient KubernetesApiClient kubApi;
    
    public KubernetesClusterContext(String id, String portRange, String masterIp) {
    	availableHostPorts = new ArrayList<Integer>();
    	this.kubernetesClusterId = id;
    	this.hostPortRange = portRange;
    	this.masterIp = masterIp;
    	this.setKubApi(new KubernetesApiClient(getEndpoint(masterIp)));
    	
	}
    
	private String getEndpoint(String ip) {
		return "http://"+ip+":8080/api/v1beta1/";
	}

	public String getKubernetesClusterId() {
		return kubernetesClusterId;
	}
	public void setKubernetesClusterId(String kubernetesClusterId) {
		this.kubernetesClusterId = kubernetesClusterId;
	}

	public String getHostPortRange() {
		return hostPortRange;
	}

	public void setHostPortRange(String hostPortRange) {
		this.hostPortRange = hostPortRange;
	}

	public List<Integer> getAvailableHostPorts() {
		return availableHostPorts;
	}

	public void setAvailableHostPorts(List<Integer> availableHostPorts) {
		this.availableHostPorts = availableHostPorts;
	}
	
	private int[] portBoundaries() {
		String[] portStrings = hostPortRange.split("-");
		int[] portInts = new int[2];
		portInts[0] = Integer.parseInt(portStrings[0]);
		portInts[1] = Integer.parseInt(portStrings[1]);
		return portInts;
	}
	
	public int getAnAvailableHostPort() {
		int[] ports = {4000, 5000};
		if (availableHostPorts.isEmpty()) {
			try {

				ports = portBoundaries();
			} catch (Exception ignore) {
				// on an exception, we use the default range
				log.warn("Unable to find a port range, hence using the default. [4000-5000]"
						+ " Exception");
			}

			// populate the ports
			populatePorts(ports[0], ports[1]);
		}
		
		return availableHostPorts.remove(0);
	}

	private void populatePorts(int i, int j) {

		for (int x = i; x < j; x++) {
			availableHostPorts.add(x);
		}
	}

	public String getMasterIp() {
		return masterIp;
	}

	public void setMasterIp(String masterIp) {
		this.masterIp = masterIp;
	}

	public KubernetesApiClient getKubApi() {
		return kubApi;
	}

	public void setKubApi(KubernetesApiClient kubApi) {
		this.kubApi = kubApi;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((hostPortRange == null) ? 0 : hostPortRange.hashCode());
		result = prime
				* result
				+ ((kubernetesClusterId == null) ? 0 : kubernetesClusterId
						.hashCode());
		result = prime * result
				+ ((masterIp == null) ? 0 : masterIp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KubernetesClusterContext other = (KubernetesClusterContext) obj;
		if (hostPortRange == null) {
			if (other.hostPortRange != null)
				return false;
		} else if (!hostPortRange.equals(other.hostPortRange))
			return false;
		if (kubernetesClusterId == null) {
			if (other.kubernetesClusterId != null)
				return false;
		} else if (!kubernetesClusterId.equals(other.kubernetesClusterId))
			return false;
		if (masterIp == null) {
			if (other.masterIp != null)
				return false;
		} else if (!masterIp.equals(other.masterIp))
			return false;
		return true;
	}

}
