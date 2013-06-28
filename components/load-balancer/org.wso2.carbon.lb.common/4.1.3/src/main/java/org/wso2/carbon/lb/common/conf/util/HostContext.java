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
package org.wso2.carbon.lb.common.conf.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.synapse.endpoints.algorithms.AlgorithmContext;
import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;

/**
 * For each unique host name defined in loadbalancer configuration, we'll generate
 * this object. 
 */
public class HostContext {
    
    /**
     * A unique identifier to identify this {@link #HostContext()}
     */
    private String hostName;
    
    /**
     * Key - tenant id
     * Value - <code>TenantDomainContext</code> of the corresponding tenant
     */
    private Map<Integer, TenantDomainContext> tenantIdToTenantDomainContextMap = new HashMap<Integer, TenantDomainContext>();
    
    /**
     * AlgorithmContext of this host
     */
    private AlgorithmContext algorithmContext;
    
    /**
     * Load balance algorithm of this host
     */
    private LoadbalanceAlgorithm algorithm;
    
    private String urlSuffix;
    
    public HostContext(String hostName) {
        this.hostName = hostName;
    }
    
    public void addTenantDomainContexts(List<TenantDomainContext> ctxts) {
        
        for (TenantDomainContext tenantDomainContext : ctxts) {
            tenantIdToTenantDomainContextMap.put(tenantDomainContext.getTenantId(), tenantDomainContext);
        }
    }
    
    @Deprecated
    public void addTenantDomainContext(TenantDomainContext ctxt) {
        tenantIdToTenantDomainContextMap.put(ctxt.getTenantId(), ctxt);
    }
    
    public TenantDomainContext getTenantDomainContext(int tenantId) {
        return tenantIdToTenantDomainContextMap.get(tenantId);
    }
    
    /**
     * Returns all the {@link TenantDomainContext} entries.
     */
    public Collection<TenantDomainContext> getTenantDomainContexts() {
        return tenantIdToTenantDomainContextMap.values();
    }
    
    /**
     * Given a tenant id, this will return its domain.
     * @param tenantId 
     * @return domain if this tenant has a dedicated one, it will be returned.
     * If not, and there's a default (*) domain, it will be returned.
     * If neither of the above is defined, null will be returned.
     */
    public String getDomainFromTenantId(int tenantId) {
        if (tenantIdToTenantDomainContextMap.get(tenantId) == null) {
            // if there's no specific domain for this tenant, we will redirect it to the default
            // cluster
            
            if(tenantIdToTenantDomainContextMap.get(0) == null){
                return null;
            }
            
            return tenantIdToTenantDomainContextMap.get(0).getDomain();
        }

        return tenantIdToTenantDomainContextMap.get(tenantId).getDomain();
    }
    
    /**
     * Given a tenant id, this will return its sub domain.
     * @param tenantId 
     * @return sub_domain if this tenant has a dedicated one, it will be returned.
     * If not, and there's a default (*) sub domain, it will be returned.
     * If neither of the above is defined, null will be returned.
     */
    public String getSubDomainFromTenantId(int tenantId) {
        if (tenantIdToTenantDomainContextMap.get(tenantId) == null) {
            // if there's no specific domain for this tenant, we will redirect it to the default
            // cluster
            
            if(tenantIdToTenantDomainContextMap.get(0) == null){
                return null;
            }
            
            return tenantIdToTenantDomainContextMap.get(0).getSubDomain();
        }

        return tenantIdToTenantDomainContextMap.get(tenantId).getSubDomain();
    }

    public String getHostName() {
        return hostName;
    }
    
    public LoadbalanceAlgorithm getAlgorithm() {
        return algorithm;
    }

    public AlgorithmContext getAlgorithmContext() {
        return algorithmContext;
    }

    public void setAlgorithmContext(AlgorithmContext algorithmContext) {
        this.algorithmContext = algorithmContext;
    }

    public void setAlgorithm(LoadbalanceAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public Map<Integer, TenantDomainContext> getTenantIdToTenantDomainContextMap() {
        return tenantIdToTenantDomainContextMap;
    }
    
    public void setUrlSuffix(String suffix)  {
        this.urlSuffix = suffix;
    }

    public String getUrlSuffix() {
        return this.urlSuffix;
    }

}
