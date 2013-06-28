package org.wso2.carbon.stratos.common;

import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.user.api.Tenant;

/**
 * The OSGI service interface that enables tenant related billing actions.
 */
public interface TenantBillingService {
    
    public void addUsagePlan(Tenant tenant, String usagePlan) throws StratosException;
    
    public String getActiveUsagePlan(String tenantDomain) throws StratosException;
    
    public void updateUsagePlan(String tenantDomain, String usagePlan) throws StratosException;
    
    public void activateUsagePlan(String tenantDomain) throws StratosException;
    
    public void deactivateActiveUsagePlan(String tenantDomain) throws StratosException;

    public void deleteBillingData(int tenantId) throws StratosException;

    
}
