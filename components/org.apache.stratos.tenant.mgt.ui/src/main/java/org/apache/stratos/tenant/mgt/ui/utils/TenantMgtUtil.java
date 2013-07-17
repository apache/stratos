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
package org.apache.stratos.tenant.mgt.ui.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.apache.stratos.tenant.mgt.ui.clients.TenantServiceClient;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Calendar;

/**
 * Utility methods for tenant.mgt.ui
 */
public class TenantMgtUtil {
    private static final Log log = LogFactory.getLog(TenantMgtUtil.class);

    /**
     * Super admin Adds a tenant
     *
     * @param request HttpServletRequest
     * @param config  ServletConfig
     * @param session HttpSession
     * @throws Exception , if error in adding the tenant
     */
    public static void addTenantConfigBean(HttpServletRequest request, ServletConfig config,
                                           HttpSession session) throws Exception {
        TenantInfoBean tenantInfoBean = new TenantInfoBean();

        try {
            tenantInfoBean.setAdmin(request.getParameter("admin"));
            tenantInfoBean.setFirstname(request.getParameter("admin-firstname"));
            tenantInfoBean.setLastname(request.getParameter("admin-lastname"));
            tenantInfoBean.setAdminPassword(request.getParameter("admin-password"));
            tenantInfoBean.setTenantDomain(request.getParameter("domain"));
            tenantInfoBean.setEmail(request.getParameter("admin-email"));
            tenantInfoBean.setUsagePlan(request.getParameter("usage-plan-name"));
            tenantInfoBean.setCreatedDate(Calendar.getInstance());
            TenantServiceClient serviceClient = new TenantServiceClient(config, session);
            serviceClient.addTenant(tenantInfoBean);
            
        } catch (Exception e) {
            String msg = "Failed to add tenant config. tenant-domain: "
                    + tenantInfoBean.getTenantDomain() + ", " + "tenant-admin: "
                    + tenantInfoBean.getAdmin() + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Super admin Updates a tenant
     *
     * @param request HttpServletRequest
     * @param config  ServletConfig
     * @param session HttpSession
     * @throws Exception , if error in updating the tenant
     */
    public static void updateTenantConfigBean(HttpServletRequest request, ServletConfig config,
                                              HttpSession session) throws Exception {
        TenantInfoBean tenantInfoBean = new TenantInfoBean();

        try {
            String tenantIdStr = request.getParameter("tenantId");
            int tenantId;
            try {
                tenantId = Integer.parseInt(tenantIdStr);
            } catch (Exception e) {
                String msg = "Error in converting tenant id: " + tenantIdStr + " to a number.";
                log.error(msg);
                throw new Exception(msg, e);
            }
            tenantInfoBean.setTenantId(tenantId);
            tenantInfoBean.setAdmin(request.getParameter("admin"));
            tenantInfoBean.setFirstname(request.getParameter("admin-firstname"));
            tenantInfoBean.setLastname(request.getParameter("admin-lastname"));
            tenantInfoBean.setAdminPassword(request.getParameter("admin-password"));
            tenantInfoBean.setTenantDomain(request.getParameter("domain"));
            tenantInfoBean.setEmail(request.getParameter("admin-email"));
            tenantInfoBean.setUsagePlan(request.getParameter("usage-plan-name"));
            TenantServiceClient serviceClient = new TenantServiceClient(config, session);
            serviceClient.updateTenant(tenantInfoBean);
            //UsagePlanClient usagePlanClient = new UsagePlanClient(config, session);
            //update usage plan(subscription) per tenant
            //usagePlanClient.updateUsagePlan(tenantInfoBean);
        } catch (Exception e) {
            String msg = "Failed to update the tenant config. tenant-domain: "
                    + tenantInfoBean.getTenantDomain() + ", " + "tenant-admin: "
                    + tenantInfoBean.getAdmin() + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Super admin gets all the tenant
     *
     * @param request HttpServletRequest
     * @param config  ServletConfig
     * @param session HttpSession
     * @return TenantInfoBean[] - Array of tenants
     * @throws Exception , if getting the tenant information failed.
     */
    public static TenantInfoBean[] getTenants(HttpServletRequest request, ServletConfig config,
                                              HttpSession session) throws Exception {

        try {

            TenantServiceClient serviceClient = new TenantServiceClient(config, session);
            return serviceClient.retrieveTenants();
        } catch (Exception e) {
            String msg = "Failed to get the minimum information bean of tenants. ";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Super admin gets a particular tenant
     *
     * @param request HttpServletRequest
     * @param config  ServletConfig
     * @param session HttpSession
     * @return TenantInfoBean - for a tenant
     * @throws Exception , if error in getting the tenant
     */
    public static TenantInfoBean getTenant(HttpServletRequest request, ServletConfig config,
                                           HttpSession session) throws Exception {
        String tenantDomain = "";
        try {
            tenantDomain = request.getParameter("domain");
            TenantServiceClient serviceClient = new TenantServiceClient(config, session);
            TenantInfoBean tenantBean=serviceClient.getTenant(tenantDomain);
            return tenantBean;
        } catch (Exception e) {
            String msg = "Failed to get existing details of the tenant:" + tenantDomain;
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Super admin activates a tenant
     *
     * @param request HttpServletRequest
     * @param config  ServletConfig
     * @param session HttpSession
     * @throws Exception , if failed to activate the tenant.
     */
    public static void activateTenant(HttpServletRequest request, ServletConfig config,
                                      HttpSession session) throws Exception {
        String tenantDomain = "";
        try {
            tenantDomain = request.getParameter("activate.domain");
            TenantServiceClient serviceClient = new TenantServiceClient(config, session);
            serviceClient.activateTenant(tenantDomain);
        } catch (Exception e) {
            String msg = "Failed to activate the tenant:" + tenantDomain;
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Super admin deactivates a tenant
     *
     * @param request HttpServletRequest
     * @param config  ServletConfig
     * @param session HttpSession
     * @throws Exception , if failed to deactivate the tenant
     */
    public static void deactivateTenant(HttpServletRequest request, ServletConfig config,
                                        HttpSession session) throws Exception {
        String tenantDomain = "";
        try {
            tenantDomain = request.getParameter("activate.domain");
            TenantServiceClient serviceClient = new TenantServiceClient(config, session);
            serviceClient.deactivateTenant(tenantDomain);
        } catch (Exception e) {
            String msg = "Failed to deactivate the tenant:" + tenantDomain;
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * This is used to avoid xss attacks
     *
     * @param text the text
     * @return the text encoding '<' and '>' elements
     */
    public static String removeHtmlElements(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }
}
