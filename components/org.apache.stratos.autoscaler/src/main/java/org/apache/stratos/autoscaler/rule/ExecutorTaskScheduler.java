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

package org.apache.stratos.autoscaler.rule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.util.ConfUtil;

/**
 * This class is responsible for scheduling the task of evaluating the current details of topology, statistics, and health
 * status against the rules set(written in Drools)
 */
public class ExecutorTaskScheduler implements Runnable {
    private static final Log log = LogFactory.getLog(ExecutorTaskScheduler.class);

    private static int initialDelay;
    private static int period;

    public ExecutorTaskScheduler() {
        XMLConfiguration conf = ConfUtil.getInstance().getConfiguration();
        initialDelay = conf.getInt("autoscaler.rulesEvaluator.schedule.initialDelay", Constants.SCHEDULE_DEFAULT_INITIAL_DELAY);
        period = conf.getInt("autoscaler.rulesEvaluator.schedule.period", Constants.SCHEDULE_DEFAULT_PERIOD);
    }

    @Override
    public void run() {
    	final ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
        final Runnable rulesEvaluator = new Runnable() {
            public void run() {

//                try {
//                    for (Service service : TopologyManager.getTopology().getServices()) {
//
//                        AutoscalerRuleEvaluator.getInstance().evaluate(service);
//                    }
//
//                    // Remove cluster context if its already removed from Topology
//                    for (String clusterContextId : AutoscalerContext.getInstance().getClusterContexes().keySet()) {
//                        boolean clusterAvailable = false;
//                        for (Service service : TopologyManager.getTopology().getServices()) {
//                            for (Cluster cluster : service.getClusters()) {
//                                if (cluster.getClusterId().equals(clusterContextId)) {
//
//                                    clusterAvailable = true;
//                                }
//                            }
//                        }
//
//                        if (!clusterAvailable) {
//                            AutoscalerContext.getInstance().removeClusterContext(clusterContextId);
//                        }
//                    }
//
//                } catch (Exception e) {
//                    String msg = "Error while evaluating rules.";
//                    log.error(msg, e);
//                    throw new RuntimeException(msg, e);
////                    log.debug("Shutting down rule scheduler");
////                    ex.shutdownNow();
//                }
            }
        };
        
        ex.scheduleWithFixedDelay(rulesEvaluator, initialDelay, period, TimeUnit.SECONDS);
    }
}
