/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */


package org.apache.stratos.cartridge.agent.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.ClusteringClient;
import org.apache.stratos.cartridge.agent.ELBMembershipListener;
import org.apache.stratos.cartridge.agent.registrant.RegistrantDatabase;
import org.apache.stratos.cartridge.agent.registrant.RegistrantHealthChecker;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @scr.component name="org.wso2.carbon.cartridge.agent" immediate="true"
 *
 * @scr.reference name="config.context.service"
 *                interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setConfigurationContextService"
 *                unbind="unsetConfigurationContextService"
 */
public class CartridgeAgentServiceComponent {
    private static final Log log = LogFactory.getLog(CartridgeAgentServiceComponent.class);

    protected void activate(ComponentContext ctx) {
        RegistrantHealthChecker healthChecker;
        ConfigurationContext configurationContext = DataHolder.getServerConfigContext();

        FileInputStream confFileIPStream = null;
        try {
            long start = System.currentTimeMillis();
            log.info("Starting WSO2 Cartridge Agent...");
            Properties conf = new Properties();
            confFileIPStream = new FileInputStream(CarbonUtils.getCarbonConfigDirPath() +
                    File.separator + "agent.properties");
            conf.load(confFileIPStream);            
            for (String name : conf.stringPropertyNames()) {
    			String value = conf.getProperty(name);
    			System.setProperty(name, value);
    		}
            
            RegistrantDatabase registrantDatabase = new RegistrantDatabase();
//            AxisServer axisServer = new AxisServer();
//            ConfigurationContext configurationContext = axisServer.getConfigurationContext();
            ClusteringClient clusteringClient = new ClusteringClient(registrantDatabase);
            configurationContext.setProperty(CartridgeAgentConstants.CLUSTERING_CLIENT, clusteringClient);

            String healthCheckInterval = conf.getProperty("registrant.heathCheckInterval");
            String threadPoolSize = conf.getProperty("registrant.healthCheckThreadPoolSize");
            int healthCheckIntervalInt =
                    (healthCheckInterval == null) ? 2000 : Integer.parseInt(healthCheckInterval);
            int threadPoolSizeInt =
                    (threadPoolSize == null) ? 10 : Integer.parseInt(healthCheckInterval);
            log.info("Registrant health check interval: " + healthCheckIntervalInt + "s");
            healthChecker = new RegistrantHealthChecker(registrantDatabase,
                    clusteringClient,
                    configurationContext,
                    healthCheckIntervalInt,
                    threadPoolSizeInt
            );
            clusteringClient.init(conf,
                    configurationContext,
                    new ELBMembershipListener(clusteringClient,
                            configurationContext,
                            registrantDatabase,
                            healthChecker));
            healthChecker.startAll();
            DataHolder.setHealthChecker(healthChecker);

            Runtime.getRuntime().addShutdownHook(new Thread(){

                @Override
                public void run() {
                    log.info("Shutting down WSO2 Cartridge Agent...");
                }
            });
            log.info("Started Cartridge Agent in " + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            log.fatal("Could not start Cartridge Agent", e);
            //System.exit(1);
        } finally {
            if (confFileIPStream != null) {
                try {
                    confFileIPStream.close();
                } catch (IOException e) {
                    log.error("Cannot close agent.properties file", e);
                }
            }
        }

    }

    protected void deactivate(ComponentContext ctx) {
    }

    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        DataHolder.setServerConfigContext(contextService.getServerConfigContext());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
    }
}
