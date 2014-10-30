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

package org.apache.stratos.manager.subscription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.jboss.util.propertyeditor.StringArrayEditor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ApplicationSubscription implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 3380699449827682550L;

	private static Log log = LogFactory.getLog(ApplicationSubscription.class);

    private String appId;

    private Set<String> cartridgeSubscriptionAliases;

    private Set<String> groupSubscriptionAliases;

    public ApplicationSubscription (String appId) {

        this.appId = appId;
        cartridgeSubscriptionAliases = new HashSet<String>();
        groupSubscriptionAliases = new HashSet<String>();
    }

    public Set<String> getGroupSubscriptionAliases() {
        return groupSubscriptionAliases;
    }

    public void addCartridgeSubscriptionAlias (String cartridgeSubscriptionAlias) {
        cartridgeSubscriptionAliases.add(cartridgeSubscriptionAlias);
    }

    public void addCartridgeSubscriptionAliases (Set<String> cartridgeSubscriptionAliases) {
        cartridgeSubscriptionAliases.addAll(cartridgeSubscriptionAliases);
    }

    public void addGroupSubscriptionAlias (String groupSubscriptionAlias) {
        groupSubscriptionAliases.add(groupSubscriptionAlias);
    }

    public void addGroupSubscriptionAliases (Set<String> groupSubscriptionAliases) {
    	groupSubscriptionAliases.addAll(groupSubscriptionAliases);
    }

    public String getAppId() {
        return appId;
    }
}
