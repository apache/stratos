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

package org.apache.stratos.common.beans.application;

import org.apache.stratos.common.beans.cartridge.CartridgeReferenceBean;

import javax.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.util.List;

@XmlRootElement(name = "groups")
public class GroupReferenceBean implements Serializable {

    private static final long serialVersionUID = 7261380706841894892L;

	private String name;
    private String alias;
    private int groupMinInstances;
    private int groupMaxInstances;
    private boolean isGroupScalingEnabled;
    private String deploymentPolicy;
    private List<CartridgeReferenceBean> cartridges;
    private List<GroupReferenceBean> groups;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getGroupMinInstances() {
        return groupMinInstances;
    }

    public void setGroupMinInstances(int groupMinInstances) {
        this.groupMinInstances = groupMinInstances;
    }

    public int getGroupMaxInstances() {
        return groupMaxInstances;
    }

    public void setGroupMaxInstances(int groupMaxInstances) {
        this.groupMaxInstances = groupMaxInstances;
    }

	public boolean isGroupScalingEnabled() {
		return isGroupScalingEnabled;
	}

	public void setGroupScalingEnabled(boolean isGroupScalingEnabled) {
		this.isGroupScalingEnabled = isGroupScalingEnabled;
	}

	public List<CartridgeReferenceBean> getCartridges() {
		return cartridges;
	}

	public void setCartridges(List<CartridgeReferenceBean> cartridges) {
		this.cartridges = cartridges;
	}

	public List<GroupReferenceBean> getGroups() {
		return groups;
	}

	public void setGroups(List<GroupReferenceBean> groups) {
		this.groups = groups;
	}

	public String getDeploymentPolicy() {
		return deploymentPolicy;
	}

	public void setDeploymentPolicy(String deploymentPolicy) {
		this.deploymentPolicy = deploymentPolicy;
	}
}
