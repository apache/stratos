/*
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the
 *"License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.apache.stratos.usage.ui.report;

public class AllTenantUsageData {

	public AllTenantUsageData() {
	}

	String yearMonth;
	
	String tenantName;
	
	String numberOfUsers;
	
	String currentDataStorage;
	
	String regBandwidth;
	
	String svcBandwidth;
	
	String svcTotalRequest;

	public String getYearMonth() {
		return yearMonth;
	}

	public void setYearMonth(String yearMonth) {
		this.yearMonth = yearMonth;
	}

	public String getTenantName() {
		return tenantName;
	}

	public void setTenantName(String tenantName) {
		this.tenantName = tenantName;
	}

	public String getNumberOfUsers() {
		return numberOfUsers;
	}

	public void setNumberOfUsers(String numberOfUsers) {
		this.numberOfUsers = numberOfUsers;
	}

	public String getCurrentDataStorage() {
		return currentDataStorage;
	}

	public void setCurrentDataStorage(String currentDataStorage) {
		this.currentDataStorage = currentDataStorage;
	}

	public String getRegBandwidth() {
		return regBandwidth;
	}

	public void setRegBandwidth(String regBandwidth) {
		this.regBandwidth = regBandwidth;
	}

	public String getSvcBandwidth() {
		return svcBandwidth;
	}

	public void setSvcBandwidth(String svcBandwidth) {
		this.svcBandwidth = svcBandwidth;
	}

	public String getSvcTotalRequest() {
		return svcTotalRequest;
	}

	public void setSvcTotalRequest(String svcTotalRequest) {
		this.svcTotalRequest = svcTotalRequest;
	}

}
