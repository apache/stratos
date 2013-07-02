/**
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.stratos.adc.topology.mgt.serviceobjects;

/**
 * Class to hold domain and subdomain details
 *
 */
public class DomainContext {
    
    private String domain;
    private String subDomain;
    
    /**
     * Constructor
     * 
     * @param domain domain name
     * @param subDomain subdomain name
     */
    public DomainContext (String domain, String subDomain) {
        this.domain = domain;
        this.subDomain = subDomain;
    }

    /**
     * Returns the domain 
     * 
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Returns the sub domain
     * 
     * @return the subDomain
     */
    public String getSubDomain() {
        return subDomain;
    }
    
    /**
     * Overridden equals method
     */
    public boolean equals (Object object) {
        if (object == null) 
            return false;
        if (object == this)  
            return true;
        if (!(object instanceof DomainContext))
            return false;
        
        DomainContext domainCtx = (DomainContext)object;
        if(this.getDomain().equals(domainCtx.getDomain()) &&
                this.getSubDomain().equals(domainCtx.getSubDomain()))
            return true;
        else
            return false;
    }
    
    /**
     * Overridden hashCode method
     */
    public int hashCode () {
        int domainHash = 0;
        int subDomainHash = 0;
        
        if(domain != null)
            domainHash = domain.hashCode();
        if(subDomain != null)
            subDomainHash = subDomain.hashCode();
        
        return domainHash + subDomainHash;
    }

}