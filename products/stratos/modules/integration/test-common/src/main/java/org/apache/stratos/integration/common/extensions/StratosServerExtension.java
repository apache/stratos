/*
 * Copyright 2005-2015 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.stratos.integration.common.extensions;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.stratos.common.test.TestLogAppender;
import org.apache.stratos.integration.common.StratosTestServerManager;
import org.apache.stratos.integration.common.Util;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.engine.extensions.ExecutionListenerExtension;
import org.wso2.carbon.automation.extensions.ExtensionConstants;

import java.io.File;

import static org.testng.Assert.assertNotNull;

public class StratosServerExtension extends ExecutionListenerExtension {
    private static final Log log = LogFactory.getLog(StratosServerExtension.class);
    public static final String PATH_SEP = File.separator;
    private TestLogAppender testLogAppender;
    private StratosTestServerManager carbonTestServerManager;
    private BrokerService broker;

    @Override
    public void initiate() throws AutomationFrameworkException {
        broker = new BrokerService();
        testLogAppender = new TestLogAppender();
    }

    @Override
    public void onExecutionStart() throws AutomationFrameworkException {
        Logger.getRootLogger().addAppender(testLogAppender);
        Logger.getRootLogger().setLevel(Level.INFO);
        try {
            String activemqBindAddress = getParameters().get(Util.ACTIVEMQ_BIND_ADDRESS);
            long time1 = System.currentTimeMillis();
            log.info("Starting ActiveMQ...");
            broker.setDataDirectory(StratosServerExtension.class.getResource(File.separator).getPath() +
                    File.separator + ".." + File.separator + "activemq-data");
            broker.setBrokerName("testBroker");
            broker.addConnector(activemqBindAddress);
            broker.start();
            long time2 = System.currentTimeMillis();
            log.info(String.format("ActiveMQ started in %d sec", (time2 - time1) / 1000));
        }
        catch (Exception e) {
            throw new RuntimeException("Could not start ActiveMQ", e);
        }

        try {
            log.info("Setting up Stratos server...");
            AutomationContext stratosAutomationCtx =
                    new AutomationContext("STRATOS", "stratos-001", TestUserMode.SUPER_TENANT_ADMIN);
            String stratosBackendURL = stratosAutomationCtx.getContextUrls().getWebAppURL();
            //if port offset is not set, setting it to 0
            if (getParameters().get(ExtensionConstants.SERVER_STARTUP_PORT_OFFSET_COMMAND) == null) {
                getParameters().put(ExtensionConstants.SERVER_STARTUP_PORT_OFFSET_COMMAND, "0");
            }
            carbonTestServerManager =
                    new StratosTestServerManager(stratosAutomationCtx, System.getProperty(Util.CARBON_ZIP_KEY),
                            getParameters());

            log.info("Stratos server port offset: " + carbonTestServerManager.getPortOffset());
            log.info("Stratos backend URL: " + stratosBackendURL);

            long time3 = System.currentTimeMillis();
            String carbonHome = carbonTestServerManager.startServer();
            assertNotNull(carbonHome, "CARBON_HOME is null");

            while (!serverStarted()) {
                log.info("Waiting for topology to be initialized...");
                Thread.sleep(5000);
            }

            while (!mockServiceStarted()) {
                log.info("Waiting for mock service to be initialized...");
                Thread.sleep(1000);
            }

            long time4 = System.currentTimeMillis();
            log.info(String.format("Stratos server started in %d sec", (time4 - time3) / 1000));

        }
        catch (Exception e) {
            throw new RuntimeException("Could not start Stratos server", e);
        }
    }

    @Override
    public void onExecutionFinish() throws AutomationFrameworkException {
        try {
            carbonTestServerManager.stopServer();
            log.info("Stopped Stratos server");
        }
        catch (Exception e) {
            log.error("Could not stop Stratos server", e);
        }

        try {
            broker.stop();
            log.info("Stopped ActiveMQ server");
        }
        catch (Exception e) {
            log.error("Could not stop ActiveMQ server", e);
        }
    }

    private boolean serverStarted() {
        for (String message : testLogAppender.getMessages()) {
            if (message.contains("Topology initialized")) {
                return true;
            }
        }
        return false;
    }

    private boolean mockServiceStarted() {
        for (String message : testLogAppender.getMessages()) {
            if (message.contains("Mock IaaS service component activated")) {
                return true;
            }
        }
        return false;
    }
}