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
 * "AS IS" BASIS, WITHOUT WARRANTIES     OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.domain;

import org.apache.stratos.common.Properties;

import java.io.Serializable;

/**
 * This is keep the partition information
 */
public class Partition implements Serializable {

    private static final long serialVersionUID = 3725971287992010720L;
    /**
     * provider should match with an IaasProvider type.
     */
    private String provider;
    private String id;
    private String description;
    private boolean isPublic;
    private String kubernetesClusterId;
    private Properties properties = new Properties();

    /**
     * Gets the value of the id property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param id
     *     allowed object is
     *     {@link String }
     *
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the value of the description property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     *
     * @param description
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the value of the isPublic property.
     *
     * @param isPublic
     *     allowed object is boolean
     *
     */
    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * Gets the value of the isPublic property.
     *
     */
    public boolean getIsPublic() {
        return isPublic;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String toString() {
        return "Partition [id=" + id + ", description=" + description + ", isPublic=" + isPublic
                + ", provider=" + provider + ", properties=" + properties + "]";
    }

    public boolean equals(Object obj) {
        if(obj != null && obj instanceof Partition) {
            return this.id.equals(((Partition) obj).getId());
        }
        return false;

    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public String getKubernetesClusterId() {
        return kubernetesClusterId;
    }

    public void setKubernetesClusterId(String kubernetesClusterId) {
        this.kubernetesClusterId = kubernetesClusterId;
    }
}
