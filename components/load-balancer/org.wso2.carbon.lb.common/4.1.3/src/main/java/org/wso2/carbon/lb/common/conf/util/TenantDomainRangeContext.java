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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//public class TenantDomainRangeContext {
//    
//    private static final Log log = LogFactory.getLog(TenantDomainRangeContext.class);
//    
//    /**
//     * Map is used instead of using a list, in order to minimize the lookup time.
//     * key - host name
//     * Value - A map with tenant id as key and TenantDomainContext object as value
//     */
//    private Map<String, Map<Integer, TenantDomainContext>> tenantDomainContextMap = new HashMap<String, Map<Integer, TenantDomainContext>>();
//
//    /**
//     * This method will populate the {@link #tenantDomainContextMap} with <code>TenantDomainContext</code>
//     * objects created for all the tenants covered by the given range.
//     */
//    public void addTenantDomain(String domain, String subDomain, String tenantRange) {
//        
//        String msg = "Invalid tenant range is specified for domain " + domain+" - sub domain "+subDomain;
//
//        String[] parsedLine = tenantRange.trim().split("-");
//        
//        if (parsedLine[0].equalsIgnoreCase("*")) {
//            
//            tenantDomainContextMap.put(0, new TenantDomainContext(0, domain, subDomain));
//
//        } else if (parsedLine.length == 1) {
//            
//            try {
//                
//                int tenantId = Integer.parseInt(tenantRange);
//                
//                tenantDomainContextMap.put(tenantId, new TenantDomainContext(tenantId, domain, subDomain));
//                
//            } catch (NumberFormatException e) {
//                
//                log.error(msg, e);
//                throw new RuntimeException(msg, e);
//            }
//
//        } else if (parsedLine.length == 2) {
//            
//            try {
//                int startIndex = Integer.parseInt(parsedLine[0]);
//                int endIndex = Integer.parseInt(parsedLine[1]);
//                
//                for (int tenantId = startIndex; tenantId <= endIndex; tenantId++) {
//                    
//                    tenantDomainContextMap.put(tenantId, new TenantDomainContext(tenantId, domain, subDomain));
//                    
//                }
//            } catch (NumberFormatException e) {
//                log.error(msg, e);
//                throw new RuntimeException(msg, e);
//            }
//            
//        } else {
//            log.error(msg);
//            throw new RuntimeException(msg);
//        }
//
//    }
//
////    private void addToTenantDomainRangeContextMap(
////                                                  String domain, String subDomain,
////                                                  String tenantRange) {
////
////        Map<String, String> map ;
////        
////        if(tenantDomainRangeContextMap.containsKey(domain)){
////            map = tenantDomainRangeContextMap.get(domain);
////        }
////        else{
////            map = new HashMap<String, String>();
////        }
////        // put this tenant range
////        map.put(subDomain, tenantRange);
////        
////        // update the parent map
////        tenantDomainRangeContextMap.put(domain, map);
////    }
////
////    public Map<Integer, String> getTenantIDClusterDomainMap() {
////        return this.tenantIDClusterDomainMap;
////    }
////    
////    public Map<Integer, String> getTenantIDClusterSubDomainMap() {
////        return this.tenantIDClusterSubDomainMap;
////    }
//
//    /**
//     * Given a tenant id, this will return its domain.
//     * @param id tenant id
//     * @return domain which this tenant belongs to.
//     */
//    public String getClusterDomainFromTenantId(int id) {
//        
//        if(tenantDomainContextMap.get(id) == null){
//            // if there's no specific domain for this tenant, we will redirect it to the default cluster
//            return tenantDomainContextMap.get(0).getDomain();
//        }
//        
//        return tenantDomainContextMap.get(id).getDomain();
//        
//    }
//    
//    /**
//     * Given a tenant id, this will return its sub domain.
//     * @param id tenant id
//     * @return sub domain which this tenant belongs to.
//     */
//    public String getClusterSubDomainFromTenantId(int id) {
//        
//        if(tenantDomainContextMap.get(id) == null){
//            // if there's no specific domain for this tenant, we will redirect it to the default cluster
//            return tenantDomainContextMap.get(0).getSubDomain();
//        }
//        
//        return tenantDomainContextMap.get(id).getSubDomain();
//    }
//
//    public Map<Integer, TenantDomainContext> getTenantDomainContextMap() {
//        return tenantDomainContextMap;
//    }
//}
