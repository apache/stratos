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
package org.apache.stratos.autoscaler.applications.dependency.context;

import org.apache.stratos.messaging.domain.topology.ClusterStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * This is to keep track of the
 */
public abstract class ApplicationContext {
    private List<ApplicationContext> applicationContextList;

    private String id;
    protected boolean started;
    private boolean terminated;

    private ClusterStatus status;

    private Stack<ClusterStatus> statusLifeCycle;

    protected boolean isDependent;

    public ApplicationContext(String id, boolean killDependent) {
        applicationContextList = new ArrayList<ApplicationContext>();
        statusLifeCycle = new Stack<ClusterStatus>();
        this.setDependent(killDependent);
        this.id = id;
    }

    public List<ApplicationContext> getApplicationContextList() {
        return applicationContextList;
    }

    public void setApplicationContextList(List<ApplicationContext> applicationContextList) {
        this.applicationContextList = applicationContextList;
    }

    public void addApplicationContext(ApplicationContext applicationContext) {
        applicationContextList.add(applicationContext);

    }

    public void addStatusToLIfeCycle(ClusterStatus status) {
       this.statusLifeCycle.push(status);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ClusterStatus getCurrentStatus() {
        return status;
    }

    public void setCurrentStatus(ClusterStatus status) {
        this.status = status;
    }

    public List<ClusterStatus> getStatusLifeCycle() {
        return statusLifeCycle;
    }

    public boolean hasChild() {
        boolean hasChild;
        if(this.applicationContextList.isEmpty()) {
            hasChild = false;
        } else {
            hasChild = true;
        }
        return hasChild;
    }


    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public boolean isDependent() {
        return isDependent;
    }

    public void setDependent(boolean isDependent) {
        this.isDependent = isDependent;
    }
}
