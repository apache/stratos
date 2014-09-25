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
package org.apache.stratos.autoscaler.monitor.group;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.grouping.DependencyBuilder;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.List;
import java.util.Map;
import java.util.Observable;

/**
 * This is GroupMonitor to monitor the group which consists of
 * groups and clusters
 */
public class GroupMonitor extends Monitor {
    private static final Log log = LogFactory.getLog(GroupMonitor.class);

    public GroupMonitor(Group group) {
        super(group);
        this.id = group.getAlias();
        if(preOrderTraverse.isEmpty()) {
            log.warn("the child group/cluster cannot be found for the Group " + id);
        } else {
            startDependency();
        }
    }

    @Override
    public void update(Observable observable, Object event) {
        if(event instanceof MonitorStatusEvent) {
            MonitorStatusEvent statusEvent = (MonitorStatusEvent) event;
            Status childStatus = statusEvent.getStatus();
            String notifier = statusEvent.getNotifierId();
            log.info(String.format("[Monitor] %s got notified from the [child] %s" +
                    "on its state change from %s to %s", id, notifier, this.status, status));
            if(childStatus == Status.Activated) {
                //start the next dependency
                if(!preOrderTraverse.isEmpty()) {
                    startDependency();
                }
            }

        }

    }

    public void setStatus(Status status) {
        log.info(String.format("[Monitor] %s is notifying the parent" +
                "on its state change from %s to %s", id, this.status, status));
        this.status = status;
        setChanged();
        notifyObservers(new MonitorStatusEvent(status, id));
    }

    @Override
    public void startDependency() {
        //Need to get the order every time as group/cluster might already been started
        //TODO breadth first search in a tree and find the parallel one
        //TODO build up the tree with ordered manner

        preOrderTraverse = DependencyBuilder.getStartupOrder(component);

        //start the first dependency
        if(!preOrderTraverse.isEmpty()) {
            String dependency = preOrderTraverse.poll();
            if(log.isDebugEnabled()) {
                log.debug("Dependency check for the Group " + dependency + " started");
            }
            if (dependency.contains("group")) {
                for(Group group: component.getAliasToGroupMap().values()) {
                    if(group.getName().equals(dependency.substring(6))) {
                        startGroupMonitor(this, group.getAlias(), component);
                    }
                }
            } else if (dependency.contains("cartridge")) {
                for(ClusterDataHolder dataHolder : component.getClusterDataMap().values()) {
                    if(dataHolder.getServiceType().equals(dependency.substring(10))) {
                        String clusterId = dataHolder.getClusterId();
                        String serviceName = dataHolder.getServiceType();
                        Cluster cluster = null;
                        //TopologyManager.acquireReadLock();
                        cluster = TopologyManager.getTopology().getService(serviceName).getCluster(clusterId);
                        //TopologyManager.releaseReadLock();
                        if (cluster != null) {
                            startClusterMonitor(this, cluster);
                        } else {
                            //TODO throw exception since Topology is inconsistent
                        }
                    }
                }


            }
        } else {
            //all the groups/clusters have been started and waiting for activation
            log.info("All the groups/clusters of the [group]: " + this.id + " have been started.");
        }
    }

    //monitor the status of the cluster and the groups
    public void monitor() {


    }

    @Override
    protected void onEvent(Event event) {

    }
}
