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
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.grouping.dependency.context.ApplicationContext;
import org.apache.stratos.autoscaler.monitor.EventHandler;
import org.apache.stratos.autoscaler.monitor.ParentComponentMonitor;
import org.apache.stratos.autoscaler.monitor.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.messaging.domain.topology.Group;
import org.apache.stratos.messaging.domain.topology.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * This is GroupMonitor to monitor the group which consists of
 * groups and clusters
 */
public class GroupMonitor extends ParentComponentMonitor implements EventHandler {
    private static final Log log = LogFactory.getLog(GroupMonitor.class);

    //Parent monitor of this monitor
    private ParentComponentMonitor parent;

    /**
     * Constructor of GroupMonitor
     * @param group Takes the group from the Topology
     * @throws DependencyBuilderException throws when couldn't build the Topology
     * @throws TopologyInConsistentException throws when topology is inconsistent
     */
    public GroupMonitor(Group group, String appId) throws DependencyBuilderException,
                                            TopologyInConsistentException {
        super(group);
        this.appId = appId;
        startDependency();
    }

    /**
     * Will set the status of the monitor based on Topology Group status/child status like scaling
     * @param status
     */
    public void setStatus(Status status) {
        log.info(String.format("[Monitor] %s is notifying the parent" +
                "on its state change from %s to %s", id, this.status, status));
        this.status = status;
        //notifying the parent
        MonitorStatusEventBuilder.handleGroupStatusEvent(this.parent, this.status, this.id);
    }

    @Override
    public void onEvent(MonitorStatusEvent statusEvent) {
        monitor(statusEvent);
    }

    @Override
    protected void monitor(MonitorStatusEvent statusEvent) {
        String id = statusEvent.getId();
        Status status1 = statusEvent.getStatus();
        ApplicationContext context = this.dependencyTree.findApplicationContextWithId(id);
        //Events coming from parent are In_Active(in faulty detection), Scaling events, termination
        //TODO if statusEvent is for active, then start the next one if any available
        if(!isParent(id)) {
            if(status1 == Status.Activated) {
                try {
                    //if life cycle is empty, need to start the monitor
                    boolean startDep = startDependency(statusEvent.getId());
                    if (log.isDebugEnabled()) {
                        log.debug("started a child: " + startDep + " by the group/cluster: " + id);

                    }
                    //updating the life cycle and current status
                    if (startDep) {
                        context.setCurrentStatus(Status.Created);
                        context.addStatusToLIfeCycle(Status.Created);
                    } else {
                        StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);
                    }

                } catch (TopologyInConsistentException e) {
                    //TODO revert the siblings and notify parent, change a flag for reverting/un-subscription
                    log.error(e);
                }
            } else if(status1 == Status.In_Active) {
                //TODO if C1 depends on C2, then if C2 is in_active, then by getting killdepend as C1 and
                //TODO need to send in_active for c1. When C1 in_active receives, get dependent and
                //TODO check whether dependent in_active. Then kill c1.
                //evaluate termination behavior and take action based on that.

                List<ApplicationContext> terminationList = new ArrayList<ApplicationContext>();
                terminationList = this.dependencyTree.getTerminationDependencies(id);

                //check whether all the children are in_active state
                for(ApplicationContext terminationContext : terminationList) {
                   //terminationContext
                }

                /*if(terminationList != null && !terminationList.isEmpty()) {
                    for(ApplicationContext context1 : terminationList) {
                        if(context1 instanceof ClusterContext) {
                            AbstractClusterMonitor monitor = this.clusterIdToClusterMonitorsMap.
                                    get(context1.getId());
                            //Whether life cycle change to Created
                            if(monitor.getStatus() == Status.Created) {
                                canTerminate = true;
                            } else {
                                //TODO sending group in_active event to dependent cluster/group
                                StatusEventPublisher.sendGroupActivatedEvent(this.appId, this.id);
                                //not all dependent clusters are in created state.
                                canTerminate = false;
                            }
                        }
                    }
                    if(canTerminate) {
                       //
                    }*/
                } else {
                    //TODO get dependents
                    List<ApplicationContext> dependents = this.dependencyTree.getTerminationDependencies(id);
                }





            } else if(status1 == Status.Created) {
                //the dependent goes to be created state, so terminate the dependents
            }
        }





    public ParentComponentMonitor getParent() {
        return parent;
    }

    public void setParent(ParentComponentMonitor parent) {
        this.parent = parent;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    private boolean isParent(String id) {
        if(this.parent.getId().equals(id)) {
            return true;
        } else {
            return false;
        }
    }





}
