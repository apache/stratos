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
package org.apache.stratos.autoscaler.grouping.dependency.context;

import org.apache.stratos.autoscaler.grouping.dependency.DependencyTree;
import org.apache.stratos.messaging.domain.topology.ParentBehavior;

/**
 * Factory to create new GroupContext or ClusterContext
 */
public class ApplicationContextFactory {

    public static ApplicationContext getApplicationContext(String startOrder,
                                                           ParentBehavior component,
                                                           DependencyTree dependencyTree) {
        String id;
        ApplicationContext applicationContext = null;
        if (startOrder.contains("group")) {
            id = getGroupFromStartupOrder(startOrder);
            //TODO getting the alias of the group using groupName
            applicationContext = new GroupContext(component.getGroup(id).getAlias(),
                    dependencyTree.isKillDependent());
        } else if (startOrder.contains("cartridge")) {
            id = getClusterFromStartupOrder(startOrder);
            //TODO getting the cluster id of the using cartridge name
            applicationContext = new ClusterContext(component.getClusterData(id).getClusterId(),
                    dependencyTree.isKillDependent());
        } else {
            //TODO throw exception
        }
        return applicationContext;

    }

    public static String getGroupFromStartupOrder(String startupOrder) {
        return startupOrder.substring(6);
    }

    public static String getClusterFromStartupOrder(String startupOrder) {
        return startupOrder.substring(10);
    }



}
