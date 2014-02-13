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
package org.apache.stratos.load.balancer.integration.tests;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.wso2.carbon.integration.framework.TestServerManager;

import java.io.File;
import java.io.IOException;

/**
 * Prepares the WSO2 LB for test runs, starts the server, and stops the server after
 * test runs
 */
public class LoadBalancerTestServerManager extends TestServerManager {
    private static final Log log = LogFactory.getLog(LoadBalancerTestServerManager.class);

    public LoadBalancerTestServerManager() {
        super(1);
    }

    @Override
    @BeforeSuite(timeOut = 300000)
    public String startServer() throws IOException {
        String carbonHome = super.startServer();
        System.setProperty("carbon.home", carbonHome);
        return carbonHome;
    }

    @Override
    @AfterSuite(timeOut = 60000)
    public void stopServer() throws Exception {
        super.stopServer();
    }

    protected void copyArtifacts(String carbonHome) throws IOException {
        String resourceLocation = System.getProperty("framework.resource.location",
                System.getProperty("basedir") + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator );
        String confDir = carbonHome + File.separator + "repository"+ File.separator + "conf";

        log.info("Copying loadbalancer.conf file...");
        FileUtils.copyFile(new File(resourceLocation,"loadbalancer.conf"),new File(confDir,"loadbalancer.conf"));

        // Could not provide the script name via Test Server Manager
        FileUtils.copyFile(new File(carbonHome + File.separator + "bin", "stratos.sh"), new File(carbonHome + File.separator + "bin", "wso2server.sh"));
        FileUtils.copyFile(new File(carbonHome + File.separator + "bin","stratos.bat"),new File(carbonHome + File.separator + "bin","wso2server.bat"));
    }
}

