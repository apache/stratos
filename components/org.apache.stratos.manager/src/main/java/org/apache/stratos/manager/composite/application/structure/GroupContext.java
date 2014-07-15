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

package org.apache.stratos.manager.composite.application.structure;

import java.util.List;
import java.util.Set;

public class GroupContext {

    private String name;

    private String alias;

    private String deploymentPolicy;

    private String autoscalingPolicy;

    private Set<GroupContext> groupContexts;

    private Set<SubscribableContext> subscribableContexts;

    private Set<StartupOrder> startupOrder;

    private String killBehaviour;


    public Set<StartupOrder> getStartupOrder() {
        return startupOrder;
    }

    public void setStartupOrder(Set<StartupOrder> startupOrder) {
        this.startupOrder = startupOrder;
    }

    public String getKillBehaviour() {
        return killBehaviour;
    }

    public void setKillBehaviour(String killBehaviour) {
        this.killBehaviour = killBehaviour;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(String deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public String getAutoscalingPolicy() {
        return autoscalingPolicy;
    }

    public void setAutoscalingPolicy(String autoscalingPolicy) {
        this.autoscalingPolicy = autoscalingPolicy;
    }

    public Set<GroupContext> getGroupContexts() {
        return groupContexts;
    }

    public void setGroupContexts(Set<GroupContext> groupContexts) {
        this.groupContexts = groupContexts;
    }

    public Set<SubscribableContext> getSubscribableContexts() {
        return subscribableContexts;
    }

    public void setSubscribableContexts(Set<SubscribableContext> subscribableContexts) {
        this.subscribableContexts = subscribableContexts;
    }
}
