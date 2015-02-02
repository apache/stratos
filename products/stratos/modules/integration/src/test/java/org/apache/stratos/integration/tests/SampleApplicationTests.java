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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.stratos.common.test.TestLogAppender;
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Sample application tests.
 */
public class SampleApplicationTests extends StratosTestServerManager {

    private static final Log log = LogFactory.getLog(StratosTestServerManager.class);

    private final static String SAMPLES_FOLDER_PATH = SampleApplicationTests.class.getResource("/").getPath() +
            "./../../../../../../samples";
    private TestLogAppender testLogAppender;

    @BeforeClass
    public void setUp(){
        String path = getClass().getResource("/").getPath();
        path = StringUtils.removeEnd(path, File.separator);
        System.setProperty("jndi.properties.dir", path);

        testLogAppender = new TestLogAppender();
        Logger.getRootLogger().addAppender(testLogAppender);
        Logger.getRootLogger().setLevel(Level.INFO);
    }

    @Test
    public void testSingleCartridgeApplication() {
        try {
            ApplicationsEventReceiver applicationsEventReceiver = new ApplicationsEventReceiver();
            ExecutorService executorService = StratosThreadPool.getExecutorService("STRATOS_TEST_SERVER", 1);
            applicationsEventReceiver.setExecutorService(executorService);
            applicationsEventReceiver.execute();

            while (!serverStarted(testLogAppender)) {
                log.info("Waiting for stratos server to be started...");
                Thread.sleep(2000);
            }
            String scriptPath = SAMPLES_FOLDER_PATH + "/applications/single-cartridge/scripts/mock/deploy.sh";
            executeCommand(scriptPath);

            long startTime = System.currentTimeMillis();
            Application application = ApplicationManager.getApplications().getApplication("single-cartridge-app");
            while(!((application != null) && (application.getStatus() == ApplicationStatus.Active))) {
                application = ApplicationManager.getApplications().getApplication("single-cartridge-app");
                if((System.currentTimeMillis() - startTime) > 240000) {
                    break;
                }
            }

            assertNotNull("Application is not found", application);
            assertEquals(String.format("Application status did not change to active", application.getStatus()), ApplicationStatus.Active, application.getStatus());
        } catch (Exception e) {
            log.error(e);
            assertTrue("An error occurred", false);
        }
    }

    private boolean serverStarted(TestLogAppender testLogAppender) {
        for(String message : testLogAppender.getMessages()) {
            if(message.contains("Topology initialized")) {
                return true;
            }
        }
        return false;
    }

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
}
