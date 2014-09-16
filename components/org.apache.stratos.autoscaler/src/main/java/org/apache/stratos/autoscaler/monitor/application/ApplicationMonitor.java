/*
 *     Licensed to the Apache Software Foundation (ASF) under one
 *     or more contributor license agreements.  See the NOTICE file
 *     distributed with this work for additional information
 *     regarding copyright ownership.  The ASF licenses this file
 *     to you under the Apache License, Version 2.0 (the
 *     "License"); you may not use this file except in compliance
 *     with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing,
 *     software distributed under the License is distributed on an
 *     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *     KIND, either express or implied.  See the License for the
 *     specific language governing permissions and limitations
 *     under the License.
 */
package org.apache.stratos.autoscaler.monitor.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.*;

public class ApplicationMonitor extends Monitor {
    private static final Log log = LogFactory.getLog(ApplicationMonitor.class);
    private Application application;
    private Queue<String> preOrderTraverse = new LinkedList<String>();
    private Queue<Cluster> clusters = new LinkedList<Cluster>();
    Map<String, StatusChecker> statusCheckerMap = new HashMap<String, StatusChecker>();

    private Queue<Group> groups = new LinkedList<Group>();


    public ApplicationMonitor(Application application) {
        this.application = application;
        //TODO build dependencies and keep them here
        DependencyOrder dependencyOrder = application.getDependencyOrder();
        Set<StartupOrder> startupOrderSet = dependencyOrder.getStartupOrders();
        for(StartupOrder startupOrder: startupOrderSet) {

            String start = startupOrder.getStart();
            String after = startupOrder.getAfter();

            if (!preOrderTraverse.contains(start)) {
                preOrderTraverse.add(start);
                if (!preOrderTraverse.contains(after)) {
                    preOrderTraverse.add(after);

                } else {
                    //TODO throw exception since after is there before start
                }
            } else {
                if (!preOrderTraverse.contains(after)) {
                    preOrderTraverse.add(after);
                } else {
                    //TODO throw exception since start and after already there
                }
            }
        }

        //TODO find out the parallel ones

        //start the first dependency
        String dependency = preOrderTraverse.poll();
        if(dependency.contains("group")) {
            startGroupMonitor(application.getGroup(dependency));
        } else if(dependency.contains("cartridge")) {
            String clusterId = application.getClusterId(dependency);
            Cluster cluster = null;
            TopologyManager.acquireReadLock();
            cluster = TopologyManager.getTopology().getService(dependency).getCluster(clusterId);
            TopologyManager.releaseReadLock();
           if(cluster != null) {
               startClusterMonitor(cluster);
           } else {
               //TODO throw exception since Topology is inconsistent
           }
        }
    }


    //start the least dependent cluster monitor as part of the applicationCreatedEvent
    public void registerFirstClusterMonitor() {
        //build dependency tree




        //traverse dependency tree and find the clusters to be started and
        // register the correct GroupMonitor or ClusterMonitor
        //startGroupMonitor(groups.peek());
        //groups.poll();







    }

    public void startMonitor() {

    }

    @Override
    public void run() {
        while (true ) { //TODO add the correct status
            if (log.isDebugEnabled()) {
                log.debug("App monitor is running.. " + this.toString());
            }



            try {
                // TODO make this configurable
                Thread.sleep(30000);
            } catch (InterruptedException ignore) {
            }
        }
    }


    @Override
    public void monitor() {

    }

    public Queue<String> getPreOrderTraverse() {
        return preOrderTraverse;
    }

    public void setPreOrderTraverse(Queue<String> preOrderTraverse) {
        this.preOrderTraverse = preOrderTraverse;
    }
}
