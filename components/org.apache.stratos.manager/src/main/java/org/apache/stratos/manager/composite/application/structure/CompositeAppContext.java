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

public class CompositeAppContext {

    private String appId;

    private List<GroupContext> groupContexts;

    private List<SubscribableContext> subscribableContexts;

    private List<StartupOrder> startupOrder;

    private String killBehaviour;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public List<GroupContext> getGroupContexts() {
        return groupContexts;
    }

    public void setGroupContexts(List<GroupContext> groupContexts) {
        this.groupContexts = groupContexts;
    }

    public List<SubscribableContext> getSubscribableContexts() {
        return subscribableContexts;
    }

    public void setSubscribableContexts(List<SubscribableContext> subscribableContexts) {
        this.subscribableContexts = subscribableContexts;
    }

    public List<StartupOrder> getStartupOrder() {
        return startupOrder;
    }

    public void setStartupOrder(List<StartupOrder> startupOrder) {
        this.startupOrder = startupOrder;
    }

    public String getKillBehaviour() {
        return killBehaviour;
    }

    public void setKillBehaviour(String killBehaviour) {
        this.killBehaviour = killBehaviour;
    }
}
