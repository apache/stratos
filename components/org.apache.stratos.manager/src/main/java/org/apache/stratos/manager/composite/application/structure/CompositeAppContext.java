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

import java.util.Properties;
import java.util.Set;

public class CompositeAppContext {

    private String appId;

    private Set<GroupContext> groupContexts;

    private Set<SubscribableContext> subscribableContexts;

    private Set<StartupOrder> startupOrder;
    
    private String [] startupOrders;

    private String killBehaviour;

    private Properties properties;

    public CompositeAppContext (String appId) {
        this.appId = appId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
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

    public String [] getStartupOrders() {
		return startupOrders;
	}

	public void setStartupOrders(String [] startupOrders) {
		this.startupOrders = startupOrders;
	}

	public boolean equals(Object other) {

        if(this == other) {
            return true;
        }
        if(!(other instanceof CompositeAppContext)) {
            return false;
        }

        CompositeAppContext that = (CompositeAppContext)other;
        return this.appId.equals(that.appId);
    }

    public int hashCode () {

        return appId.hashCode();
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
