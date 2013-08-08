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
package org.apache.stratos.cloud.controller.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jclouds.compute.domain.NodeMetadata;

/**
 * 
 * Holds runtime data of an IaaS
 */
public class IaasContext implements Serializable{
	
    private static final long serialVersionUID = 3370272526949562217L;

	private String type;
	
	private List<String> nodeIds;
	
	private Map<String, String> nodeToPublicIp;
	
	private transient Map<String, NodeMetadata> nodes;
	
	private List<String> toBeRemovedNodeIds;
	
	public IaasContext(String iaasType) {
		this.type = iaasType;
		nodeToPublicIp = new HashMap<String, String>();
		nodeIds = new ArrayList<String>();
		nodes = new HashMap<String, NodeMetadata>();
		toBeRemovedNodeIds = new ArrayList<String>();
    }

	public Map<String, String> getNodeToPublicIp() {
		return nodeToPublicIp;
	}
	
	public Map<String, NodeMetadata> getNodes() {
		return nodes;
	}
	
	public void setToBeRemovedNodeIds(List<String> list) {
		this.toBeRemovedNodeIds = list;
	}
	
	public List<String> getAllNodeIds() {
		List<String> allNodeIds = new ArrayList<String>(nodeIds);
		allNodeIds.addAll(toBeRemovedNodeIds);
		return allNodeIds;
	}
	
	public List<String> getNodeIds() {
		return nodeIds;
	}
	
	public List<String> getToBeRemovedNodeIds() {
		return toBeRemovedNodeIds;
	}
	
	public boolean didISpawn(String nodeId) {
		if(nodeIds.contains(nodeId) || toBeRemovedNodeIds.contains(nodeId)){
			return true;
		}
		return false;
	}
	
	public void addNodeId(String nodeId) {
		nodeIds.add(nodeId);
	}
	
	public void addNodeToPublicIp(String nodeId, String publicIp) {
		nodeToPublicIp.put(nodeId, publicIp);
	}
	
	public void addToBeRemovedNodeId(String nodeId) {
		toBeRemovedNodeIds.add(nodeId);
	}
	
	public void removeNodeId(String nodeId) {
		if(nodeIds.remove(nodeId)){
			toBeRemovedNodeIds.add(nodeId);
		}
	}
	
	public void removeToBeRemovedNodeId(String nodeId) {
		toBeRemovedNodeIds.remove(nodeId);
	}
	
	public void setNodeIds(List<String> nodeIds) {
		this.nodeIds = nodeIds;
	}
	
	public String lastlySpawnedNode() {
		return nodeIds.get(nodeIds.size()-1);
	}
	
	public void addNodeMetadata(NodeMetadata node) {
	    if(nodes == null){
	        nodes = new HashMap<String, NodeMetadata>();
	    }
		nodes.put(node.getId(), node);
	}
	
    public void removeNodeMetadata(NodeMetadata node) {
        if (nodes != null) {
            nodes.remove(node.getId());
        }
    }
	
	public void removeNodeIdToPublicIp(String nodeId){
		nodeToPublicIp.remove(nodeId);
	}
	
	public NodeMetadata getNode(String nodeId) {
	    if(nodes == null) {
	        return null;
	    }
		return nodes.get(nodeId);
	}
	
	public String getPublicIp(String nodeId){
		return nodeToPublicIp.get(nodeId);
	}

	public String getType() {
        return type;
    }

	public void setType(String type) {
        this.type = type;
    }

}
