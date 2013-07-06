/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.common.packages;

/*
 * Deserialize following XML
<packages xmlns="http://wso2.com/carbon/multitenancy/billing/pacakges">
    <package name="multitenancy-free">
        <!--<subscriptionCharge>0</subscriptionCharge>--> <!-- $ per month -->
        <users>
            <limit>5</limit>
            <charge>0</charge> <!-- charge per month -->
        </users>
        <resourceVolume>
            <limit>10</limit> <!--mb per user -->
        </resourceVolume>
        <bandwidth>
            <limit>1000</limit> <!-- mb per user -->
            <overuseCharge>0</overuseCharge> <!-- $ per user per month -->
        </bandwidth>
    </package>
    <package name="multitenancy-small">
        ...
    </package>
</packages>
 */
public class PackageInfo {
	
	private String name;
	private int usersLimit;
	private int subscriptionCharge;
	private int chargePerUser;
	private int resourceVolumeLimit;
	private int bandwidthLimit;
	private int bandwidthOveruseCharge;

	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getUsersLimit() {
		return usersLimit;
	}

	public void setUsersLimit(int usersLimit) {
		this.usersLimit = usersLimit;
	}

	public int getSubscriptionCharge() {
		return subscriptionCharge;
	}

	public void setSubscriptionCharge(int subscriptionCharge) {
		this.subscriptionCharge = subscriptionCharge;
	}

	public int getChargePerUser() {
		return chargePerUser;
	}

	public void setChargePerUser(int chargePerUser) {
		this.chargePerUser = chargePerUser;
	}

	public int getResourceVolumeLimit() {
		return resourceVolumeLimit;
	}

	public void setResourceVolumeLimit(int resourceVolumeLimit) {
		this.resourceVolumeLimit = resourceVolumeLimit;
	}

	public int getBandwidthLimit() {
		return bandwidthLimit;
	}

	public void setBandwidthLimit(int bandwidthLimit) {
		this.bandwidthLimit = bandwidthLimit;
	}

	public int getBandwidthOveruseCharge() {
		return bandwidthOveruseCharge;
	}

	public void setBandwidthOveruseCharge(int bandwidthOveruseCharge) {
		this.bandwidthOveruseCharge = bandwidthOveruseCharge;
	}

    
}
