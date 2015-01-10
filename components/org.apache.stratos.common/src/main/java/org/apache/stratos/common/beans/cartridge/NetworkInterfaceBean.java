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

package org.apache.stratos.common.beans.cartridge;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement (name = "networkInterfaces")
public class NetworkInterfaceBean {

	private String networkUuid;
	private String fixedIp;
	private String portUuid;
	private List<FloatingNetworkBean> floatingNetworks;
    
    private String getFloatingNetworksString() {
    	StringBuilder sb = new StringBuilder();
    	if (floatingNetworks != null) {
    		sb.append('[');
    		String delimeter = "";
    		for (FloatingNetworkBean floatingNetworkBean:floatingNetworks) {
    			sb.append(delimeter).append(floatingNetworkBean);
    			delimeter = ", ";
    		}
    		sb.append(']');
    	}
    	return sb.toString();
    }

	public String getNetworkUuid() {
		return networkUuid;
	}

	public void setNetworkUuid(String networkUuid) {
		this.networkUuid = networkUuid;
	}

	public String getFixedIp() {
		return fixedIp;
	}

	public void setFixedIp(String fixedIp) {
		this.fixedIp = fixedIp;
	}

	public String getPortUuid() {
		return portUuid;
	}

	public void setPortUuid(String portUuid) {
		this.portUuid = portUuid;
	}

	public void setFloatingNetworks(List<FloatingNetworkBean> floatingNetworks) {
		this.floatingNetworks = floatingNetworks;
	}

	public List<FloatingNetworkBean> getFloatingNetworks() {
		return floatingNetworks;
	}

	public String toString () {
		StringBuilder sb = new StringBuilder('{');
		String delimeter = "";
		if (getNetworkUuid() != null) {
			sb.append(delimeter).append("networkUuid : ").append(getNetworkUuid());
			delimeter = ", ";
		}
		if (getFixedIp() != null) {
			sb.append(delimeter).append("fixedIp : ").append(getFixedIp());
			delimeter = ", ";
		}
		if (getPortUuid() != null) {
			sb.append(delimeter).append("portUuid : ").append(getPortUuid());
			delimeter = ", ";
		}
		if (getFloatingNetworks() != null) {
			sb.append(delimeter).append("floatingNetworks : ").append(getFloatingNetworks());
			delimeter = ", ";
		}
		sb.append('}');
		return sb.toString();
	}
}