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

package org.apache.stratos.autoscaler.applications.pojo;

import org.apache.stratos.common.Properties;

import java.io.Serializable;

public class SubscribableInfoContext implements Serializable {

    private static final long serialVersionUID = -6874644941002783034L;

    private String alias;
    private String deploymentPolicy;
    private String autoscalingPolicy;
    private int minMembers;
    private int maxMembers;
    private String[] dependencyAliases;
    private ArtifactRepositoryContext artifactRepositoryContext;
    private Properties properties;
    private PersistenceContext persistenceContext;
	//This is the virtual IP that we need to pass for use LVS as Load Balancer
	private String lvsVirtualIP;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(String deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public String getAutoscalingPolicy() {
        return autoscalingPolicy;
    }

    public void setAutoscalingPolicy(String autoscalingPolicy) {
        this.autoscalingPolicy = autoscalingPolicy;
    }

    public String[] getDependencyAliases() {
        return dependencyAliases;
    }

    public void setDependencyAliases(String[] dependencyAliases) {
        this.dependencyAliases = dependencyAliases;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public int getMinMembers() {
        return minMembers;
    }

    public void setMinMembers(int minMembers) {
        this.minMembers = minMembers;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public ArtifactRepositoryContext getArtifactRepositoryContext() {
        return artifactRepositoryContext;
    }

    public void setArtifactRepositoryContext(ArtifactRepositoryContext artifactRepositoryContext) {
        this.artifactRepositoryContext = artifactRepositoryContext;
    }

    public PersistenceContext getPersistenceContext() {
        return persistenceContext;
    }

    public void setPersistenceContext(PersistenceContext persistenceContext) {
        this.persistenceContext = persistenceContext;
    }

	public String getLvsVirtualIP() {
		return lvsVirtualIP;
	}

	public void setLvsVirtualIP(String lvsVirtualIP) {
		this.lvsVirtualIP = lvsVirtualIP;
	}
}
