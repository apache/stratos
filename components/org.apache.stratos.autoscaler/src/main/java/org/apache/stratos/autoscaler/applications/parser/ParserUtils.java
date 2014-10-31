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

package org.apache.stratos.autoscaler.applications.parser;

import org.apache.stratos.autoscaler.applications.pojo.GroupContext;
import org.apache.stratos.autoscaler.applications.pojo.SubscribableContext;
import org.apache.stratos.autoscaler.exception.ApplicationDefinitionException;
import org.apache.stratos.messaging.domain.applications.StartupOrder;

import java.util.*;

public class ParserUtils {

    public static Set<StartupOrder> convert (String [] startupOrderArr) throws ApplicationDefinitionException {

        Set<StartupOrder> startupOrders = new HashSet<StartupOrder>();

        if (startupOrderArr == null) {
            return startupOrders;
        }

        for (String commaSeparatedStartupOrder : startupOrderArr) {
            startupOrders.add(getStartupOrder(commaSeparatedStartupOrder));
        }

        return startupOrders;
    }

    private static StartupOrder getStartupOrder (String commaSeparatedStartupOrder) throws ApplicationDefinitionException{

        List<String> startupOrders = new ArrayList<String>();

        for (String startupOrder : Arrays.asList(commaSeparatedStartupOrder.split(","))) {
            startupOrder = startupOrder.trim();
            if (!startupOrder.startsWith("cartridge.") && !startupOrder.startsWith("group.")) {
                throw new ApplicationDefinitionException("Incorrect Startup Order specified, should start with 'cartridge.' or 'group.'");
            }

            startupOrders.add(startupOrder);
        }

        return new StartupOrder(startupOrders);
    }

    public static Set<StartupOrder> convert (String [] startupOrderArr, GroupContext groupContext)
            throws ApplicationDefinitionException {

        Set<StartupOrder> startupOrders = new HashSet<StartupOrder>();

        if (startupOrderArr == null) {
            return startupOrders;
        }


        for (String commaSeparatedStartupOrder : startupOrderArr) {
            // convert all Startup Orders to aliases-based
            List<String> components = Arrays.asList(commaSeparatedStartupOrder.split(","));
            startupOrders.add(getStartupOrder(components, groupContext));
        }

        return startupOrders;
    }

    private static StartupOrder getStartupOrder (List<String> components, GroupContext groupContext)
            throws ApplicationDefinitionException {

        List<String> aliasBasedComponents = new ArrayList<String>();

        for (String component : components) {
            component = component.trim();

            String aliasBasedComponent;
            if (component.startsWith("cartridge.")) {
                String cartridgeType = component.substring(10);
                aliasBasedComponent = getAliasForServiceType(cartridgeType, groupContext);
                if (aliasBasedComponent == null) {
                    throw new ApplicationDefinitionException("Unable convert Startup Order to alias-based; " +
                            "cannot find the matching alias for Service type " + cartridgeType);
                }

                aliasBasedComponent = "cartridge.".concat(aliasBasedComponent);

            } else if (component.startsWith("group.")) {
                String groupName = component.substring(6);
                aliasBasedComponent = getAliasForGroupName(groupName, groupContext);
                if (aliasBasedComponent == null) {
                    throw new ApplicationDefinitionException("Unable convert Startup Order to alias-based; " +
                            "cannot find the matching alias for Group name " + groupName);
                }

                aliasBasedComponent = "group.".concat(aliasBasedComponent);

            } else {
                throw new ApplicationDefinitionException("Incorrect Startup Order specified, " +
                        "should start with 'cartridge.' or 'group.'");
            }
            aliasBasedComponents.add(aliasBasedComponent);
        }

        return new StartupOrder(aliasBasedComponents);
    }

    private static String getAliasForGroupName (String groupName, GroupContext groupContext) {

        for (GroupContext groupCtxt : groupContext.getGroupContexts()) {
            if (groupName.equals(groupCtxt.getName())) {
                return groupCtxt.getAlias();
            }
        }

        return null;
    }


    private static String getAliasForServiceType (String serviceType, GroupContext groupContext) {

        for (SubscribableContext subCtxt : groupContext.getSubscribableContexts()) {
            if (serviceType.equals(subCtxt.getType())) {
                return subCtxt.getAlias();
            }
        }

        return null;
    }
}
