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

package org.apache.stratos.autoscaler.policy.model;

import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The model class for Deployment-Policy definition.
 */
public class DeploymentPolicy implements Serializable{

    private static final long serialVersionUID = 5675507196284400099L;
    private String id;
    private String description;
    private boolean isPublic;
    private PartitionGroup[] partitionGroups;
    private int tenantId;

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
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
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
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }
    
    /**
     * Gets the value of the isPublic property.
     * 
     * @return
     *     possible object is boolean
     *     
     */
	public boolean getIsPublic() {
		return isPublic;
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
     * Gets the value of the tenantId property.
     * 
     *          
     */
	public int getTenantId() {
		return tenantId;
	}

	 /**
     * Sets the value of the tenantId property.
     * 
     *     
     */
	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}
    
    public void setPartitionGroups(PartitionGroup[] partitionGroups) {
        if(partitionGroups == null) {
            this.partitionGroups = new PartitionGroup[0];
        } else {
            this.partitionGroups = Arrays.copyOf(partitionGroups, partitionGroups.length);
        }
    }
    
    public Partition[] getAllPartitions() {
    	ArrayList<Partition> partitionsList = new ArrayList<Partition>();
    	for (PartitionGroup partitionGroup : this.getPartitionGroups()) {
            Partition[] partitions = partitionGroup.getPartitions();
            if(partitions != null) {
            	partitionsList.addAll(Arrays.asList(partitions));
            }
        }
        return partitionsList.toArray(new Partition[partitionsList.size()]);
    }
        
    public Partition getPartitionById(String id){
    	for(Partition p : this.getAllPartitions()){
    		if(p.getId().equalsIgnoreCase(id))
    			return p;
    	}
    	 return null;
    }
    
    /**
     * Gets the value of the partition-groups.
     */
    public PartitionGroup[] getPartitionGroups() {
        
        return this.partitionGroups;
    }
    
    public PartitionGroup getPartitionGroup(String partitionGrpId){
    	for(PartitionGroup parGrp : this.getPartitionGroups()){
    		if(parGrp.getId().equals(partitionGrpId))
    			return parGrp;
    		
    	}
    	return null;
    }
    
    public String toString() {
        return "Deployment Policy [id]" + this.id + " Description " +  this.description 
        		+ " isPublic " +  this.isPublic 
        		+" [partitions] " + Arrays.toString(this.getAllPartitions());
    }

}
