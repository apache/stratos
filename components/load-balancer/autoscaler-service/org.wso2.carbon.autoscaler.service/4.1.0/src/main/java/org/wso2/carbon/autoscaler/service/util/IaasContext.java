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
package org.wso2.carbon.autoscaler.service.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Template;
import org.wso2.carbon.autoscaler.service.impl.AutoscalerServiceImpl.Iaases;

/**
 * This object holds all IaaS related runtime data.
 */
public class IaasContext implements Serializable{
   
    private static final long serialVersionUID = -922284976926131383L;
    
    // name of the IaaS
    private Enum<Iaases> name;
    
    /* We keep following maps in order to make the look up time, low.*/
    
    /**
     * Key - domain
     * Value - is another map
     *          key - sub domain
     *          value - <code>InstanceContext</code>
     */
    private Map<String, Map<String, InstanceContext>> instanceCtxts;
    
    /**
     * Key - public IP
     * Value - <code>InstanceContext</code>
     */
    private Map<String, InstanceContext> publicIpToInstanceCtxt;
    
    /**
     * Key - node id 
     * Value - <code>InstanceContext</code>
     */
    private Map<String, InstanceContext> nodeIdToInstanceCtxt;
    
    
//    private transient Map<String, Template> domainToTemplateMap;
    private transient ComputeService computeService;
    
    // Since Jclouds' NodeMetadata object contains unserializable objects, I had to use 3 maps.
//    private Map<String, String> nodeIdToDomainMap = new LinkedHashMap<String, String>();
//    private Map<String, String> publicIpToDomainMap = new LinkedHashMap<String, String>();
//    private Map<String, String> publicIpToNodeIdMap = new LinkedHashMap<String, String>();
    
    private int scaleUpOrder, scaleDownOrder;

    public IaasContext(Enum<Iaases> name, ComputeService computeService) {
        this.name = name;
        this.computeService = computeService;
        instanceCtxts = new LinkedHashMap<String, Map<String,InstanceContext>>();
        publicIpToInstanceCtxt = new LinkedHashMap<String, InstanceContext>();
        nodeIdToInstanceCtxt = new LinkedHashMap<String, InstanceContext>();
    }

    public Enum<Iaases> getName() {
        return name;
    }
    
    public void addInstanceContext(InstanceContext ctx) {
        
        if(ctx == null){
            return;
        }
        
        String domain = ctx.getDomain();
        String subDomain = ctx.getSubDomain();
        
        if(domain != null && subDomain != null){
            addToInstanceCtxts(domain, subDomain, ctx);
        }
        
    }
    
    public void addNodeDetails(String domain, String subDomain, String nodeId, String ip) {
        
        if(getInstanceContext(domain, subDomain) != null){
            getInstanceContext(domain, subDomain).addNode(nodeId, ip);
            
            nodeIdToInstanceCtxt.put(nodeId, getInstanceContext(domain, subDomain));
            publicIpToInstanceCtxt.put(ip, getInstanceContext(domain, subDomain));
        }
    }
    
    private void addToInstanceCtxts(String domainName, String subDomainName, InstanceContext ctx) {

        Map<String, InstanceContext> map;
        
        if(instanceCtxts.get(domainName) == null){
            map = new HashMap<String, InstanceContext>();
            
        } else{
            map = instanceCtxts.get(domainName);
        }
        
        map.put(subDomainName, ctx);
        instanceCtxts.put(domainName, map);
        
    }

//    public void addToDomainToTemplateMap(String key, Template value) {
//        domainToTemplateMap.put(key, value);
//    }

    public Template getTemplate(String domain, String subDomain) {
        if(getInstanceContext(domain, subDomain) == null){
            return null;
        }
        return getInstanceContext(domain, subDomain).getTemplate();
    }
    
    public InstanceContext getInstanceContext(String domain, String subDomain) {
        if (instanceCtxts.get(domain) != null) {
            return instanceCtxts.get(domain).get(subDomain);
        }
        return null;
    }

    public ComputeService getComputeService() {
        return computeService;
    }
    
    public void setComputeService(ComputeService computeService) {
        this.computeService = computeService;
    }

//    public void addNodeIdToDomainMap(String nodeId, String domain) {
//        nodeIdToDomainMap.put(nodeId, domain);
//    }
//    
//    public void addPublicIpToDomainMap(String ip, String domain) {
//        publicIpToDomainMap.put(ip, domain);
//    }
//    
//    public void addPublicIpToNodeIdMap(String ip, String nodeId) {
//        publicIpToNodeIdMap.put(ip, nodeId);
//    }

    /**
     * This will return the node id of the node which is belong to the
     * requesting domain, sub domain and which is the most recently created. If it cannot find a
     * matching node id, this will return <code>null</code>.
     * @param domain service domain.
     * @param subDomain service sub domain. 
     * @return the node Id of the node
     */
    public String getLastMatchingNode(String domain, String subDomain) {
        
        InstanceContext ctx = getInstanceContext(domain, subDomain);
        
        if(ctx == null){
            return null;
        }
        
        // iterate in reverse order
        ListIterator<String> iter =
            new ArrayList<String>(ctx.getNodeIdToIpMap().keySet()).
                                listIterator(ctx.getNodeIdToIpMap().size());

        if (iter.hasPrevious()) {
            return iter.previous();
        }
        
        return null;
    }
    
