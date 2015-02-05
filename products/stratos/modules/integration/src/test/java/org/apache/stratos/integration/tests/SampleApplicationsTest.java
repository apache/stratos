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

package org.apache.stratos.integration.tests;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.ExecutorService;

import static junit.framework.Assert.*;

/**
 * Sample application tests.
 */
public class SampleApplicationsTest extends StratosTestServerManager {

    private static final Log log = LogFactory.getLog(StratosTestServerManager.class);

    public static final int APPLICATION_ACTIVATION_TIMEOUT = 240000;
    private ApplicationsEventReceiver applicationsEventReceiver;

    @BeforeClass
    public void setUp(){
        // Set jndi.properties.dir system property for initializing event receivers
        System.setProperty("jndi.properties.dir", getResourcesFolderPath());
    }

    @Test
    public void testSingleCartridgeApplication() {
        try {
            initializeApplicationEventReceiver();

            String scriptPath = getResourcesFolderPath() + "/../../../../../../" +
                    "samples/applications/single-cartridge/scripts/mock/deploy.sh";
            executeCommand(scriptPath);

            assertApplicationActivation("single-cartridge-app");
        } catch (Exception e) {
            log.error(e);
            assertTrue("An error occurred", false);
        }
    }

    /**
     * Initialize application event receiver
     */
    private void initializeApplicationEventReceiver() {
        if(applicationsEventReceiver == null) {
            applicationsEventReceiver = new ApplicationsEventReceiver();
            ExecutorService executorService = StratosThreadPool.getExecutorService("STRATOS_TEST_SERVER", 1);
            applicationsEventReceiver.setExecutorService(executorService);
            applicationsEventReceiver.execute();
        }
    }

    /**
     * Execute shell command
     * @param commandText
     */
    private void executeCommand(String commandText) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            CommandLine commandline = CommandLine.parse(commandText);
            DefaultExecutor exec = new DefaultExecutor();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            exec.setStreamHandler(streamHandler);
            exec.execute(commandline);
            log.info(outputStream.toString());
        } catch (Exception e) {
            log.error(outputStream.toString(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Assert application activation
     * @param applicationName
     */
    private void assertApplicationActivation(String applicationName) {
        long startTime = System.currentTimeMillis();
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        while(!((application != null) && (application.getStatus() == ApplicationStatus.Active))) {
            application = ApplicationManager.getApplications().getApplication(applicationName);
            if((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                break;
            }
        }

        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);
        assertEquals(String.format("Application status did not change to active: [application-id] %s", applicationName),
                ApplicationStatus.Active, application.getStatus());
    }

    /**
     * Get resources folder path
     * @return
     */
    private String getResourcesFolderPath() {
        String path = getClass().getResource("/").getPath();
        return StringUtils.removeEnd(path, File.separator);
    }
}
