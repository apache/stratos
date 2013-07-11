/*
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the
 *"License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.apache.stratos.usage.agent.listeners;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.stratos.usage.agent.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.util.SystemFilter;
import org.wso2.carbon.statistics.services.util.SystemStatistics;
import org.apache.stratos.usage.agent.util.PublisherUtils;
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
