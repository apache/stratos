/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.tenant.mgt.realm;

import java.util.Map;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.TenantMgtConfiguration;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.config.RealmConfigXMLProcessor;
import org.wso2.carbon.user.core.config.multitenancy.MultiTenantRealmConfigBuilder;
import org.wso2.carbon.user.core.jdbc.JDBCRealmConstants;
import org.wso2.carbon.user.core.tenant.Tenant;

public class IdaasWSRealmConfigBuilder implements MultiTenantRealmConfigBuilder {
    
    private static final Log log = LogFactory.getLog(CloudWSRealmConfigBuilder.class);

    /**
     * This method is called, on server startup by DefaultRealmService by Idaas only
     * 
     * This is not called on ws.api startup or by any non-Idaas servers
     */
    public RealmConfiguration getRealmConfigForTenantToCreateRealm(
            RealmConfiguration bootStrapConfig, RealmConfiguration persistedConfig, int tenantId)
            throws UserStoreException {
        RealmConfiguration realmConfig;
        try {
                realmConfig = persistedConfig;
               // now this is Idaas
                Map<String, String> realmProps = realmConfig.getRealmProperties();
                Map<String, String> bootStrapProps = bootStrapConfig.getRealmProperties();
                realmProps.put(JDBCRealmConstants.URL, bootStrapProps.get(JDBCRealmConstants.URL));
                realmProps.put(JDBCRealmConstants.DRIVER_NAME, bootStrapProps.get(
                        JDBCRealmConstants.DRIVER_NAME));
                realmProps.put(JDBCRealmConstants.USER_NAME, bootStrapProps.get(
                        JDBCRealmConstants.USER_NAME));
                realmProps.put(JDBCRealmConstants.PASSWORD, bootStrapProps.get(
                        JDBCRealmConstants.PASSWORD));
                realmConfig.setTenantId(tenantId);

                if(log.isDebugEnabled()) {
                    OMElement omElement = RealmConfigXMLProcessor.serialize(realmConfig);
                    log.debug("Creating realm from **** " + omElement.toString());
                }
                
        } catch (Exception e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new UserStoreException(msg);
        }
        return realmConfig;
    }
    
    public RealmConfiguration getRealmConfigForTenantToCreateRealmOnTenantCreation(
            RealmConfiguration bootStrapConfig, RealmConfiguration persistedConfig, int tenantId)
            throws UserStoreException{
        //never called
        return null;
    }

    public RealmConfiguration getRealmConfigForTenantToPersist(RealmConfiguration bootStrapConfig,
                                                               TenantMgtConfiguration tenantMgtConfig,
                                                               Tenant tenantInfo, int tenantId)
            throws UserStoreException {
        //never called
        return null;
    }
}