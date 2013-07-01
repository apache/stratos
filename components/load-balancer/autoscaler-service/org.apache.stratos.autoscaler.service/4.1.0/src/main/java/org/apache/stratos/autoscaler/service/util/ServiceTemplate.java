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
package org.apache.stratos.autoscaler.service.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.stratos.lb.common.conf.util.Constants;

/**
 * We keep details under a service element of jcoluds configuration file,
 * in this object.
 */
public class ServiceTemplate implements Cloneable {

    private String domainName;
    private String subDomainName = Constants.DEFAULT_SUB_DOMAIN;
    private Map<String, String> properties = new HashMap<String, String>();
    
    public String getDomainName() {
        return domainName;
    }
    
    public boolean setDomainName(String domainName) {
        if (!"".equals(domainName)) {
            this.domainName = domainName;
            return true;
        }
        
        return false;
    }
    
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
    
    public String getProperty(String key) {
        
        if(properties.containsKey(key)){
            return properties.get(key);
        }
        
        return "";
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public String getSubDomainName() {
        return subDomainName;
    }

    public void setSubDomainName(String subDomainName) {
        if(subDomainName == null || "".equals(subDomainName)){
            return;
        }
        this.subDomainName = subDomainName;
    }
    
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
