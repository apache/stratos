/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.cartridge.agent.registrant;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;

import java.io.Serializable;
import java.util.Arrays;

/**
 * This class represents a process which registers itself with this Cartridge Agent
 */
@SuppressWarnings("unused")
public class Registrant implements Serializable {

    private static final long serialVersionUID = 1026289225178520964L;

    private String key;
    //private int tenantId;
    private String tenantRange;
    private String service;
    private String hostName;
    private PortMapping[] portMappings;
    private String remoteHost;
    private transient ClusteringAgent clusteringAgent;
    private int minInstanceCount;
    private int maxInstanceCount;
    private int maxRequestsPerSecond;
	private int roundsToAverage;
	private double alarmingUpperRate;
	private double alarmingLowerRate;
	private double scaleDownFactor;
   

	public String getService() {
        return service;
    }

    public String getTenantRange() {
    	return tenantRange;
    }

	public void setTenantRange(String tenantRange) {
    	this.tenantRange = tenantRange;
    }

	public void setService(String service) {
        this.service = service;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public PortMapping[] getPortMappings() {
        return Arrays.copyOf(portMappings, portMappings.length);
    }

    public void setPortMappings(PortMapping[] portMappings) {
        this.portMappings = Arrays.copyOf(portMappings, portMappings.length);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }    
    
    public int getMinInstanceCount() {
		return minInstanceCount;
	}

	public void setMinInstanceCount(int minInstanceCount) {
		this.minInstanceCount = minInstanceCount;
	}

	public int getMaxInstanceCount() {
		return maxInstanceCount;
	}

	public void setMaxInstanceCount(int maxInstanceCount) {
		this.maxInstanceCount = maxInstanceCount;
	}

	public String retrieveClusterDomain() {
		// alias.hostname.php.domain
		return hostName+"."+service+".domain";
    }

    public void start(ClusteringAgent clusteringAgent) throws ClusteringFault {
        this.clusteringAgent = clusteringAgent;
        clusteringAgent.init();
    }

    public void stop() {
        if (clusteringAgent != null) {
            clusteringAgent.stop();
        }
        clusteringAgent = null;
    }

    public boolean running() {
        return clusteringAgent != null;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }    
    
    public int getMaxRequestsPerSecond() {
    	return maxRequestsPerSecond;
    }

	public void setMaxRequestsPerSecond(int maxRequestsPerSecond) {
    	this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

	public int getRoundsToAverage() {
    	return roundsToAverage;
    }

	public void setRoundsToAverage(int roundsToAverage) {
    	this.roundsToAverage = roundsToAverage;
    }

	public double getAlarmingUpperRate() {
    	return alarmingUpperRate;
    }

	public void setAlarmingUpperRate(double alarmingUpperRate) {
    	this.alarmingUpperRate = alarmingUpperRate;
    }

	public double getAlarmingLowerRate() {
    	return alarmingLowerRate;
    }

	public void setAlarmingLowerRate(double alarmingLowerRate) {
    	this.alarmingLowerRate = alarmingLowerRate;
    }

	public double getScaleDownFactor() {
    	return scaleDownFactor;
    }

	public void setScaleDownFactor(double scaleDownFactor) {
    	this.scaleDownFactor = scaleDownFactor;
    }

	@Override
    public String toString() {
        return "Registrant{" +
               "key='" + key + '\'' +
               ", remoteHost='" + remoteHost + '\'' +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Registrant that = (Registrant) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
