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

import org.apache.stratos.common.beans.PropertyBean;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ApplicationBean implements Serializable {

    private static final long serialVersionUID = -2829206180707597651L;

	private String applicationId;
    private boolean multiTenant;
    private String name;
    private String description;
    private String alias;
    private String status;
    private ComponentBean components;
    private List<PropertyBean> property;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public boolean isMultiTenant() {
        return multiTenant;
    }

    public void setMultiTenant(boolean multiTenant) {
        this.multiTenant = multiTenant;
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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public ComponentBean getComponents() {
        return components;
    }

    public void setComponents(ComponentBean components) {
        this.components = components;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

	public List<PropertyBean> getProperty() {
		return property;
	}

	public void setProperty(List<PropertyBean> property) {
		this.property = property;
	}
}
