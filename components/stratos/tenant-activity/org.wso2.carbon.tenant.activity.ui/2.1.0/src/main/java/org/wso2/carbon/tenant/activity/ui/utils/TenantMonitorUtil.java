/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.tenant.activity.ui.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.config.CloudServiceConfig;
import org.wso2.carbon.stratos.common.config.CloudServiceConfigParser;
import org.wso2.carbon.stratos.common.config.CloudServicesDescConfig;

import java.util.*;

import org.wso2.carbon.tenant.activity.stub.beans.xsd.PaginatedTenantDataBean;
import org.wso2.carbon.tenant.activity.stub.beans.xsd.TenantDataBean;

public class TenantMonitorUtil {
    private static final Log log = LogFactory.getLog(TenantMonitorUtil.class);

    public static Map<String, String[]> tenantList = new HashMap<String, String[]>();
    public static Map<String, TenantDataBean[]> tenantDataList = new HashMap<String, TenantDataBean[]>();


    private static ManagerConfigurations managerConfig = null;


    public static ManagerConfigurations getManagerConfig() {
        return managerConfig;
    }

    public static void setManagerConfig(ManagerConfigurations managerConfig) {
        TenantMonitorUtil.managerConfig = managerConfig;
    }

  /*  public static Map<String, Integer> getActiveTenantCount(ServletConfig config, HttpSession session) throws Exception {
        Map<String, Integer> map = new HashMap<String, Integer>();
        try {
            CloudServicesDescConfig cloudServicesDescConfig = CloudServiceConfigParser.loadCloudServicesConfiguration();

            Map<String, CloudServiceConfig> cloudServicesConfigs = cloudServicesDescConfig.getCloudServiceConfigs();
            for (String serviceName : cloudServicesConfigs.keySet()) {
                String backEndURL = cloudServicesConfigs.get(serviceName).getLink();
                System.out.println(backEndURL);
                if (backEndURL == null) {
                    try {
                        TenantActivityServiceClient client = new TenantActivityServiceClient(config, session);
                        map.put(serviceName, client.getActiveTenantCount());
                        for (String nn : client.getActiveTenantList()) {
                            System.out.println(nn);
                        }
                    } catch (Exception e) {
                        log.error("Failed to get active tenants for manager service");
                    }

                } else {
                    try {
                        TenantActivityServiceClient client = new TenantActivityServiceClient(backEndURL, config, session);
                        map.put(serviceName, client.getActiveTenantCount());
                    } catch (Exception e) {
                        log.error("failed to get Active tenants for" + serviceName + e.toString());
                    }
                }

            }
        } catch (Exception e) {
            log.error("Error while retrieving cloud desc configuration");

        }
        return map;
    }*/

    public static Map<String, CloudServiceConfig> getCloudServiceConfigMap() {
        try {
            CloudServicesDescConfig cloudServicesDescConfig = CloudServiceConfigParser.loadCloudServicesConfiguration();
            return cloudServicesDescConfig.getCloudServiceConfigs();
        } catch (Exception e) {
            log.error("Error while getting service names " + e.toString());
        }
        return null;
    }

    public static PaginatedTenantDataBean getPaginatedTenantData(int pageNumber, String serviceName) {
        int entriesPerPage = 15;
        List<TenantDataBean> tenantListOnService = Arrays.asList(tenantDataList.get(serviceName));
        List<TenantDataBean> tenantUsages = new ArrayList<TenantDataBean>();
        int i = 0;
        int numberOfPages = 0;
        for (TenantDataBean tenant : tenantListOnService) {
            if (i % entriesPerPage == 0) {
                numberOfPages++;
            }
            if (numberOfPages == pageNumber) {
                tenantUsages.add(tenant);
            }
            i++;

        }
        PaginatedTenantDataBean paginatedTenantInfo = new PaginatedTenantDataBean();
        paginatedTenantInfo.setTenantInfoBeans(
                tenantUsages.toArray(new TenantDataBean[tenantUsages.size()]));
        paginatedTenantInfo.setNumberOfPages(numberOfPages);
        return paginatedTenantInfo;
    }

    public static boolean isTenantActiveOnService(String serviceName, String domain) {
        boolean status = false;
        for (TenantDataBean tenantBean : tenantDataList.get(serviceName)) {
            if (tenantBean.getDomain().equalsIgnoreCase(domain)) {
                status = true;
            }
        }
        return status;
    }

    public static Map<String, String> getAdminParameters() {
        Map<String, String> adminParameters = new HashMap<String, String>();
        if (managerConfig == null) {
            try {
                managerConfig = new ManagerConfigurations();
            } catch (Exception e) {
                log.error("Failed to get administrator credentials" + e.toString());
            }
        }
        adminParameters.put("userName", managerConfig.getUserName());
        adminParameters.put("password", managerConfig.getPassword());
        return adminParameters;
    }
}