    /**
     * This will return the public IP of the node which is belong to the
     * requesting domain, sub domain and which is the most recently created. If it cannot find a
     * matching public IP, this will return <code>null</code>.
     * @param domain service domain. 
     * @param subDomain service sub domain. 
     * @return the public IP of the node
     */
    public String getLastMatchingPublicIp(String domain, String subDomain) {
        
        InstanceContext ctx = getInstanceContext(domain, subDomain);
        
        if(ctx == null){
            return null;
        }
        
        // iterate in reverse order
        ListIterator<String> iter =
            new ArrayList<String>(ctx.getNodeIdToIpMap().keySet()).
                                listIterator(ctx.getNodeIdToIpMap().size());

        while (iter.hasPrevious()) {
            return ctx.getNodeIdToIpMap().get(iter.previous());
        }
        
        return null;
        
//        // traverse from the last entry of the map
//        ListIterator<Map.Entry<String, String>> iter =
//            new ArrayList<Entry<String, String>>(publicIpToDomainMap.entrySet()).
//                                listIterator(publicIpToDomainMap.size());
//
//        while (iter.hasPrevious()) {
//            Map.Entry<String, String> entry = iter.previous();
//            if (entry.getValue().equals(domain)) {
//                return entry.getKey();
//            }
//        }
//        
//        return null;
    }

    /**
     * This will return the node id of the node which is belong to the
     * requesting domain, sub domain and which is created at first. If it cannot find a
     * matching node id, this will return <code>null</code>.
     * @param domain service domain.
     * @param subDomain service sub domain.
     * @return node id of the node
     */
    public String getFirstMatchingNode(String domain, String subDomain) {
        
        InstanceContext ctx = getInstanceContext(domain, subDomain);
        
        if(ctx == null){
            return null;
        }
        
        // iterate in added order
        ListIterator<String> iter =
            new ArrayList<String>(ctx.getNodeIdToIpMap().keySet()).
                                listIterator(0);

        while (iter.hasNext()) {
            return iter.next();
        }
        
        return null;
        
//        for (Entry<String, String> entry : nodeIdToDomainMap.entrySet()) {
//            if (entry.getValue().equals(domain)) {
//                return entry.getKey();
//            }
//        }
//        return null;
    }

    /**
     * This will return the node id of the node which has the given public IP. 
     * If it cannot find a matching node id, this will return 
     * <code>null</code>.
     * @param publicIp public IP of a node.
     * @return node id of the matching node.
     */
    public String getNodeWithPublicIp(String publicIp) {

        InstanceContext ctx;

        for (String ip : publicIpToInstanceCtxt.keySet()) {

            if (ip.equals(publicIp)) {

                ctx = publicIpToInstanceCtxt.get(ip);

                for (String nodeId : nodeIdToInstanceCtxt.keySet()) {
                    if (ctx.equals(nodeIdToInstanceCtxt.get(nodeId))) {
                        return nodeId;
                    }
                }
            }
        }

        return null;
    }

    /**
     * This will return a list of node Ids that are started in this IaaS and that are 
     * belong to the given domain, sub domain.
     * @param domain service domain.
     * @param subDomain service sub domain.
     * @return List of node Ids.
     */
    public List<String> getNodeIds(String domain, String subDomain) {
        
        InstanceContext ctx = getInstanceContext(domain, subDomain);
        
        if(ctx == null){
            return new ArrayList<String>();
        }
        
        return new ArrayList<String>(ctx.getNodeIdToIpMap().keySet());
        

//        List<String> nodeIds = new ArrayList<String>();
//
//        for (Entry<String, String> entry : nodeIdToDomainMap.entrySet()) {
//            if (entry.getValue().equals(domain)) {
//                nodeIds.add(entry.getKey());
//            }
//        }
//
//        return nodeIds;
    }

    /**
     * Removes a specific node id and related entries.
     * @param node id of the node to be removed.
     */
    public void removeNodeId(String nodeId) {
        
        InstanceContext ctx;
        
        if(nodeIdToInstanceCtxt.containsKey(nodeId)){
            // remove from node id map
            ctx = nodeIdToInstanceCtxt.remove(nodeId);
            
            // remove from public ip map
            publicIpToInstanceCtxt.remove(ctx.getNodeIdToIpMap().get(nodeId));
            
            // remove from main map
            instanceCtxts.get(ctx.getDomain()).get(ctx.getSubDomain()).removeNode(nodeId);
            
        }
    }

    public boolean equals(Object obj) {

        if (obj instanceof IaasContext) {
            return new EqualsBuilder().append(getName(), ((IaasContext) obj).getName()).isEquals();
        }
        return false;

    }
    
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
        append(name).
        toHashCode();
    }

    public int getScaleDownOrder() {
        return scaleDownOrder;
    }

    public void setScaleDownOrder(int scaleDownOrder) {
        this.scaleDownOrder = scaleDownOrder;
    }

    public int getScaleUpOrder() {
        return scaleUpOrder;
    }

    public void setScaleUpOrder(int scaleUpOrder) {
        this.scaleUpOrder = scaleUpOrder;
    }
    
//    public void setDomainToTemplateMap(Map<String, Template> map) {
//        domainToTemplateMap = map;
//    }
//    
//    public Map<String, Template> getDomainToTemplateMap() {
//        return domainToTemplateMap;
//    }

}
