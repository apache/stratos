/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.lb.common.conf.structure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * This is the basic data structure which holds a <i>Nginx</i> formatted configuration file.
 * 
 */
public class Node implements Serializable{

    private static final long serialVersionUID = 4071569903421115370L;

    /**
     * Name of this Node element
     */
    private String name;

    /**
     * Every node can have 0..n child nodes. 
     * They are kept in a List.
     */
    private List<Node> childNodes = new ArrayList<Node>();

    /**
     * Every node can have 0..n properties. 
     * They are kept in a Map, in the order they appear.
     * Key: property name
     * Value: property value
     */
    private Map<String, String> properties = new LinkedHashMap<String, String>();

    public void setChildNodes(List<Node> childNodes) {
        this.childNodes = childNodes;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * This will convert each child Node of this Node to a String.
     * @return a string which represents child nodes of this node.
     */
    public String childNodesToString(int indentation) {
        StringBuilder childNodesString = new StringBuilder();
        indentation++;
        
        for (Node node : childNodes) {
            childNodesString.append(node.toString(indentation)+"\n");
        }
        
        return childNodesString.toString();
    }

    /**
     * This will try to find a child Node of this Node, which has the given name.
     * @param name name of the child node to find.
     * @return child Node object if found or else <code>null</code>.
     */
    public Node findChildNodeByName(String name) {
        for (Node aNode : childNodes) {
            if (aNode.getName().equals(name)) {
                return aNode;
            }
        }

        return null;
    }

    /**
     * Returns the name of this Node. 
     * @return name of the node.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns child nodes List of this Node.
     * @return List of Node
     */
    public List<Node> getChildNodes() {
        return childNodes;
    }

    /**
     * Returns properties Map of this Node.
     * @return Map whose keys and values are String.  
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Returns the value of a given property.
     * @param key name of a property.
     * @return trimmed value if the property is found in this Node, or else <code>null</code>. 
     */
    public String getProperty(String key) {
        if (properties.get(key) == null) {
            return null;
        }
        return properties.get(key).trim();
    }

    /**
     * Returns all the properties of this Node as a String.
     * Key and value of the property is separated by a tab (\t) character and
     * each property is separated by a new line character.
     * @param indentation relative number of tabs 
     * @return properties of this node as a String.
     */
    public String propertiesToString(int indentation) {
        
        String indent = getIndentation(indentation);
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            // hack to get a quick fix in.
            if (!"tenant_id".equals(entry.getKey()) && !"alias".equals(entry.getKey())) {
                sb.append(indent + entry.getKey() + "\t" + entry.getValue() + ";\n");
            }
        }
        return sb.toString();
    }
    
    /**
     * Removes the first occurrence of a node having the given name
     * and returns the removed {@link Node}.
     * @param name name of the child node to be removed.
     * @return removed {@link Node} or else <code>null</code>.
     */
    public Node removeChildNode(String name) {
        Node aNode = findChildNodeByName(name);
        
        if(aNode != null){
            if(childNodes.remove(aNode)){
                return aNode;
            }
        }
        
        return null;
    }

    /**
     * Removes the first occurrence of a node equals to the given node.
     * @param node {@link Node} to be removed.
     * @return whether the removal is successful or not.
     */
    public boolean removeChildNode(Node node){

        return childNodes.remove(node);
    }
    
    /**
     * Sets the name of this Node.
     * @param name String to be set as the name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Appends a child node at the end of the List of child nodes of this Node, if 
     * a similar node is not already present as a child node.
     * @param aNode child Node to be appended.
     */
    public void appendChild(Node aNode) {
        if (aNode != null && !nodeAlreadyPresent(aNode)) {
            childNodes.add(aNode);
        }
    }
    
    /**
     * Adds a new property to properties Map of this Node if and only if 
     * key is not <code>null</code>.
     * @param key name of the property to be added.
     * @param value value of the property to be added.
     */
    public void addProperty(String key, String value) {
        if (key != null) {
            properties.put(key, value);
        }
    }
    
    /**
     * Convert this Node to a String which is in <i>Nginx</i> format.
     * <br/>
     * Sample:
     * <br></br>
     * <code>
     * ij {
     * <br/>
     * klm n;
     * <br/>
     * pq {
     * <br/>
     * rst u;
     * <br/>
     * }
     * <br/>
     * }
     * <br/>
     * </code>
     */
    public String toString() {
        
        String nodeString = 
                getName()+" {\n" +
                (propertiesToString(1)) +
                (childNodesToString(1)) +
                "}";
        
        return nodeString;
    }
    
    public boolean equals(Object node) {
        
        if(node instanceof Node){
            return this.getName().equals(((Node) node).getName()) &&
                    isIdenticalProperties(this.getProperties(), ((Node) node).getProperties()) &&
                    isIdenticalChildren(this.getChildNodes(), ((Node) node).getChildNodes());
        }
        
        return false;
        
    }
    
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
            append(name).
            append(properties).
            append(childNodes).
            toHashCode();
    }
    
    private boolean isIdenticalChildren(List<Node> childNodes1, List<Node> childNodes2) {
        
        if(childNodes1.size() != childNodes2.size()){
            return false;
        }
        
        for (Node node1 : childNodes1) {
            int i=0;
            for (Node node2 : childNodes2) {
                
                i++;
                if(node1.equals(node2)){
                    break;
                }
                
                if(i == childNodes1.size()){
                    return false;
                }
                
            }
        }
        
        return true;
    }
    
    private boolean nodeAlreadyPresent(Node aNode){
        
        for(Node node : this.childNodes){
            if(node.equals(aNode)){
                return true;
            }
        }
        
        return false;
    }

    private boolean isIdenticalProperties(Map<String, String> map1,
        Map<String, String> map2) {
        
        if(map1.size() != map2.size()){
            return false;
        }
        
        for (Iterator<Entry<String, String>> iterator1 = map1.entrySet().iterator(); iterator1.hasNext();) {
            Map.Entry<String, String> entry1 = (Map.Entry<String, String>) iterator1.next();
            
            int i=0;
            
            for(Iterator<Entry<String, String>> iterator2 = map2.entrySet().iterator(); iterator2.hasNext();) {
                Map.Entry<String, String> entry2 = (Map.Entry<String, String>) iterator2.next();
                
                i++;
                
                if((entry1.getKey().equals(entry2.getKey()) &&
                        entry1.getValue().equals(entry2.getValue()))){
                    
                    break;
                }
                
                if(i == map1.size()){
                    return false;
                }
                
            }
        }
        
        return true;
    }

    private String toString(int indentation){
        
        String indent = getIndentation(indentation-1);
        
        String nodeString = 
                indent + getName()+" {\n" +
                (propertiesToString(indentation)) +
                (childNodesToString(indentation)) +
                indent + "}";
        
        return nodeString;
    }
    
    private String getIndentation(int tabs){
        
        StringBuilder indent = new StringBuilder("");
        
        for (int i = 0; i < tabs; i++) {
            indent.append("\t");
        }
                
        return indent.toString();
    }

}
