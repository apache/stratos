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

package org.apache.stratos.manager.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.statistics.publisher.ThriftClientConfig;
import org.apache.stratos.common.statistics.publisher.ThriftStatisticsPublisher;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.manager.utils.StratosManagerConstants;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Application signup data publisher.
 */
public class DASApplicationSignUpDataPublisher extends ThriftStatisticsPublisher {

    private static final Log log = LogFactory.getLog(DASApplicationSignUpDataPublisher.class);

    private static final String DATASTREAM_NAME = "application_signups";
    private static final String DATASTREAM_NICKNAME = "Application Signup Statistics";
    private static final String DATASTREAM_DESC = "Application signup statistics for generating metering information.";
    private static final String VERSION = "1.0.0";
    private static final String APPLICATION_ID = "application_id";
    private static final String TENANT_ID = "tenant_id";
    private static final String TENANT_DOMAIN = "tenant_domain";
    private static final String START_TIME = "start_time";
    private static final String END_TIME = "end_time";
    private static final String DURATION = "duration";

    private ExecutorService executorService;

    private static DASApplicationSignUpDataPublisher instance;

    public enum SignUpAction {Added, Removed}

    /**
     * Constructor for initializing the data publisher.
     */
    private DASApplicationSignUpDataPublisher() {
        super(getStreamDefinition(), ThriftClientConfig.DAS_THRIFT_CLIENT_NAME);
        int threadPoolSize = Integer.getInteger(StratosManagerConstants.STATS_PUBLISHER_THREAD_POOL_ID,
                StratosManagerConstants.STATS_PUBLISHER_THREAD_POOL_SIZE);
        executorService = StratosThreadPool
                .getExecutorService(StratosManagerConstants.STATS_PUBLISHER_THREAD_POOL_ID, threadPoolSize);
    }

    public static DASApplicationSignUpDataPublisher getInstance() {
        if (instance == null) {
            synchronized (DASApplicationSignUpDataPublisher.class) {
                if (instance == null) {
                    instance = new DASApplicationSignUpDataPublisher();
                }
            }
        }
        return instance;
    }

    private static StreamDefinition getStreamDefinition() {
        try {
            // Create stream definition
            StreamDefinition streamDefinition = new StreamDefinition(DATASTREAM_NAME, VERSION);
            streamDefinition.setNickName(DATASTREAM_NICKNAME);
            streamDefinition.setDescription(DATASTREAM_DESC);
            List<Attribute> payloadData = new ArrayList<Attribute>();

            // Set payload definition
            payloadData.add(new Attribute(APPLICATION_ID, AttributeType.STRING));
            payloadData.add(new Attribute(TENANT_ID, AttributeType.INT));
            payloadData.add(new Attribute(TENANT_DOMAIN, AttributeType.STRING));
            payloadData.add(new Attribute(START_TIME, AttributeType.LONG));
            payloadData.add(new Attribute(END_TIME, AttributeType.LONG));
            payloadData.add(new Attribute(DURATION, AttributeType.LONG));

            streamDefinition.setPayloadData(payloadData);
            return streamDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Could not create stream definition", e);
        }
    }

    /**
     * Publish application signup statistics
     *
     * @param applicationId
     * @param tenantId
     * @param tenantDomain
     * @param startTime
     * @param endTime
     * @param duration
     */
    public void publish(final String applicationId, final int tenantId, final String tenantDomain,
            final long startTime, final long endTime, final long duration) {
        Runnable publisher = new Runnable() {
            @Override public void run() {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Publishing application signup statistics: [application_id] %s "
                                    + "[tenant_id] %d [tenant_domain] %s [start_time] %d [end_time] %d "
                                    + "[duration] %d ", applicationId, tenantId, tenantDomain, startTime, endTime,
                            duration));
                }
                //adding payload data
                List<Object> payload = new ArrayList<Object>();
                payload.add(applicationId);
                payload.add(tenantId);
                payload.add(tenantDomain);
                payload.add(startTime);
                payload.add(endTime);
                payload.add(duration);
                publish(payload.toArray());
            }
        };
        executorService.execute(publisher);
    }
}
