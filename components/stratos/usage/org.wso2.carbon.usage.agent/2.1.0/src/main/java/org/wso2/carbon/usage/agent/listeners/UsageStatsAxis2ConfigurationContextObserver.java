package org.wso2.carbon.usage.agent.listeners;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;


public class UsageStatsAxis2ConfigurationContextObserver extends AbstractAxis2ConfigurationContextObserver {

    private static final Log log = LogFactory.getLog(UsageStatsAxis2ConfigurationContextObserver.class);

    @Override
    public void createdConfigurationContext(ConfigurationContext configContext) {

        AxisConfiguration axisConfiguration = configContext.getAxisConfiguration();
        int tenantId = PrivilegedCarbonContext.getCurrentContext().getTenantId(false);
        try {
            axisConfiguration.engageModule("metering");
        } catch (AxisFault axisFault) {
            log.error("Could not engage metering module for tenant: " + tenantId, axisFault);
        }


    }

}
