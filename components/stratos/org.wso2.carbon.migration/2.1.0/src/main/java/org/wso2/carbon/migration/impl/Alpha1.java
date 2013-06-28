/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.migration.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.constants.UsageConstants;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.migration.util.Migrator;
import org.wso2.carbon.migration.util.Util;
import org.wso2.carbon.usage.agent.api.SystemMeteringAgent;

public class Alpha1 implements Migrator {

    public static final String OLD_BANDWIDTH_USE_STORE_PATH =
            "/repository/components/org.wso2.carbon.bandwidth-use";
    private static final Log log = LogFactory.getLog(Alpha1.class);

    public void migrate() throws Exception {
        // changes to the data model for the statics.
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        SystemMeteringAgent meteringAgent = Util.getSystemMeteringAgent();

        //get super tenant governance registry
        UserRegistry superTenantGovernanceRegistry = Util.getSuperTenantGovernanceSystemRegistry();
        if (!superTenantGovernanceRegistry.resourceExists(OLD_BANDWIDTH_USE_STORE_PATH)) {
            return;
        }

        Tenant[] tenants = tenantManager.getAllTenants();
        // uses only the current year month 2010-july
        String yearMonth = CommonUtil.getCurrentMonthString();
        for (Tenant tenant : tenants) {
            int tenantId = tenant.getId();
            long incomingBw = getOldIncomingRegistryBandwidth(yearMonth, tenantId);
            long outgoingBw = getOldOutgoingRegistryBandwidth(yearMonth, tenantId);

            if (incomingBw != 0) {
                meteringAgent.persistUsage(tenantId, yearMonth,
                        UsageConstants.REGISTRY_INCOMING_BW, incomingBw + "");
            }
            if (outgoingBw != 0) {
                meteringAgent.persistUsage(tenantId, yearMonth,
                        UsageConstants.REGISTRY_OUTGOING_BW, outgoingBw + "");
            }
            log.info("Migrating registry bandwidth data to the new data model. tenant id: " +
                    tenantId + ".");
        }
        superTenantGovernanceRegistry.delete(OLD_BANDWIDTH_USE_STORE_PATH);
    }

    public long getOldIncomingRegistryBandwidth(String yearMonth, int tenantId)
            throws RegistryException {
        UserRegistry superTenantGovernanceRegistry = Util.getSuperTenantGovernanceSystemRegistry();

        String incomingBandwidthUserCollectionPath =
                OLD_BANDWIDTH_USE_STORE_PATH + RegistryConstants.PATH_SEPARATOR + tenantId;
        if (!superTenantGovernanceRegistry.resourceExists(incomingBandwidthUserCollectionPath)) {
            return 0;
        }

        Resource incomingBandwidthUserCollectionR =
                superTenantGovernanceRegistry.get(incomingBandwidthUserCollectionPath);
        if (!(incomingBandwidthUserCollectionR instanceof Collection)) {
            String msg = "incoming bandwidth user collection is not a collection";
            log.error(msg);
            throw new RegistryException(msg);
        }

        Collection incomingBandwidthUserCollection = (Collection) incomingBandwidthUserCollectionR;
        String[] incomingBandwidthUserPaths = incomingBandwidthUserCollection.getChildren();

        long totalIncomingBandwidth = 0;
        for (String incomingBandwidthUserPath : incomingBandwidthUserPaths) {
            String incomingBandwidthResourcePath =
                    incomingBandwidthUserPath + RegistryConstants.PATH_SEPARATOR + yearMonth +
                            RegistryConstants.PATH_SEPARATOR + StratosConstants.INCOMING_PATH_DIR;

            if (!superTenantGovernanceRegistry.resourceExists(incomingBandwidthResourcePath)) {
                return 0;
            }
            Resource incomingBandwidthResource =
                    superTenantGovernanceRegistry.get(incomingBandwidthResourcePath);
            if (incomingBandwidthResource instanceof Collection) {
                String msg = "Incoming bandwidth is not stored as a non collection.";
                log.error(msg);
                throw new RegistryException(msg);
            }
            byte[] contentBytes = (byte[]) incomingBandwidthResource.getContent();
            if (contentBytes != null) {
                String contentStr = new String(contentBytes);
                try {
                    totalIncomingBandwidth += Integer.parseInt(contentStr);
                } catch (NumberFormatException e) {
                    String msg = "Error in parsing the integer string: " + contentStr;
                    log.error(msg, e);
                    throw new RegistryException(msg, e);
                }
            }
        }
        return totalIncomingBandwidth;
    }

    public long getOldOutgoingRegistryBandwidth(String yearMonth, int tenantId)
            throws RegistryException {
        UserRegistry superTenantGovernanceRegistry = Util.getSuperTenantGovernanceSystemRegistry();
        String outgoingBandwidthUserCollectionPath =
                OLD_BANDWIDTH_USE_STORE_PATH + RegistryConstants.PATH_SEPARATOR + tenantId;
        if (!superTenantGovernanceRegistry.resourceExists(outgoingBandwidthUserCollectionPath)) {
            return 0;
        }

        Resource outgoingBandwidthUserCollectionR =
                superTenantGovernanceRegistry.get(outgoingBandwidthUserCollectionPath);
        if (!(outgoingBandwidthUserCollectionR instanceof Collection)) {
            String msg = "outgoing bandwidth user collection is not a collection";
            log.error(msg);
            throw new RegistryException(msg);
        }

        Collection outgoingBandwidthUserCollection = (Collection) outgoingBandwidthUserCollectionR;
        String[] outgoingBandwidthUserPaths = outgoingBandwidthUserCollection.getChildren();

        long totalOutgoingBandwidth = 0;
        for (String outgoingBandwidthUserPath : outgoingBandwidthUserPaths) {
            String outgoingBandwidthResourcePath =
                    outgoingBandwidthUserPath + RegistryConstants.PATH_SEPARATOR + yearMonth +
                            RegistryConstants.PATH_SEPARATOR + StratosConstants.OUTGOING_PATH_DIR;

            if (!superTenantGovernanceRegistry.resourceExists(outgoingBandwidthResourcePath)) {
                return 0;
            }
            Resource outgoingBandwidthResource =
                    superTenantGovernanceRegistry.get(outgoingBandwidthResourcePath);
            if (outgoingBandwidthResource instanceof Collection) {
                String msg = "outgoing bandwidth is not stored as a non collection.";
                log.error(msg);
                throw new RegistryException(msg);
            }
            byte[] contentBytes = (byte[]) outgoingBandwidthResource.getContent();
            String contentStr = new String(contentBytes);
            try {
                totalOutgoingBandwidth += Integer.parseInt(contentStr);
            } catch (NumberFormatException e) {
                String msg = "Error in parsing the integer string: " + contentStr;
                log.error(msg, e);
                throw new RegistryException(msg, e);
            }
        }
        return totalOutgoingBandwidth;
    }
}
