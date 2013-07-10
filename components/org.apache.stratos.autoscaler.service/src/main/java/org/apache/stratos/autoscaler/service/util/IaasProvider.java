/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.autoscaler.service.util;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the basic data structure which holds an IaaS specific details.
 */
public class IaasProvider {
    
    /**
     * Unique id to identify this IaaS provider.
     */
    private String type;
    
    /**
     * human description of this IaaS provider
     */
    private String name;
    
    /**
     * Property map for this IaaS provider.
     */
    private Map<String, String> properties = new HashMap<String, String>();
    
    /**
     * Image identifier.
     */
    private String template;
    
    /**
     * Scale up order and scale down order of the IaaS.
     */
    private int scaleUpOrder, scaleDownOrder;
    
    private String provider, identity, credential;
    
//    public enum SortParameter {
//        SCALE_UP, SCALE_DOWN
//    }

    
    public String getType() {
        return type;
    }

    public void setType(String id) {
        this.type = id;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
    
    public void setProperty(String key, String value) {
        
        if(key != null && value != null){
            properties.put(key, value);
        }
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public int getScaleUpOrder() {
        return scaleUpOrder;
    }

    public void setScaleUpOrder(int scaleUpOrder) {
        this.scaleUpOrder = scaleUpOrder;
    }

    public int getScaleDownOrder() {
        return scaleDownOrder;
    }

    public void setScaleDownOrder(int scaleDownOrder) {
        this.scaleDownOrder = scaleDownOrder;
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

    
}
