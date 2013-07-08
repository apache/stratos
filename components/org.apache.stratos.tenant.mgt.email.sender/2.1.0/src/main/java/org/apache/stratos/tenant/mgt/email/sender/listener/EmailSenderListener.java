package org.apache.stratos.tenant.mgt.email.sender.listener;

import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
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

    public void onTenantInitialActivation(int tenantId) throws StratosException {
     // send the notification message to the tenant admin
        TenantMgtEmailSenderUtil.notifyTenantInitialActivation(tenantId);
    }

    public void onTenantActivation(int tenantId) throws StratosException {
        // Do nothing. 
    }

    public void onTenantDeactivation(int tenantId) throws StratosException {
        // Do nothing. 
    }

    public void onSubscriptionPlanChange(int tenentId, String oldPlan, 
                                         String newPlan) throws StratosException {
        // Do nothing. 
    }

}
