package org.wso2.carbon.stratos.cloud.controller.util;

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
	
	private int currentInstanceCount = 0;
	
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

	public int getCurrentInstanceCount() {
	    return currentInstanceCount;
    }

	public void incrementCurrentInstanceCountByOne() {
	    this.currentInstanceCount += 1;
    }
	
	public void decrementCurrentInstanceCountByOne() {
	    this.currentInstanceCount -= 1;
    }
}
