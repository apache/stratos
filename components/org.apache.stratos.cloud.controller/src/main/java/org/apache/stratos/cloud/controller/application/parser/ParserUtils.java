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

package org.apache.stratos.cloud.controller.application.parser;

import org.apache.stratos.cloud.controller.pojo.application.GroupContext;
import org.apache.stratos.cloud.controller.pojo.application.SubscribableContext;
import org.apache.stratos.messaging.domain.topology.StartupOrder;

import java.util.HashSet;
import java.util.Set;

public class ParserUtils {

	/*
    public static Set<StartupOrder> convert (org.apache.stratos.cloud.controller.pojo.StartupOrder [] startupOrderArr, GroupContext groupContext) {

        Set<StartupOrder> startupOrders = new HashSet<StartupOrder>();

        if (startupOrderArr == null) {
            return startupOrders;
        }


        for (int i = 0; i < startupOrderArr.length ; i++) {
            // convert all Startup Orders to aliases-based
            // start
            String startAlias;
            if (startupOrderArr[i].getStart().startsWith("cartridge.")) {
                String cartridgeType = startupOrderArr[i].getStart().substring(10);
                startAlias = getAliasForServiceType(cartridgeType, groupContext);
                if (startAlias == null) {
                    throw new RuntimeException("Unable convert Startup Order to alias-based; " +
                            "cannot find the matching alias for Service type " + cartridgeType);
                }

                startAlias = "cartridge.".concat(startAlias);

            } else if (startupOrderArr[i].getStart().startsWith("group."))  {
                String groupName = startupOrderArr[i].getStart().substring(6);
                startAlias = getAliasForGroupName(groupName, groupContext);
                if (startAlias == null) {
                    throw new RuntimeException("Unable convert Startup Order to alias-based; " +
                            "cannot find the matching alias for Group name " + groupName);
                }

                startAlias = "group.".concat(startAlias);

            } else {
                throw new RuntimeException("Incorrect Startup Order specified");
            }

            // after
            String afterAlias;
            if (startupOrderArr[i].getAfter().startsWith("cartridge.")) {
                String cartridgeType = startupOrderArr[i].getAfter().substring(10);
                afterAlias = getAliasForServiceType(cartridgeType, groupContext);
                if (afterAlias == null) {
                    throw new RuntimeException("Unable convert Startup Order to alias-based; " +
                            "cannot find the matching alias for Service type " + cartridgeType);
                }

                afterAlias = "cartridge.".concat(afterAlias);

            } else if (startupOrderArr[i].getAfter().startsWith("group."))  {
                String groupName = startupOrderArr[i].getAfter().substring(6);
                afterAlias = getAliasForGroupName(groupName, groupContext);
                if (afterAlias == null) {
                    throw new RuntimeException("Unable convert Startup Order to alias-based; " +
                            "cannot find the matching alias for Group name " + groupName);
                }

                afterAlias = "group.".concat(afterAlias);

            } else {
                throw new RuntimeException("Incorrect Startup Order specified");
            }

            startupOrders.add(new StartupOrder(startAlias, afterAlias));
        }

        return startupOrders;
    }

    private static String getAliasForGroupName (String groupName, GroupContext groupContext) {

        for (GroupContext groupCtxt : groupContext.getGroupContexts()) {
            if (groupName.equals(groupCtxt.getName())) {
                return groupCtxt.getAlias();
            }
        }

        return null;
    }

*/
    private static String getAliasForServiceType (String serviceType, GroupContext groupContext) {

        for (SubscribableContext subCtxt : groupContext.getSubscribableContexts()) {
            if (serviceType.equals(subCtxt.getType())) {
                return subCtxt.getAlias();
            }
        }

        return null;
    }
}
