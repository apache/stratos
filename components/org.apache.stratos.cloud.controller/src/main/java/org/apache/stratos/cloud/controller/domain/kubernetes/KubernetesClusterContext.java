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
package org.apache.stratos.cloud.controller.domain.kubernetes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds information about a Kubernetes Cluster.
 *
 *
 */
public class KubernetesClusterContext implements Serializable {

	private static final long serialVersionUID = -802025758806195791L;
	private static final Log log = LogFactory.getLog(KubernetesClusterContext.class);
	
	// id of the Kubernetes cluster
    private String kubernetesClusterId;
    private int upperPort;
    private int lowerPort;
    // kubernetes master ip
    private String masterIp;
    private String masterPort;
    // available list of ports
    private List<Integer> servicePorts;
    // kubernetes client API instance
    private transient KubernetesApiClient kubApi;
    private AtomicLong serviceSeqNo;
    private AtomicLong podSeqNo;

    public KubernetesClusterContext(String id, String masterIp, String masterPort, int lowerPort, int upperPort) {
    	servicePorts = new ArrayList<Integer>();
        this.lowerPort = lowerPort;
    	this.upperPort = upperPort;
    	// Generate the ports
        generateServicePorts(lowerPort, upperPort);
    	this.kubernetesClusterId = id;
    	this.masterIp = masterIp;
    	this.masterPort = masterPort;
    	this.setKubApi(new KubernetesApiClient(getEndpoint(masterIp, masterPort)));
        this.serviceSeqNo = new AtomicLong();
        this.podSeqNo = new AtomicLong();
    	
	}
    
	private String getEndpoint(String ip, String port) {
		return "http://"+ip+":"+port+"/api/v1beta1/";
	}

	public String getKubernetesClusterId() {
		return kubernetesClusterId;
	}

	public void setKubernetesClusterId(String kubernetesClusterId) {
		this.kubernetesClusterId = kubernetesClusterId;
	}

	public List<Integer> getServicePorts() {
		return servicePorts;
	}

	public void setServicePorts(List<Integer> servicePorts) {
		this.servicePorts = servicePorts;
	}
	
	public int getNextServicePort() {
	    if (servicePorts.isEmpty()) {
	        return -1;
	    }
		return servicePorts.remove(0);
	}
	
	public void deallocatePort(int port) {
		if (!servicePorts.contains(port)) {
			servicePorts.add(port);
            // TODO Sort elements
		}
	}

	private void generateServicePorts(int lowerPort, int upperPort) {
		for (int port = lowerPort; port <= upperPort; port++) {
			servicePorts.add(port);
		}
	}

	public String getMasterIp() {
		return masterIp;
	}

	public void setMasterIp(String masterIp) {
		this.masterIp = masterIp;
	}

	public KubernetesApiClient getKubApi() {
		if (kubApi == null) {
			kubApi = new KubernetesApiClient(getEndpoint(masterIp, masterPort));
		}
		return kubApi;
	}

	public void setKubApi(KubernetesApiClient kubApi) {
		this.kubApi = kubApi;
	}

	public int getUpperPort() {
        return upperPort;
    }

    public void setUpperPort(int upperPort) {
        this.upperPort = upperPort;
    }

    public int getLowerPort() {
        return lowerPort;
    }

    public void setLowerPort(int lowerPort) {
        this.lowerPort = lowerPort;
    }

    public AtomicLong getServiceSeqNo() {
        return serviceSeqNo;
    }

    public AtomicLong getPodSeqNo() {
        return podSeqNo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((servicePorts == null) ? 0 : servicePorts.hashCode());
        result = prime * result + ((kubernetesClusterId == null) ? 0 : kubernetesClusterId.hashCode());
        result = prime * result + lowerPort;
        result = prime * result + ((masterIp == null) ? 0 : masterIp.hashCode());
        result = prime * result + ((masterPort == null) ? 0 : masterPort.hashCode());
        result = prime * result + upperPort;
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
        if (servicePorts == null) {
            if (other.servicePorts != null)
                return false;
        } else if (!servicePorts.equals(other.servicePorts))
            return false;
        if (kubernetesClusterId == null) {
            if (other.kubernetesClusterId != null)
                return false;
        } else if (!kubernetesClusterId.equals(other.kubernetesClusterId))
            return false;
        if (lowerPort != other.lowerPort)
            return false;
        if (masterIp == null) {
            if (other.masterIp != null)
                return false;
        } else if (!masterIp.equals(other.masterIp))
            return false;
        if (masterPort == null) {
            if (other.masterPort != null)
                return false;
        } else if (!masterPort.equals(other.masterPort))
            return false;
        if (upperPort != other.upperPort)
            return false;
        return true;
    }
}
