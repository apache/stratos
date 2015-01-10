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

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement (name = "floatingNetworks")
public class FloatingNetworkBean {

	private String name;
	private String networkUuid;
	private String floatingIP;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNetworkUuid() {
		return networkUuid;
	}

	public void setNetworkUuid(String networkUuid) {
		this.networkUuid = networkUuid;
	}

	public String getFloatingIP() {
		return floatingIP;
	}

	public void setFloatingIP(String floatingIP) {
		this.floatingIP = floatingIP;
	}

	public String toString () {
		StringBuilder sb = new StringBuilder('{');
		String delimeter = "";
		if (getName() != null) {
			sb.append(delimeter).append("name : ").append(getName());
			delimeter = ", ";
		}
		if (getNetworkUuid() != null) {
			sb.append(delimeter).append("networkUuid : ").append(getNetworkUuid());
			delimeter = ", ";
		}
		if (getFloatingIP() != null) {
			sb.append(delimeter).append("floatingIP : ").append(getFloatingIP());
			delimeter = ", ";
		}
		sb.append('}');
		return sb.toString();
	}
}