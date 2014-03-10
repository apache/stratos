/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.executor.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.data.publisher.DataPublisherConfiguration;
import org.apache.stratos.cartridge.agent.data.publisher.exception.DataPublisherException;
import org.apache.stratos.cartridge.agent.data.publisher.log.LogPublisherManager;
import org.apache.stratos.cartridge.agent.executor.ExtensionExecutor;

/**
 * This extension is suppose to initialize and start log publishers.
 * 
 */
public class LogPublisherExtensionExecutor extends ExtensionExecutor {
	
	private static final Log log = LogFactory.getLog(LogPublisherExtensionExecutor.class);
	private LogPublisherManager logPublisherManager;

	public LogPublisherExtensionExecutor() {
		super(LogPublisherExtensionExecutor.class.getName());
	}
	
	public LogPublisherExtensionExecutor(List<String> fileNames) {
		super.setFileNamesToBeExecuted(fileNames);
	}

	@Override
	public void execute() {
		try {
            if(log.isDebugEnabled()) {
                log.debug("Executing Extension: "+super.getId());
            }
            LogPublisherManager logPublisherManager = new LogPublisherManager();
            publishLogs(logPublisherManager);
        }
		catch (Exception e) {
            log.error("Could not execute extension: "+super.getId() , e);
        }
	}
	
	private void publishLogs (LogPublisherManager logPublisherManager) {

		this.logPublisherManager = logPublisherManager;
		
        // check if enabled
        if (DataPublisherConfiguration.getInstance().isEnabled()) {

            List<String> logFilePaths = CartridgeAgentConfiguration.getInstance().getLogFilePaths();
            if (logFilePaths == null) {
                log.error("No valid log file paths found, no logs will be published");
                return;

            } else {
                // initialize the log publishing
                try {
                    logPublisherManager.init(DataPublisherConfiguration.getInstance());

                } catch (DataPublisherException e) {
                    log.error("Error occurred in log publisher initialization", e);
                    return;
                }

                // start a log publisher for each file path
                for (String logFilePath : logFilePaths) {
                    try {
                        logPublisherManager.start(logFilePath);

                    } catch (DataPublisherException e) {
                        log.error("Error occurred in publishing logs ", e);
                    }
                }
            }
        }
    }

	@Override
	public void cleanUp() {
		logPublisherManager.stop();
	}
}
