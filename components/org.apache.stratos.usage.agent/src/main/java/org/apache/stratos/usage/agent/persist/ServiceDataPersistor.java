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
package org.apache.stratos.usage.agent.persist;

import org.apache.stratos.usage.agent.beans.BandwidthUsage;
import org.apache.stratos.usage.agent.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.transports.metering.MeteredServletRequest;
import org.wso2.carbon.core.transports.metering.MeteredServletResponse;
import org.wso2.carbon.core.transports.metering.RequestDataPersister;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.constants.UsageConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * this class is used to persist service data
 */
public class ServiceDataPersistor implements RequestDataPersister {
    private static final Log log = LogFactory.getLog(ServiceDataPersistor.class);

    /**
     * this method get tenant id, inDataSize and outDataSize from the wrappedRequest, construct a
     * BandwidthUsage object and add it to PersistingControllerQueue
     * @param wrappedRequest  MeteredServletRequest
     * @param wrappedResponse MeteredServletResponse
     */
    public void persist(MeteredServletRequest wrappedRequest, MeteredServletResponse wrappedResponse) {
        if ("true".equals(wrappedRequest.getAttribute(StratosConstants.SERVICE_NAME_SERVLET_ATTR))) {
            return;
        }

        Integer tenantId = (Integer) wrappedRequest.getAttribute(
                StratosConstants.TENANT_ID_SERVLET_ATTR);
        if (tenantId == null || tenantId == MultitenantConstants.SUPER_TENANT_ID) {
            return;
        }
        long inDataSize = wrappedRequest.getReadSize();
        long outDataSize = wrappedResponse.getWrittenSize();

        if(log.isTraceEnabled()){
            log.trace("Persisting service bandwidth usage for tenant " + tenantId + " in size: " + inDataSize + " out size: " + outDataSize);
        }
        // add the job to queue
        if (inDataSize > 0) {
            BandwidthUsage usage = new BandwidthUsage(tenantId,
                    UsageConstants.SERVICE_INCOMING_BW, inDataSize);
            Util.addToPersistingControllerQueue(usage);
        }
        if (outDataSize > 0) {
            BandwidthUsage usage = new BandwidthUsage(tenantId,
                    UsageConstants.SERVICE_OUTGOING_BW, outDataSize);
            Util.addToPersistingControllerQueue(usage);
        }
    }
}
