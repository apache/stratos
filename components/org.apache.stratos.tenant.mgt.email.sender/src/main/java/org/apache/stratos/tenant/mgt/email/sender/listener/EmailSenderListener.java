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
package org.apache.stratos.tenant.mgt.email.sender.listener;

import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.exception.StratosException;
import org.apache.stratos.common.listeners.TenantMgtListener;
import org.apache.stratos.tenant.mgt.email.sender.util.TenantMgtEmailSenderUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EmailSenderListener implements TenantMgtListener {
    
    private static final int EXEC_ORDER = 20;
    private static final Log log = LogFactory.getLog(EmailSenderListener.class);

    public void onTenantCreate(TenantInfoBean tenantInfoBean) throws StratosException {
        try {
            TenantMgtEmailSenderUtil.sendTenantCreationVerification(tenantInfoBean);
        } catch (Exception e) {
            String message = "Error sending tenant creation Mail to tenant domain " 
                + tenantInfoBean.getTenantDomain();
            log.error(message, e);
            throw new StratosException(message, e);
        }
        TenantMgtEmailSenderUtil.notifyTenantCreationToSuperAdmin(tenantInfoBean);
    }

    public int getListenerOrder() {
        return EXEC_ORDER;
    }

    public void onTenantRename(int tenantId, String oldDomainName, 
                             String newDomainName) throws StratosException {
        // Do nothing. 

    }

    public void onTenantUpdate(TenantInfoBean tenantInfoBean) throws StratosException {
        if ((tenantInfoBean.getAdminPassword() != null) && 
                (!tenantInfoBean.getAdminPassword().equals(""))) {
            try {
                TenantMgtEmailSenderUtil.notifyResetPassword(tenantInfoBean);
            } catch (Exception e) {
                String message = "Error sending tenant update Mail to tenant domain " 
                    + tenantInfoBean.getTenantDomain();
                log.error(message, e);
                throw new StratosException(message, e);
            }
        }
    }

    public void onTenantDelete(int tenantId) {
        // Do nothing
    }

    public void onTenantInitialActivation(int tenantId) throws StratosException {
     // send the notification message to the tenant admin
        TenantMgtEmailSenderUtil.notifyTenantInitialActivation(tenantId);
    }

    public void onTenantActivation(int tenantId) throws StratosException {
        // Do nothing
    }

    public void onTenantDeactivation(int tenantId) throws StratosException {
        // Do nothing
    }

    public void onSubscriptionPlanChange(int tenentId, String oldPlan, 
                                         String newPlan) throws StratosException {
        // Do nothing
    }

}
