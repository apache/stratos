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
package org.apache.stratos.tenant.mgt.realm;

import java.util.Map;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.um.ws.api.WSRealm;
import org.wso2.carbon.um.ws.api.WSRemoteUserMgtConstants;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.TenantMgtConfiguration;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.config.RealmConfigXMLProcessor;
import org.wso2.carbon.user.core.config.multitenancy.MultiTenantRealmConfigBuilder;
import org.wso2.carbon.user.core.jdbc.JDBCRealmConstants;
import org.wso2.carbon.user.core.tenant.Tenant;

/**
 * This class is no more used by cloud manager or elsewhere.
 * Hence deprecated and will be removed eventually.
 */
@Deprecated
public class CloudWSRealmConfigBuilder implements MultiTenantRealmConfigBuilder {

    private static final Log log = LogFactory.getLog(CloudWSRealmConfigBuilder.class);


    /**
     * This method is called on server startup by DefaultRealmService by non-Idaas cloud services
     * 
     * This is not called on ws.api startup.
     */
    public RealmConfiguration getRealmConfigForTenantToCreateRealm(
            RealmConfiguration bootStrapConfig, RealmConfiguration persistedConfig, int tenantId)
            throws UserStoreException {
        RealmConfiguration realmConfig;
        try {
            if (persistedConfig.getRealmClassName().equals(WSRealm.class.getName())) {
                realmConfig = persistedConfig;
            } else {
                realmConfig = bootStrapConfig.cloneRealmConfiguration();
                realmConfig.setTenantId(tenantId);
            }
            if (log.isDebugEnabled()) {
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
        RealmConfiguration realmConfig;
        try {
            realmConfig = bootStrapConfig.cloneRealmConfiguration();
            realmConfig.setRealmClassName("org.wso2.carbon.um.ws.api.WSRealm");
            
            realmConfig.setAdminPassword(UUIDGenerator.getUUID());
            Map<String, String> realmProps = realmConfig.getRealmProperties();
            realmProps.remove(JDBCRealmConstants.URL);
            realmProps.remove(JDBCRealmConstants.DRIVER_NAME);
            realmProps.remove(JDBCRealmConstants.USER_NAME);
            realmProps.remove(JDBCRealmConstants.PASSWORD);
            
            realmConfig.setTenantId(tenantId);

            if (log.isDebugEnabled()) {
                OMElement omElement = RealmConfigXMLProcessor.serialize(realmConfig);
                log.debug("Creating realm from (On tenant creation)**** " + omElement.toString());
            }

        } catch (Exception e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new UserStoreException(msg);
        }
        return realmConfig;
    }


    public RealmConfiguration getRealmConfigForTenantToPersist(RealmConfiguration bootStrapConfig,
                                                               TenantMgtConfiguration tenantMgtConfig,
                                                               Tenant tenantInfo, int tenantId)
            throws UserStoreException {
        RealmConfiguration realmConfig;
        try {
            realmConfig = bootStrapConfig.cloneRealmConfiguration();
            realmConfig.setAdminUserName(tenantInfo.getAdminName());
            realmConfig.setAdminPassword(UUIDGenerator.getUUID());
            Map<String, String> realmProps = realmConfig.getRealmProperties();
            realmProps.remove(JDBCRealmConstants.URL);
            realmProps.remove(JDBCRealmConstants.DRIVER_NAME);
            realmProps.remove(JDBCRealmConstants.USER_NAME);
            realmProps.remove(JDBCRealmConstants.PASSWORD);
            realmProps.remove(WSRemoteUserMgtConstants.SERVER_URL);
            realmProps.remove(WSRemoteUserMgtConstants.USER_NAME);
            realmProps.remove(WSRemoteUserMgtConstants.PASSWORD);
            realmProps.remove(WSRemoteUserMgtConstants.SINGLE_USER_AUTHENTICATION);
            realmProps.put("MultiTenantRealmConfigBuilder", IdaasWSRealmConfigBuilder.class.getName());
            realmConfig.setTenantId(tenantId);
            if (log.isDebugEnabled()) {
                OMElement omElement = RealmConfigXMLProcessor.serialize(realmConfig);
                log.debug("Saving RealmConfiguration **** " + omElement.toString());
            }

        } catch (Exception e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new UserStoreException(msg);
        }
        return realmConfig;
    }

}
