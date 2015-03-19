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
import org.apache.stratos.common.beans.artifact.repository.ArtifactRepositoryBean;
import org.apache.stratos.common.beans.cartridge.PersistenceBean;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "subscribableInfo")
public class SubscribableInfo implements Serializable {

    private static final long serialVersionUID = 8097432440778125606L;

	private String alias;
    private String deploymentPolicy;
    private String autoscalingPolicy;
    private int maxMembers;
    private int minMembers;
    private String[] dependencyAliases;
    private ArtifactRepositoryBean artifactRepository;
    private List<PropertyBean> property;
    private PersistenceBean persistence;

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
    
    public List<PropertyBean> getProperty() {
        return property;
    }

    public void setProperty(List<PropertyBean> property) {
        this.property = property;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public int getMinMembers() {
        return minMembers;
    }

    public void setMinMembers(int minMembers) {
        this.minMembers = minMembers;
    }

    public ArtifactRepositoryBean getArtifactRepository() {
        return artifactRepository;
    }

    public void setArtifactRepository(ArtifactRepositoryBean artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    public PersistenceBean getPersistence() {
        return persistence;
    }

    public void setPersistence(PersistenceBean persistence) {
        this.persistence = persistence;
    }
}
