package org.wso2.carbon.usage.agent.listeners;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.init.CarbonServerManager;
import org.wso2.carbon.core.util.SystemFilter;
import org.wso2.carbon.statistics.services.util.SystemStatistics;
import org.wso2.carbon.usage.agent.util.PublisherUtils;
import org.wso2.carbon.usage.agent.util.Util;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;


public class StatisticsOutHandler extends AbstractHandler{

    private static Log log = LogFactory.getLog(StatisticsOutHandler.class);

    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {


        AxisService axisService =  messageContext.getAxisService();
        if(axisService== null || SystemFilter.isFilteredOutService(axisService.getAxisServiceGroup()) ||
                axisService.isClientSide()){

            PrivilegedCarbonContext.destroyCurrentContext();
            return InvocationResponse.CONTINUE;
        }

        if(Util.getSystemStatisticsUtil()==null){

            PrivilegedCarbonContext.destroyCurrentContext();
            return InvocationResponse.CONTINUE;
        }
        SystemStatistics systemStatistics = Util.getSystemStatisticsUtil().getSystemStatistics(messageContext);
        
        int tenantId = MultitenantConstants.INVALID_TENANT_ID;
        tenantId = CarbonContext.getCurrentContext().getTenantId();

        if(tenantId == MultitenantConstants.INVALID_TENANT_ID ||
            tenantId == MultitenantConstants.SUPER_TENANT_ID) {

            PrivilegedCarbonContext.destroyCurrentContext();
            return Handler.InvocationResponse.CONTINUE;
        }

        try {
            PublisherUtils.publish(systemStatistics, tenantId);
        } catch (Exception e) {
            //Logging the complete stacktrace in debug mode
            if(log.isDebugEnabled()){
                log.debug(e);
            }

            log.error("Error occurred while publishing request statistics. Full stacktrace available in debug logs. " + e.getMessage());
        }

        PrivilegedCarbonContext.destroyCurrentContext();
        return InvocationResponse.CONTINUE;
    }
}
