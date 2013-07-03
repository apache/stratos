/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.cartridge.messages;

public class ClusterDomain {

	private String domain;
	private String subDomain;
	private String hostName;
	//private int tenantId;
	private String tenantRange;
	private int minInstances;
	private int maxInstances;
	private String serviceName;
	private int maxRequestsPerSecond;
	private int roundsToAverage;
	private double alarmingUpperRate;
	private double alarmingLowerRate;
	private double scaleDownFactor;
	
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getSubDomain() {
		return subDomain;
	}
	public void setSubDomain(String subDomain) {
		this.subDomain = subDomain;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	/*public int getTenantId() {
		return tenantId;
	}
	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}*/
	public int getMinInstances() {
		return minInstances;
	}
	public void setMinInstances(int minInstances) {
		this.minInstances = minInstances;
	}
	public int getMaxInstances() {
		return maxInstances;
	}
	public void setMaxInstances(int maxInstances) {
		this.maxInstances = maxInstances;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getTenantRange() {
    	return tenantRange;
    }
	public void setTenantRange(String tenantRange) {
    	this.tenantRange = tenantRange;
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
	
	
}
