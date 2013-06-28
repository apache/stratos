/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.dao;

public class DataCartridge {

	private int id;
	private String dataCartridgeType;
	private String userName;
	private String password;
	public int getId() {
    	return id;
    }
	public void setId(int id) {
    	this.id = id;
    }
	public String getDataCartridgeType() {
    	return dataCartridgeType;
    }
	public void setDataCartridgeType(String dataCartridgeType) {
    	this.dataCartridgeType = dataCartridgeType;
    }
	public String getUserName() {
    	return userName;
    }
	public void setUserName(String userName) {
    	this.userName = userName;
    }
	public String getPassword() {
    	return password;
    }
	public void setPassword(String password) {
    	this.password = password;
    }
}
