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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.applications.dependency.DependencyTree;
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.domain.applications.ParentComponent;

/**
 * Factory to create new GroupContext or ClusterContext
 */
public class ApplicationContextFactory {
    private static final Log log = LogFactory.getLog(ApplicationContextFactory.class);

    /**
     * Will return the GroupContext/ClusterContext based on the type in start order/scaling order
     *
     * @param order      reference of group/cluster in the start/scaling order
     * @param component       The component which used to build the dependency
     * @param tree kill dependent behaviour of this component
     * @return Context
     */
    public static ApplicationContext getApplicationContext(String order,
                                                           ParentComponent component,
                                                           DependencyTree tree) {
        String id;
        ApplicationContext applicationContext = null;
        boolean isDependent = tree.isTerminateDependent() || tree.isTerminateAll();
        if (order.startsWith(Constants.GROUP + ".")) {
            //getting the group alias
            id = getGroupFromStartupOrder(order);
            applicationContext = getGroupContext(id, isDependent);
        } else if (order.startsWith(Constants.CARTRIDGE + ".")) {
            //getting the cluster alias
            id = getClusterFromStartupOrder(order);
            //getting the cluster-id from cluster alias
            ClusterDataHolder clusterDataHolder = component.getClusterDataMap().get(id);
            applicationContext = getClusterContext(clusterDataHolder, isDependent);

        } else {
            log.warn("[Startup Order]: " + order + " contains unknown reference");
        }
        return applicationContext;

    }

    /**
     * Utility method to get the group alias from the startup order Eg: group.mygroup
     *
     * @param startupOrder startup order
     * @return group alias
     */
    public static String getGroupFromStartupOrder(String startupOrder) {
        return startupOrder.substring(Constants.GROUP.length() + 1);
    }

    /**
     * Utility method to get the cluster alias from startup order Eg: cartridge.myphp
     *
     * @param startupOrder startup order
     * @return cluster alias
     */
    public static String getClusterFromStartupOrder(String startupOrder) {
        return startupOrder.substring(Constants.CARTRIDGE.length() + 1);
    }

    public static ApplicationContext getClusterContext(ClusterDataHolder dataHolder,
                                                       boolean isKillDependent) {
        ApplicationContext applicationContext;
        applicationContext = new ClusterContext(dataHolder.getClusterId(),
                isKillDependent);
        ((ClusterContext) applicationContext).setServiceName(dataHolder.getServiceType());
        return  applicationContext;
    }

    public static ApplicationContext getGroupContext(String id, boolean isDependent) {
        ApplicationContext applicationContext;
        applicationContext = new GroupContext(id,
                isDependent);
        return applicationContext;
    }
}
