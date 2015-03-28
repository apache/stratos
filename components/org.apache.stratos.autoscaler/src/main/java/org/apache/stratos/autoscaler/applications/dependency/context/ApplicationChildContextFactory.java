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
import org.apache.stratos.autoscaler.applications.dependency.DependencyTree;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.application.ParentComponent;

/**
 * Factory to create new GroupChildContext or ClusterChildContext
 */
public class ApplicationChildContextFactory {
    private static final Log log = LogFactory.getLog(ApplicationChildContextFactory.class);

    /**
     * Will create and return GroupChildContext/ClusterChildContext based on the type in start order/scaling order
     *
     * @param startUpOrder      reference of group/cluster in the start/scaling order
     * @param component       The component which used to build the dependency
     * @param tree kill dependent behaviour of this component
     * @return Context
     */
    public static ApplicationChildContext createApplicationChildContext(String componentId, String startUpOrder,
                                                                        ParentComponent component,
                                                                        DependencyTree tree) {

        boolean hasDependents = tree.isTerminateDependent() || tree.isTerminateAll();
        if (startUpOrder.trim().startsWith(AutoscalerConstants.GROUP + ".")) {
            // Find group alias
            String groupAlias = startUpOrder.substring(AutoscalerConstants.GROUP.length() + 1);
            return createGroupChildContext(groupAlias, hasDependents);
        } else if (startUpOrder.trim().startsWith(AutoscalerConstants.CARTRIDGE + ".")) {
            // Find cartridge alias
            String cartridgeAlias = startUpOrder.substring(AutoscalerConstants.CARTRIDGE.length() + 1);
            // Find cluster-id from cluster alias
            ClusterDataHolder clusterDataHolder = component.getClusterDataHolderRecursivelyByAlias(cartridgeAlias);
            return createClusterChildContext(clusterDataHolder, hasDependents);
        } else {
            throw new RuntimeException(String.format("Startup order contains an unknown reference: " +
                            "[component] %s [startup-order] %s", componentId, startUpOrder));
        }
    }

	/**
	 * Get cluster child context
	 * @param dataHolder Cluster Data holder
	 * @param isKillDependent Whether is this a kill dependant or not
	 * @return ApplicationChildContext
	 */
    public static ApplicationChildContext createClusterChildContext(ClusterDataHolder dataHolder,
                                                                    boolean isKillDependent) {
        ClusterChildContext clusterChildContext = new ClusterChildContext(dataHolder.getClusterId(), isKillDependent);
        clusterChildContext.setServiceName(dataHolder.getServiceType());
        return  clusterChildContext;
    }

	/**
	 * Get the group child context
	 * @param id ID of the group
	 * @param isDependent Whether is this a dependant or not
	 * @return ApplicationChildContext
	 */
    public static ApplicationChildContext createGroupChildContext(String id, boolean isDependent) {
        GroupChildContext groupChildContext = new GroupChildContext(id, isDependent);
        return groupChildContext;
    }
}
