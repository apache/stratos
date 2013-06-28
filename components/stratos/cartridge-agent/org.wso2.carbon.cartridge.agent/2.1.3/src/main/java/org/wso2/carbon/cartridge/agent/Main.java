/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.cartridge.agent;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cartridge.agent.internal.CartridgeAgentConstants;
import org.wso2.carbon.cartridge.agent.registrant.RegistrantDatabase;
import org.wso2.carbon.cartridge.agent.registrant.RegistrantHealthChecker;
import org.wso2.carbon.cartridge.agent.service.CartridgeAgentService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * The main class which starts up the Cartridge agent
 */
//public class Main {
//    private static final Log log = LogFactory.getLog(Main.class);
//    private static RegistrantHealthChecker healthChecker;
//    public static void main(String[] args) {
//        FileInputStream confFileIPStream = null;
//        try {
//            long start = System.currentTimeMillis();
//            log.info("Starting WSO2 Cartridge Agent...");
//            Properties conf = new Properties();
//            confFileIPStream = new FileInputStream("conf" + File.separator + "agent.properties");
//            conf.load(confFileIPStream);
//            RegistrantDatabase registrantDatabase = new RegistrantDatabase();
//            AxisServer axisServer = new AxisServer();
//            ConfigurationContext configurationContext = axisServer.getConfigurationContext();
//            ClusteringClient clusteringClient = new ClusteringClient(registrantDatabase);
//            configurationContext.setProperty(CartridgeAgentConstants.CLUSTERING_CLIENT, clusteringClient);
//            String healthCheckInterval = conf.getProperty("registrant.heathCheckInterval");
//            String threadPoolSize = conf.getProperty("registrant.healthCheckThreadPoolSize");
//            int healthCheckIntervalInt =
//                    (healthCheckInterval == null) ? 2000 : Integer.parseInt(healthCheckInterval);
//            int threadPoolSizeInt =
//                    (threadPoolSize == null) ? 10 : Integer.parseInt(healthCheckInterval);
//            log.info("Registrant health check interval: " + healthCheckIntervalInt + "s");
//            healthChecker = new RegistrantHealthChecker(registrantDatabase,
//                                                        clusteringClient,
//                                                        configurationContext,
//                                                        healthCheckIntervalInt,
//                                                        threadPoolSizeInt
//                                                        );
//            clusteringClient.init(conf,
//                                  configurationContext,
//                                  new ELBMembershipListener(clusteringClient,
//                                                            configurationContext,
//                                                            registrantDatabase,
//                                                            healthChecker));
//            healthChecker.startAll();
//            axisServer.deployService(CartridgeAgentService.class.getName());
//
//
//            // Starting cliet..
//            String trustStorePath = conf.getProperty("wso2.carbon.truststore");
//            System.setProperty("javax.net.ssl.trustStore", trustStorePath);
//            System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
//            // new InstanceStateNotificationClient().notify(null, null);
//
//            System.setProperty("adc.host",  conf.getProperty("adc.host"));
//            System.setProperty("adc.port",  conf.getProperty("adc.port"));
//            // ----------------------
//
//            writePID(".");
//            Runtime.getRuntime().addShutdownHook(new Thread(){
//
//                @Override
//                public void run() {
//                    log.info("Shutting down WSO2 Cartridge Agent...");
//                }
//            });
//            log.info("Started Cartridge Agent in " + (System.currentTimeMillis() - start) + "ms");
//        } catch (Exception e) {
//            log.fatal("Could not start Cartridge Agent", e);
//            System.exit(1);
//        } finally {
//            if (confFileIPStream != null) {
//                try {
//                    confFileIPStream.close();
//                } catch (IOException e) {
//                    log.error("Cannot close agent.properties file", e);
//                }
//            }
//        }
//    }
//
//    public static RegistrantHealthChecker getHealthChecker(){
//        return healthChecker;
//    }
//    /**
//     * Write the process ID of this process to the file
//     *
//     * @param carbonHome carbon.home sys property value.
//     */
//    private static void writePID(String carbonHome) {
//        byte[] bo = new byte[100];
//        String[] cmd = {"bash", "-c", "echo $PPID"};
//        Process p;
//        try {
//            p = Runtime.getRuntime().exec(cmd);
//        } catch (IOException e) {
//            //ignored. We might be invoking this on a Window platform. Therefore if an error occurs
//            //we simply ignore the error.
//            return;
//        }
//
//        try {
//            int bytes = p.getInputStream().read(bo);
//        } catch (IOException e) {
//            log.error(e.getMessage(), e);
//        }
//
//        String pid = new String(bo);
//        if (pid.length() != 0) {
//            BufferedWriter out = null;
//            try {
//                FileWriter writer = new FileWriter(carbonHome + File.separator + "wso2carbon.pid");
//                out = new BufferedWriter(writer);
//                out.write(pid);
//            } catch (IOException e) {
//                log.warn("Cannot write wso2carbon.pid file");
//            } finally {
//                if (out != null) {
//                    try {
//                        out.close();
//                    } catch (IOException ignored) {
//                    }
//                }
//            }
//        }
//    }
//}
