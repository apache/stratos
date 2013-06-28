/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.stratos.cloud.controller.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Template;
import org.wso2.carbon.stratos.cloud.controller.interfaces.Iaas;

/**
 * This is the basic data structure which holds an IaaS specific details.
 * NOTE: If you add a new attribute, please assign it in the constructor too.
 */
public class IaasProvider implements Serializable{
   
    private static final long serialVersionUID = -940288190885166118L;

	/**
     * Unique id to identify this IaaS provider.
     */
    private String type;
    
    /**
     * Fully qualified class name of an implementation of {@link Iaas}
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
     * Image identifier.
     */
    private String image;
    
    /**
     * Max instance limit that an IaaS can spawn.
     */
    private int maxInstanceLimit = -1;
    
    /**
     * Scale up order and scale down order of the IaaS.
     */
    private int scaleUpOrder = -1, scaleDownOrder = -1;
    
    private String provider, identity, credential;
    
    private transient ComputeService computeService;
    
    private transient Template template;
    
    private byte[] payload;
    
    /** 
     * Corresponding {@link Iaas} implementation
     */
    private transient Iaas iaas;
    
    public IaasProvider(){}
    
    public IaasProvider(IaasProvider anIaasProvider){
    	this.type = anIaasProvider.getType();
    	this.name = anIaasProvider.getName();
    	this.className = anIaasProvider.getClassName();
    	this.properties = anIaasProvider.getProperties();
    	this.image = anIaasProvider.getImage();
    	this.scaleUpOrder = anIaasProvider.getScaleUpOrder();
    	this.scaleDownOrder = anIaasProvider.getScaleDownOrder();
    	this.provider = anIaasProvider.getProvider();
    	this.identity = anIaasProvider.getIdentity();
    	this.credential = anIaasProvider.getCredential();
    	this.computeService = anIaasProvider.getComputeService();
    	this.template = anIaasProvider.getTemplate();
    	this.payload = anIaasProvider.getPayload();
    	this.iaas = anIaasProvider.getIaas();
    	this.maxInstanceLimit = anIaasProvider.getMaxInstanceLimit();
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
    
    public void setProperty(String key, String value) {
        
        if(key != null && value != null){
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
        if(o instanceof IaasProvider){
            return ((IaasProvider) o).getType().equals(this.getType());
        }
        
        return false;
    }
    
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
            append(type).
            toHashCode();
    }
    
    public IaasProvider copy(){
		return new IaasProvider(this);
	}

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public Iaas getIaas() {
        return iaas;
    }

    public void setIaas(Iaas iaas) {
        this.iaas = iaas;
    }
    
    public void reset(){
//    	nodeIds = new ArrayList<String>();
//    	nodes = new HashMap<String, NodeMetadata>();
//    	toBeRemovedNodeIds = new ArrayList<String>();
    }

	public int getMaxInstanceLimit() {
	    return this.maxInstanceLimit;
    }

	public void setMaxInstanceLimit(int maxInstanceLimit) {
	    this.maxInstanceLimit = maxInstanceLimit;
    }

    
}
