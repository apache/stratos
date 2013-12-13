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

package org.apache.stratos.autoscaler.partition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The model class for managing Partitions.
 */
public class PartitionManager {

private static final Log log = LogFactory.getLog(PartitionManager.class);
	
	// Partitions against partitionID
	private static Map<String,Partition> partitions = new HashMap<String, Partition>();
	
	private List<NetworkPartitionContext> networkPartitions;
	
	/*
	 * Key - partition id
	 * Value - reference to NetworkPartition 
	 */
//	private Map<String, NetworkPartitionContext> partitionIdToNetworkPartition;


	/*
	 * Key - network partition id
	 * Value - reference to NetworkPartition
	 */
	private Map<String, NetworkPartitionContext> networkPartitionContexts;

	private static PartitionManager instance;
	
	private String partitionResourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE 
			+ AutoScalerConstants.PARTITION_RESOURCE + "/";
	
	private PartitionManager(){
        networkPartitionContexts = new HashMap<String, NetworkPartitionContext>();
//	    networkPartitions = new ArrayList<NetworkPartitionContext>();
//	    partitionIdToNetworkPartition = new HashMap<String, NetworkPartitionContext>();
	}
	
	public static PartitionManager getInstance(){
		if(null == instance)
			return new PartitionManager();
		else
			return instance;
	}
	
	public boolean partitionExist(String partitionId){
		return partitions.containsKey(partitionId);
	}
	
	/*
	 * Deploy a new partition to Auto Scaler.
	 */
	public boolean addNewPartition(Partition partition) throws AutoScalerException{
		String partitionId = partition.getId();
		if(this.partitionExist(partition.getId()))
			throw new AutoScalerException("A parition with the ID " +  partitionId + " already exist.");
				
		String resourcePath= this.partitionResourcePath + partition.getId();
		
        RegistryManager regManager = RegistryManager.getInstance();     
        
        try {
        	this.validatePartition(partition);

        	regManager.persist(partition, resourcePath);
			addPartitionToInformationModel(partition);	

	        // register network partition
//	        NetworkPartitionContext nwPartition = getOrAddNetworkPartition(partition);
//	        this.partitionIdToNetworkPartition.put(partitionId, nwPartition);
//            this.networkPartitionIdToNetworkPartition.put(nwPartition.getId(), nwPartition);


		} catch (RegistryException e) {
			throw new AutoScalerException(e);
		} catch(PartitionValidationException e){
			throw new AutoScalerException(e);
		}
                
		log.info("Partition :" + partition.getId() + " is deployed successfully.");
		return true;
	}
	
	
	public void addPartitionToInformationModel(Partition partition) {
		partitions.put(partition.getId(), partition);
	}

//	public NetworkPartitionContext getNetworkPartitionOfPartition(String partitionId) {
//	    return this.partitionIdToNetworkPartition.get(partitionId);
//	}
	
	public NetworkPartitionContext getNetworkPartition(String networkPartitionId) {
	    return this.networkPartitionContexts.get(networkPartitionId);
	}

	public List<NetworkPartitionContext> getAllNetworkPartitions() {
	    return this.networkPartitions;
	}

//	/**
//	 * TODO make {@link NetworkPartitionContext}s extensible.
//	 * @param partition
//	 */
//	protected NetworkPartitionContext getOrAddNetworkPartition(Partition partition) {
//
//	    if(partition == null) {
//	        return null;
//	    }
//	    String provider = partition.getProvider();
//	    String region = null;
//	    Properties properties = partition.getProperties();
//        if (properties != null) {
//            for (Property prop : properties.getProperties()) {
//                if(Constants.REGION_PROPERTY.equals(prop.getName())) {
//                    region = prop.getValue();
//                    break;
//                }
//            }
//        }
//        NetworkPartitionContext networkPar = new NetworkPartitionContext(provider, region);
//        if(!this.networkPartitions.contains(networkPar)){
//            this.networkPartitions.add(networkPar);
//        } else {
//            int idx = this.networkPartitions.indexOf(networkPar);
//            networkPar = this.networkPartitions.get(idx);
//        }
//
//        return networkPar;
//    }

    public Partition getPartitionById(String partitionId){
		if(partitionExist(partitionId))
			return partitions.get(partitionId);
		else
			return null;
	}
	
	public Partition[] getAllPartitions(){
		//return Collections.unmodifiableList(new ArrayList<Partition>(partitions.values()));
		return partitions.values().toArray(new Partition[0]);
		
	}
	
	public boolean validatePartition(Partition partition) throws PartitionValidationException{				
		return CloudControllerClient.getInstance().validatePartition(partition);
	}

    public void deployNewNetworkPartitions(DeploymentPolicy depPolicy) {
        for(PartitionGroup partitionGroup: depPolicy.getPartitionGroups()){
            NetworkPartitionContext networkPartitionContext = new NetworkPartitionContext(partitionGroup.getId());
            networkPartitionContexts.put(partitionGroup.getId(), networkPartitionContext);

        }
    }

}
