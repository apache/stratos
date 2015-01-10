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

package org.apache.stratos.common.beans.application;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ApplicationAliasesBean implements Serializable {

    private static final long serialVersionUID = 3380699449827682550L;

    private String applicationId;
    private Set<String> cartridgeAliases;
    private Set<String> groupAliases;

    public ApplicationAliasesBean(String applicationId) {

        this.applicationId = applicationId;
        this.cartridgeAliases = new HashSet<String>();
        this.groupAliases = new HashSet<String>();
    }

    public Set<String> getGroupAliases() {
        return groupAliases;
    }

    public void addCartridgeAlias(String cartridgeSubscriptionAlias) {
        cartridgeAliases.add(cartridgeSubscriptionAlias);
    }

    public void addCartridgeAliases(Set<String> cartridgeSubscriptionAliases) {
        cartridgeSubscriptionAliases.addAll(cartridgeSubscriptionAliases);
    }

    public void addGroupAlias(String groupSubscriptionAlias) {
        groupAliases.add(groupSubscriptionAlias);
    }

    public void addGroupAliases(Set<String> groupSubscriptionAliases) {
        groupSubscriptionAliases.addAll(groupSubscriptionAliases);
    }

    public String getApplicationId() {
        return applicationId;
    }
}
