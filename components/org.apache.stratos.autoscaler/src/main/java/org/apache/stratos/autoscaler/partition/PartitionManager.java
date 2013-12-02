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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;

/**
 * The model class for managing Partitions.
 */
public class PartitionManager {

private static final Log log = LogFactory.getLog(PartitionManager.class);
	
	// Partitions against partitionID
	private static Map<String,Partition> partitionListMap = new HashMap<String, Partition>();
	
	private static PartitionManager instance;
	
	private PartitionManager(){}
	
	public static PartitionManager getInstance(){
		if(null == instance)
			return new PartitionManager();
		else
			return instance;
	}
	
	public boolean partitionExist(String partitionId){
		return partitionListMap.containsKey(partitionId);
	}
	
	public void addPartition(String partitionId, Partition partition){
		if(partitionExist(partitionId))
			log.error("A partition with the ID " + partitionId +" already exist.");
		else
			partitionListMap.put(partitionId, partition);		 
	}
	
	public void removePartition(String partitionId){
		if(partitionExist(partitionId))
			partitionListMap.remove(partitionId);
		else
			log.error("A partition with the ID " + partitionId +" already does not exist."); 
	}
	
	public Partition getPartitionById(String partitionId){
		if(partitionExist(partitionId))
			return partitionListMap.get(partitionId);
		else
			return null;
	}
	
	public List<Partition> getAllPartitions(){
		return Collections.unmodifiableList(new ArrayList<Partition>(partitionListMap.values()));
		
	}

}
