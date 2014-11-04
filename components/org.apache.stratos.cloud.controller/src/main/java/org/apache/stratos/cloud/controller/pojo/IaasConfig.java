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

package org.apache.stratos.cloud.controller.pojo;

import java.io.Serializable;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;

/**
 * This class is used to support <link>CartridgeConfig</link>
 * class for the Rest API
 */
public class IaasConfig implements Serializable {


    private static final long serialVersionUID = 3329046207651171707L;

    private String type;
   
    private String className;
   
    private String name;
   
    private String provider, identity, credential;
   
    private String imageId;

    private int maxInstanceLimit;

    private Properties properties;
    
    private NetworkInterfaces networkInterfaces;
    
    private byte[] payload;

    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
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
    
    public byte[] getPayload() {
        return payload;
    }
    
    public void setPayload(byte[] payload) {
        this.payload = ArrayUtils.clone(payload);
    }
    
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public int getMaxInstanceLimit() {
        return maxInstanceLimit;
    }

    public void setMaxInstanceLimit(int maxInstanceLimit) {
        this.maxInstanceLimit = maxInstanceLimit;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String toString () {

        return " [ Type: " + type + ", Name: " + name + ", Class Name: " + className + ", Image Id: " + imageId +
                ", Max Instance Limit: " + maxInstanceLimit + ", Provider: " + provider + ", Identity: " + identity +
                ", Credentials: " + credential + ", Properties: " + getIaasProperties() + " ] ";
    }

    private String getIaasProperties () {

        StringBuilder iaasPropertyBuilder = new StringBuilder();
        if (properties != null) {
            Property[] propertyArray = properties.getProperties();
            if(propertyArray.length > 0) {
                for (Property property : propertyArray) {
                    iaasPropertyBuilder.append(property.toString() + " | ");
                }
            }
        }
        return iaasPropertyBuilder.toString();
    }

    /**
     * @return the networkInterfaces
     */
    public NetworkInterfaces getNetworkInterfaces() {
        return networkInterfaces;
    }

    /**
     * @param networkInterfaces the networkInterfaces to set
     */
    public void setNetworkInterfaces(NetworkInterfaces networkInterfaces) {
        this.networkInterfaces = networkInterfaces;
    }

}
