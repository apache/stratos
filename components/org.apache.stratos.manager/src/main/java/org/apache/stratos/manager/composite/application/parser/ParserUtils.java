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

package org.apache.stratos.manager.composite.application.parser;

import org.apache.stratos.manager.composite.application.beans.GroupDefinition;
import org.apache.stratos.manager.composite.application.beans.SubscribableDefinition;
import org.apache.stratos.manager.composite.application.beans.SubscribableInfo;
import org.apache.stratos.manager.composite.application.structure.GroupContext;
import org.apache.stratos.manager.composite.application.structure.StartupOrder;
import org.apache.stratos.manager.composite.application.structure.SubscribableContext;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.grouping.definitions.StartupOrderDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParserUtils {

    public static SubscribableContext convert (SubscribableDefinition subscribable, SubscribableInfo subscribableInfo) {

        SubscribableContext subscribableContext = new SubscribableContext(subscribable.getType(), subscribable.getAlias());
        subscribableContext.setAutoscalingPolicy(subscribableInfo.getAutoscalingPolicy());
        subscribableContext.setDeploymentPolicy(subscribableInfo.getDeploymentPolicy());

        if (subscribableInfo.getRepoUrl() != null && !subscribableInfo.getRepoUrl().isEmpty()) {
            subscribableContext.setRepoUrl(subscribableInfo.getRepoUrl());
            subscribableContext.setPrivateRepo(subscribableInfo.isPrivateRepo());
            subscribableContext.setUsername(subscribableInfo.getUsername());
            subscribableContext.setPassword(subscribableInfo.getPassword());
        }

        return subscribableContext;
    }

    public static Set<StartupOrder> convert (List<StartupOrderDefinition> startupOrderDefinitions) {

        Set<StartupOrder> startupOrders = new HashSet<StartupOrder>();
        for (StartupOrderDefinition startupOrderDefinition : startupOrderDefinitions) {
            StartupOrder startupOrder = new StartupOrder(startupOrderDefinition.getStart(), startupOrderDefinition.getAfter());
            startupOrders.add(startupOrder);
        }

        return startupOrders;
    }
}
