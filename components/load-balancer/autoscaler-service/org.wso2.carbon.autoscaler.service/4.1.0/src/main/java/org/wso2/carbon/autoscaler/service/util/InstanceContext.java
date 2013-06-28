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
import java.util.LinkedHashMap;
import java.util.Map;

import org.jclouds.compute.domain.Template;

/**
 * This will hold the run-time data related to an instance.
 * Instance can be an EC2 one, Openstack one etc.
 */
public class InstanceContext implements Serializable {

    private static final long serialVersionUID = -2604902942512629140L;
    private String domain;
    private String subDomain;
    private transient Template template;

    /**
     * Key - node Id
     * Value - IP
     */
    private Map<String, String> nodeIdToIpMap;
    
    public InstanceContext(String domain, String subDomain, Template temp) {
        this.domain = domain;
        this.subDomain = subDomain;
        this.template = temp;
        nodeIdToIpMap = new LinkedHashMap<String, String>();
    }
//    
//    public InstanceContext(String domain, String subDomain, Template temp, String nodeId, String publicIp) {
//        this.domain = domain;
//        this.subDomain = subDomain;
//        this.template = temp;
//        this.nodeId = nodeId;
//        this.publicIp = publicIp;
//    }

    
    public String getDomain() {
        return domain;
    }

    public String getSubDomain() {
        return subDomain;
    }
    
    public Template getTemplate() {
        return template;
    }
    
    public void setTemplate(Template temp) {
        this.template = temp;
    }
    
    public void addNode(String nodeId, String ip) {
        if("".equals(ip)){
            ip = null;
        }
        nodeIdToIpMap.put(nodeId, ip);
    }
    
    public void removeNode(String nodeId) {
        nodeIdToIpMap.remove(nodeId);
    }


    public Map<String, String> getNodeIdToIpMap() {
        return nodeIdToIpMap;
    }

    
}
