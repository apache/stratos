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
package org.apache.stratos.manager.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class Policy implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	private String description;
	private boolean defaultPolicy;

	private Integer minAppInstances;
	private Integer maxAppInstances;
	private Integer maxRequestsPerSecond;
	private BigDecimal alarmingUpperRate;
	private BigDecimal alarmingLowerRate;
	private BigDecimal scaleDownFactor;
	private Integer roundsToAverage;

	public Policy() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isDefaultPolicy() {
		return defaultPolicy;
	}

	public void setDefaultPolicy(boolean defaultPolicy) {
		this.defaultPolicy = defaultPolicy;
	}

	public Integer getMinAppInstances() {
		return minAppInstances;
	}

	public void setMinAppInstances(Integer minAppInstances) {
		this.minAppInstances = minAppInstances;
	}

	public Integer getMaxAppInstances() {
		return maxAppInstances;
	}

	public void setMaxAppInstances(Integer maxAppInstances) {
		this.maxAppInstances = maxAppInstances;
	}

	public Integer getMaxRequestsPerSecond() {
		return maxRequestsPerSecond;
	}

	public void setMaxRequestsPerSecond(Integer maxRequestsPerSecond) {
		this.maxRequestsPerSecond = maxRequestsPerSecond;
	}

	public BigDecimal getAlarmingUpperRate() {
		return alarmingUpperRate;
	}

	public void setAlarmingUpperRate(BigDecimal alarmingUpperRate) {
		this.alarmingUpperRate = alarmingUpperRate;
	}

	public BigDecimal getAlarmingLowerRate() {
		return alarmingLowerRate;
	}

	public void setAlarmingLowerRate(BigDecimal alarmingLowerRate) {
		this.alarmingLowerRate = alarmingLowerRate;
	}

	public BigDecimal getScaleDownFactor() {
		return scaleDownFactor;
	}

	public void setScaleDownFactor(BigDecimal scaleDownFactor) {
		this.scaleDownFactor = scaleDownFactor;
	}

	public Integer getRoundsToAverage() {
		return roundsToAverage;
	}

	public void setRoundsToAverage(Integer roundsToAverage) {
		this.roundsToAverage = roundsToAverage;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Policy))
			return false;
		Policy other = (Policy) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
