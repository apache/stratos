/*
 * Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.usage.agent.config;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class UsageAgentConfiguration {
    private static final Log log = LogFactory.getLog(UsageAgentConfiguration.class);

    private static final Integer DEFAULT_NUMBER_OF_RECORDS_PER_RUN = 100;
    private static final Integer DEFAULT_EXECUTION_INTERVAL_IN_MILLISECONDS = 100;
    private static final Integer DEFAULT_STARTUP_DELAY_IN_MILLISECONDS = 60000;

    private int usageTasksNumberOfRecordsPerExecution = -1;

    private int usageTasksExecutionIntervalInMilliSeconds = -1;

    private int usageTasksStartupDelayInMilliSeconds = -1;

    public UsageAgentConfiguration(File configFile) {
        if (configFile.exists()) {
            try {
                OMElement usageAndThrottlingAgentConfiguration =
                        new StAXOMBuilder(new FileInputStream(configFile)).getDocumentElement();
                if (usageAndThrottlingAgentConfiguration != null) {
                    OMElement usageAgent = usageAndThrottlingAgentConfiguration.getFirstChildWithName(
                            new QName("http://wso2.com/carbon/multitenancy/usage-throttling-agent/config", "UsageAgent"));
                    if (usageAgent != null) {
                        OMElement usageDataPersistenceTaskConfig = usageAgent.getFirstChildWithName(
                                new QName("http://wso2.com/carbon/multitenancy/usage-throttling-agent/config",
                                        "UsageDataPersistenceTask"));
                        if (usageDataPersistenceTaskConfig != null) {
                            OMElement numberOfRecordsPerExecutionEle = usageDataPersistenceTaskConfig.getFirstChildWithName(
                                    new QName("http://wso2.com/carbon/multitenancy/usage-throttling-agent/config",
                                            "NumberOfRecordsPerExecution"));
                            if (numberOfRecordsPerExecutionEle != null && numberOfRecordsPerExecutionEle.getText() != null &&
                                    numberOfRecordsPerExecutionEle.getText().length() > 0) {
                                try {
                                    usageTasksNumberOfRecordsPerExecution = Integer.parseInt(numberOfRecordsPerExecutionEle.getText());
                                } catch (NumberFormatException ne) {
                                    log.error("Error while parsing usage persistence task number of records value.", ne);
                                }
                            }

                            OMElement executionIntervalInMilliSeconds = usageDataPersistenceTaskConfig.getFirstChildWithName(
                                    new QName("http://wso2.com/carbon/multitenancy/usage-throttling-agent/config",
                                            "ExecutionIntervalInMilliSeconds"));
                            if (executionIntervalInMilliSeconds != null && executionIntervalInMilliSeconds.getText() != null &&
                                    executionIntervalInMilliSeconds.getText().length() > 0) {
                                try {
                                    usageTasksExecutionIntervalInMilliSeconds =
                                            Integer.parseInt(executionIntervalInMilliSeconds.getText());
                                } catch (NumberFormatException ne) {
                                    log.error("Error while parsing usage persistence task  execution interval value.", ne);
                                }
                            }

                            OMElement startupDelayInMilliSeconds = usageDataPersistenceTaskConfig.getFirstChildWithName(
                                    new QName("http://wso2.com/carbon/multitenancy/usage-throttling-agent/config",
                                            "StartupDelayInMilliSeconds"));
                            if (startupDelayInMilliSeconds != null && startupDelayInMilliSeconds.getText() != null &&
                                    startupDelayInMilliSeconds.getText().length() > 0) {
                                try {
                                    usageTasksStartupDelayInMilliSeconds =
                                            Integer.parseInt(startupDelayInMilliSeconds.getText());
                                } catch (NumberFormatException ne) {
                                    log.error("Error while parsing usage persistence task startup delay value.", ne);
                                }
                            }
                        }
                    }
                }

            } catch (FileNotFoundException e) {
                log.error("Cannot find " + configFile.getAbsolutePath(), e);
            } catch (XMLStreamException e) {
                log.error("Error reading XML stream of file " + configFile.getAbsolutePath(), e);
            }
        }
    }

    public int getUsageTasksNumberOfRecordsPerExecution() {
        return usageTasksNumberOfRecordsPerExecution;
    }

    public int getUsageTasksExecutionIntervalInMilliSeconds() {
        return usageTasksExecutionIntervalInMilliSeconds;
    }

    public int getUsageTasksStartupDelayInMilliSeconds() {
        return usageTasksStartupDelayInMilliSeconds;
    }
}
