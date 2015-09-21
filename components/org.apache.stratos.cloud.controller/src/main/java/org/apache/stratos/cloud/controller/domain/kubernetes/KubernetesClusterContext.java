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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds information about a Kubernetes Cluster.
 */
public class KubernetesClusterContext implements Serializable {

    private static final long serialVersionUID = -802025758806195791L;

    private static final Log log = LogFactory.getLog(KubernetesClusterContext.class);

    private String kubernetesClusterId;
    private int upperPort;
    private int lowerPort;
    private String masterIp;
    private String masterPort;
    private List<Integer> servicePortSequence;

    private AtomicLong serviceSeqNo;
    private AtomicLong podSeqNo;
    private transient KubernetesApiClient kubApi;
    public static final long MAX_POD_ID = 99999999999999L;
    public static final long MAX_SERVICE_ID = 99999999999999L;

    public KubernetesClusterContext(String id, String masterIp, String masterPort, int lowerPort, int upperPort) {
        this.servicePortSequence = new ArrayList<>();
        serviceSeqNo = new AtomicLong(0);
        podSeqNo = new AtomicLong(0);

        this.lowerPort = lowerPort;
        this.upperPort = upperPort;
        // Generate the ports
        initializeServicePortSequence(lowerPort, upperPort);
        this.kubernetesClusterId = id;
        this.masterIp = masterIp;
        this.masterPort = masterPort;
        this.setKubApi(new KubernetesApiClient(getEndpoint(masterIp, masterPort)));
    }

    private String getEndpoint(String ip, String port) {
        return "http://" + ip + ":" + port + "/api/v1beta1/";
    }

    public String getKubernetesClusterId() {
        return kubernetesClusterId;
    }

    public void setKubernetesClusterId(String kubernetesClusterId) {
        this.kubernetesClusterId = kubernetesClusterId;
    }

    public List<Integer> getServicePorts() {
        return servicePortSequence;
    }

    public void setServicePorts(List<Integer> servicePorts) {
        this.servicePortSequence = servicePorts;
    }

    /***
     * Get next available service port.
     *
     * @return
     */
    public int getNextServicePort() {
        if (servicePortSequence.isEmpty()) {
            return -1;
        }
        return servicePortSequence.remove(0);
    }

    /**
     * Deallocate a service port by adding it again to the sequence.
     *
     * @param port
     */
    public void deallocatePort(int port) {
        if (!servicePortSequence.contains(port)) {
            servicePortSequence.add(port);
            Collections.sort(servicePortSequence);
        }
    }

    /**
     * Initialize service port sequence according to the given port range.
     *
     * @param lowerPort
     * @param upperPort
     */
    private void initializeServicePortSequence(int lowerPort, int upperPort) {
        for (int port = lowerPort; port <= upperPort; port++) {
            servicePortSequence.add(port);
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

    public long getNextServiceSeqNo() {
        // reset before we hit the max character length for Kub service id
        if (serviceSeqNo.get() > MAX_SERVICE_ID) {
            serviceSeqNo.set(0);
        }
        return serviceSeqNo.incrementAndGet();
    }

    public long getNextPodSeqNo() {
        // reset before we hit the max character length for Kub pod id
        if (podSeqNo.get() > MAX_POD_ID) {
            podSeqNo.set(0);
        }
        return podSeqNo.incrementAndGet();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((servicePortSequence == null) ? 0 : servicePortSequence.hashCode());
        result = prime * result + ((kubernetesClusterId == null) ? 0 : kubernetesClusterId.hashCode());
        result = prime * result + lowerPort;
        result = prime * result + ((masterIp == null) ? 0 : masterIp.hashCode());
        result = prime * result + ((masterPort == null) ? 0 : masterPort.hashCode());
        result = prime * result + upperPort;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        KubernetesClusterContext other = (KubernetesClusterContext) obj;
        if (servicePortSequence == null) {
            if (other.servicePortSequence != null) {
                return false;
            }
        } else if (!servicePortSequence.equals(other.servicePortSequence)) {
            return false;
        }
        if (kubernetesClusterId == null) {
            if (other.kubernetesClusterId != null) {
                return false;
            }
        } else if (!kubernetesClusterId.equals(other.kubernetesClusterId)) {
            return false;
        }
        if (lowerPort != other.lowerPort) {
            return false;
        }
        if (masterIp == null) {
            if (other.masterIp != null) {
                return false;
            }
        } else if (!masterIp.equals(other.masterIp)) {
            return false;
        }
        if (masterPort == null) {
            if (other.masterPort != null) {
                return false;
            }
        } else if (!masterPort.equals(other.masterPort)) {
            return false;
        }
        if (upperPort != other.upperPort) {
            return false;
        }
        return true;
    }
}