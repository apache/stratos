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
package org.wso2.carbon.cartridge.agent.registrant;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.axis2.clustering.management.GroupManagementAgent;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cartridge.agent.ClusteringClient;
import org.wso2.carbon.cartridge.agent.InstanceStateNotificationClientThread;

/**
 * This health checker periodically checks the health of the {@link Registrant}s
 *
 * If a registrant is found to be unhealthy, then it is stopped. This task will also try to
 * connect to reactivate a registrant which was previously found to be unhealthy.
 *
 * If the Elastic Load Balancer (ELB) is not running, this health checker will not continue with
 * registrant health checks since it is futile to try to connect the registrants to the
 * unavailable ELB.
 */
public class RegistrantHealthChecker {
    private static final Log log = LogFactory.getLog(RegistrantHealthChecker.class);

    private RegistrantDatabase database;
    private ClusteringClient clusteringClient;
    private ConfigurationContext configurationContext;
    private ScheduledExecutorService scheduler;
    private volatile boolean isELBRunning;
    private int healthCheckInterval;

    public RegistrantHealthChecker(RegistrantDatabase database,
                                   ClusteringClient clusteringClient,
                                   ConfigurationContext configurationContext,
                                   int healthCheckInterval,
                                   int threadPoolSize) {
        this.database = database;
        this.clusteringClient = clusteringClient;
        this.configurationContext = configurationContext;
        this.healthCheckInterval = healthCheckInterval;
        scheduler = Executors.newScheduledThreadPool(threadPoolSize);
    }

    public void startAll() {
        List<Registrant> registrants = database.getRegistrants();
        for (Registrant registrant : registrants) {
            scheduler.scheduleWithFixedDelay(new HealthCheckerTask(registrant), 45,
                                 healthCheckInterval, TimeUnit.SECONDS);
            if (log.isDebugEnabled()) {
                log.debug("Started a health checker for " + registrant + " ...");
            }
        }
    }

    public void start(Registrant registrant){
        scheduler.scheduleWithFixedDelay(new HealthCheckerTask(registrant), 45,
                             healthCheckInterval, TimeUnit.SECONDS);
        if (log.isDebugEnabled()) {
            log.debug("Added a health checker for " + registrant + " ...");
        }
    }

    public void setELBRunning(boolean ELBRunning) {
        isELBRunning = ELBRunning;
    }

    private final class HealthCheckerTask implements Runnable {
        Registrant registrant;
        public HealthCheckerTask(Registrant registrant){
            this.registrant = registrant;
        }
        public void run() {
            if(!isELBRunning){
                return;
            }
            try {
                boolean healthyRegistrant = RegistrantUtil.isHealthy(registrant);
                if (!healthyRegistrant && registrant.running()) {
                    registrant.stop();
                    new Thread(new InstanceStateNotificationClientThread(registrant, "INACTIVE")).start();
                    log.warn("Stopped registrant " + registrant + " since it is unhealthy." );
                } else if (healthyRegistrant && !registrant.running()) {
                    registrant.stop();
                    new Thread(new InstanceStateNotificationClientThread(registrant, "INACTIVE")).start();
                    clusteringClient.joinGroup(registrant, configurationContext);
                    log.info("Restarted registrant " + registrant + " after it became active");
                }
            } catch (Exception e) {
                log.error("Error occurred while running registrant health check", e);
            }
        }
    }
}
