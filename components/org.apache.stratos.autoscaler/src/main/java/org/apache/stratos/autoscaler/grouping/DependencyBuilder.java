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
package org.apache.stratos.autoscaler.grouping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.*;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * This is to build the startup/termination dependencies
 * across all the groups and clusters
 */
public class DependencyBuilder {
    private static final Log log = LogFactory.getLog(DependencyBuilder.class);

    public static Queue<String> getStartupOrder(ParentBehavior component) {

        Queue<String> startup = new LinkedList<String>();
        DependencyOrder dependencyOrder = component.getDependencyOrder();
        if (dependencyOrder != null) {
            Set<StartupOrder> startupOrderSet = dependencyOrder.getStartupOrders();
            for (StartupOrder startupOrder : startupOrderSet) {

                String start = startupOrder.getStart();
                String after = startupOrder.getAfter();

                if (!startup.contains(start)) {
                    startup.add(start);
                    if (!startup.contains(after)) {
                        startup.add(after);

                    } else {
                        //TODO throw exception since after is there before start
                    }
                } else {
                    if (!startup.contains(after)) {
                        startup.add(after);
                    } else {
                        //TODO throw exception since start and after already there
                    }
                }
            }
        }
        //TODO adding all the missed groups or clusters as the top child to the list
        //TODO handle by application and group itself groupName and serviceName

        if (component instanceof Application) {
            if(log.isDebugEnabled()) {
                log.debug("Building dependency for the Application/Group " +
                        ((Application) component).getId());
            }
            for (Group group : component.getAliasToGroupMap().values()) {
                if (!startup.contains("group." + group.getAlias())) {
                    startup.add("group." + group.getAlias());
                }
            }

            Set<String> cartridgeAliases = component.getClusterDataMap().keySet();

            for (String carAlias : cartridgeAliases) {
                if (!startup.contains("cartridge." + carAlias)) {
                    startup.add("cartridge." + carAlias);

                }
            }
        } else if (component instanceof Group) {
            if(log.isDebugEnabled()) {
                log.debug("Building dependency for the Application/Group " +
                        ((Group) component).getAlias());
            }
            for (Group group : component.getAliasToGroupMap().values()) {
                if (!startup.contains("group." + group.getName())) {
                    startup.add("group." + group.getName());
                }
            }
            for (ClusterDataHolder dataHolder : component.getClusterDataMap().values()) {
                if (!startup.contains("cartridge." + dataHolder.getServiceType())) {
                    startup.add("cartridge." + dataHolder.getServiceType());

                }
            }
        }
        return startup;

    }

}
