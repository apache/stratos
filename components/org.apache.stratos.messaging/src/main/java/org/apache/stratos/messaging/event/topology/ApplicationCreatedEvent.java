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

package org.apache.stratos.messaging.event.topology;

import org.apache.stratos.messaging.domain.topology.Application;
import org.apache.stratos.messaging.domain.topology.Cluster;

import java.util.List;

public class ApplicationCreatedEvent extends TopologyEvent {

    private Application application;

    private List<Cluster> clusterList;

    public ApplicationCreatedEvent (Application application, List<Cluster> clusters) {
        this.application = application;
        this.setClusterList(clusters);
    }

    public Application getApplication() {
        return application;
    }

    public String toString() {
        return "ApplicationCreatedEvent [app id= " + application.getId() + ", groups= " + application.getGroups() + ", clusters= " +
                application.getClusterDataMap().values() + "]";
    }

    public List<Cluster> getClusterList() {
        return clusterList;
    }

    public void setClusterList(List<Cluster> clusterList) {
        this.clusterList = clusterList;
    }
}
