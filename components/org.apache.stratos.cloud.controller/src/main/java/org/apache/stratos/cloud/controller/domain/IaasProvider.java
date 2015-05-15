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
package org.apache.stratos.cloud.controller.domain;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Template;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the basic data structure which holds an IaaS specific details.
 * NOTE: If you add a new attribute, please assign it in the constructor too.
 */
public class IaasProvider implements Serializable {

    private static final long serialVersionUID = -940288190885166118L;

    /**
     * Type of the IaasProvider.
     */
    private String type;


    /**
     * Fully qualified class name of an implementation of {@link org.apache.stratos.cloud.controller.iaases.Iaas}
     */
    private String className;

    /**
     * human description of this IaaS provider
     */
    private String name;

    /**
     * Property map of this IaaS provider.
     */
    private Map<String, String> properties = new HashMap<String, String>();

    /**
     * Network Interfaces Configuration
     */
    private NetworkInterface[] networkInterfaces;

    /**
     * Image identifier.
     */
    private String image;

    private String provider, identity, credential;

    private transient ComputeService computeService;

    private transient Template template;

    private byte[] payload;

    /**
     * Corresponding {@link org.apache.stratos.cloud.controller.iaases.Iaas} implementation
     */
    private transient Iaas iaas;

    public IaasProvider() {
    }

    public IaasProvider(IaasProvider anIaasProvider) {
        this.type = anIaasProvider.getType();
        this.name = anIaasProvider.getName();
        this.className = anIaasProvider.getClassName();
        this.computeService = anIaasProvider.getComputeService();
        this.properties = new HashMap<String, String>(anIaasProvider.getProperties());
        this.networkInterfaces = anIaasProvider.getNetworkInterfaces();
        this.image = anIaasProvider.getImage();
        this.provider = anIaasProvider.getProvider();
        this.identity = anIaasProvider.getIdentity();
        this.credential = anIaasProvider.getCredential();
        this.payload = anIaasProvider.getPayload();
    }

    public String getType() {
        return type;
    }

    public void setType(String id) {
        this.type = id;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void addProperty(String key, String val) {
        if (key != null && val != null) {
            properties.put(key, val);
        }
    }

    public void setProperty(String key, String value) {

        if (key != null && value != null) {
            properties.put(key, value);
        }
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public ComputeService getComputeService() {
        return computeService;
    }

    public void setComputeService(ComputeService computeService) {
        this.computeService = computeService;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }


    public boolean equals(Object o) {
        if (o instanceof IaasProvider) {
            return ((IaasProvider) o).getType().equals(this.getType());
        }

        return false;
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
                append(type).
                toHashCode();
    }

    public IaasProvider copy() {
        return new IaasProvider(this);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Iaas getIaas() {
        if (iaas == null) {
            synchronized (IaasProvider.this) {
                if (iaas == null) {
                    try {
                        iaas = CloudControllerUtil.createIaasInstance(this);
                        iaas.initialize();
                    } catch (InvalidIaasProviderException e) {
                        throw new RuntimeException("Could not create IaaS instance", e);
                    }
                }
            }
        }
        return iaas;
    }


    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = ArrayUtils.clone(payload);
    }

    /**
     * @return the networkInterfaces
     */
    public NetworkInterface[] getNetworkInterfaces() {
        return networkInterfaces;
    }

    /**
     * @param networkInterfaces the networkInterfaces to set
     */
    public void setNetworkInterfaces(NetworkInterface[] networkInterfaces) {
        this.networkInterfaces = ArrayUtils.clone(networkInterfaces);
    }

    @Override
    public String toString() {
        return "IaasProvider [type=" + type + ", name=" + name + ", image=" + image +
                ", provider=" + provider + "]";
    }
}
