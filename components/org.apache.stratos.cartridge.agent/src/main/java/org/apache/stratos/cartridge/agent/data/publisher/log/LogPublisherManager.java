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

package org.apache.stratos.cartridge.agent.data.publisher.log;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.data.publisher.DataPublisherConfiguration;
import org.apache.stratos.cartridge.agent.data.publisher.exception.DataPublisherException;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;

public class LogPublisherManager {

    private static final Log log = LogFactory.getLog(LogPublisherManager.class);

    private static DataPublisherConfiguration dataPublisherConfig = null;
    private static StreamDefinition streamDefinition = null;
    private static List<LogPublisher> fileBasedLogPublishers = new ArrayList<LogPublisher>();

    public void init (DataPublisherConfiguration dataPublisherConfig) throws DataPublisherException {

        this.dataPublisherConfig = dataPublisherConfig;

        List<Integer> ports = new ArrayList<Integer>();
        ports.add(Integer.parseInt(dataPublisherConfig.getMonitoringServerPort()));
        ports.add(Integer.parseInt(dataPublisherConfig.getMonitoringServerSecurePort()));

        // wait till monitoring server ports are active
        CartridgeAgentUtils.waitUntilPortsActive(dataPublisherConfig.getMonitoringServerIp(), ports);
        if(!CartridgeAgentUtils.checkPortsActive(dataPublisherConfig.getMonitoringServerIp(), ports)) {
            throw new DataPublisherException("Monitoring server not active, data publishing is aborted");
        }

        // stream definition identifier = {log.publisher.<cluster id>}
        try {
        	streamDefinition = new StreamDefinition(Constants.LOG_PUBLISHER_STREAM_PREFIX + getValidTenantId(CartridgeAgentConfiguration.getInstance().getTenantId()) + "." + getAlias(CartridgeAgentConfiguration.getInstance().getClusterId()) + "." + getCurrentDate(),
                    Constants.LOG_PUBLISHER_STREAM_VERSION);

        } catch (MalformedStreamDefinitionException e) {
            throw new RuntimeException(e);
        }

        streamDefinition.setDescription("Apache Stratos Instance Log Publisher");

        List<Attribute> metaDataDefinition = new ArrayList<Attribute>();
        metaDataDefinition.add(new Attribute(Constants.MEMBER_ID, AttributeType.STRING));

        List<Attribute> payloadDataDefinition = new ArrayList<Attribute>();
        payloadDataDefinition.add(new Attribute(Constants.TENANT_ID, AttributeType.STRING));
        payloadDataDefinition.add(new Attribute(Constants.SERVER_NAME, AttributeType.STRING));
        payloadDataDefinition.add(new Attribute(Constants.APP_NAME, AttributeType.STRING));
        payloadDataDefinition.add(new Attribute(Constants.LOG_TIME, AttributeType.LONG));
        payloadDataDefinition.add(new Attribute(Constants.PRIORITY, AttributeType.STRING));
        payloadDataDefinition.add(new Attribute(Constants.MESSAGE, AttributeType.STRING));
        payloadDataDefinition.add(new Attribute(Constants.LOGGER, AttributeType.STRING));
        payloadDataDefinition.add(new Attribute(Constants.IP, AttributeType.STRING));
        payloadDataDefinition.add(new Attribute(Constants.INSTANCE, AttributeType.STRING));
        payloadDataDefinition.add(new Attribute(Constants.STACKTRACE, AttributeType.STRING));
                 
        streamDefinition.setMetaData(metaDataDefinition);
        streamDefinition.setPayloadData(payloadDataDefinition);
    }

    public void start (String filePath) throws DataPublisherException {

        File logFile = new File (filePath);
        if (!logFile.exists() || !logFile.canRead() || logFile.isDirectory()) {
            throw new DataPublisherException("Unable to read the file at path " + filePath);
        }

        LogPublisher fileBasedLogPublisher = new FileBasedLogPublisher(dataPublisherConfig, streamDefinition, filePath,
        		CartridgeAgentConfiguration.getInstance().getMemberId(),
        		getValidTenantId(CartridgeAgentConfiguration.getInstance().getTenantId()),
        		getAlias(CartridgeAgentConfiguration.getInstance().getClusterId()),
        		getDateTime());

        fileBasedLogPublisher.initialize();
        fileBasedLogPublisher.start();

        // add instance to list
        fileBasedLogPublishers.add(fileBasedLogPublisher);
    }

    public void stop () {

       if (dataPublisherConfig.isEnabled()) {
           for (LogPublisher fileBasedLogPublisher : fileBasedLogPublishers) {
               fileBasedLogPublisher.stop();
           }
       }
    }
    
	private String getCurrentDate() {
		Date now = new Date();
		DateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMATTER);
		String formattedDate = formatter.format(now);
		return formattedDate.replace("-", ".");
	}

	private String getAlias(String clusterId) {
		String alias;
		try {
			alias = clusterId.split("\\.")[0];
		} catch (Exception e) {
			e.printStackTrace();
			alias = clusterId;
		}
		return alias;
	}

	private Long getDateTime() {
		Date date = new Date();
		return date.getTime();
	}

	private String getValidTenantId(String tenantId) {
		if (tenantId.equals(String
				.valueOf(MultitenantConstants.INVALID_TENANT_ID))
				|| tenantId.equals(String
						.valueOf(MultitenantConstants.SUPER_TENANT_ID))) {
			return "0";
		}
		return tenantId;
	}
}
