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

import org.apache.stratos.cloud.controller.stub.pojo.CompositeApplicationDefinition;
import org.apache.stratos.manager.composite.application.beans.CompositeAppDefinition;
import org.apache.stratos.manager.composite.application.beans.GroupDefinition;
import org.apache.stratos.manager.composite.application.beans.SubscribableDefinition;
import org.apache.stratos.manager.composite.application.beans.SubscribableInfo;
import org.apache.stratos.manager.exception.CompositeApplicationDefinitionException;
import org.apache.stratos.manager.exception.PersistenceManagerException;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;

import java.util.List;

public class DefaultCompositeApplicationParser implements CompositeApplicationParser {

    DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager;

    public DefaultCompositeApplicationParser () {
        dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();
    }

    @Override
    public CompositeApplicationDefinition parse (Object compositeAppObj) throws CompositeApplicationDefinitionException {

        CompositeAppDefinition compositeAppDefinition = null;

        if (compositeAppObj instanceof CompositeAppDefinition) {
            compositeAppDefinition = (CompositeAppDefinition) compositeAppObj;

        } else {
            throw new CompositeApplicationDefinitionException("Invalid Composite Application definition");
        }

        if (compositeAppDefinition == null) {
            throw new CompositeApplicationDefinitionException("Composite Application definition not found");
        }

        String compositeAppId = compositeAppDefinition.getApplicationId();
        String compositeAppAlias = compositeAppDefinition.getAlias();

        // groups
        processGroups(compositeAppDefinition.getGroups());

        // get subscription related information
        if (compositeAppDefinition.getSubscribableInfo() != null) {
             // get the set (flat structure, not recursive) iterate and fill in..
        }

        return null;
    }

    // TODO: should return the relevant object type to send to CC
    private void processGroups (List<GroupDefinition> groups) throws CompositeApplicationDefinitionException {

        if (groups == null) {
            return;
        }

        for (GroupDefinition group : groups) {
            // process the group definitions
            String groupName = group.getName();
            String groupAlias = group.getAlias();

            // neither group name nor alias can be empty
            if (groupName == null || groupName.isEmpty()) {
                throw new CompositeApplicationDefinitionException("Group Name is invalid");
            }
            if (groupAlias == null || groupAlias.isEmpty()) {
                throw new CompositeApplicationDefinitionException("Group Alias is invalid");
            }

            // check if the group is deployed. if not can't continue
            if (!isGroupDeployed(groupName)) {
                throw new CompositeApplicationDefinitionException("No Service Group found with name [ " + groupName + " ]");
            }

            // get group level policy information
            String groupDepPolicy = group.getDeploymentPolicy();
            String groupScalePolicy = group.getAutoscalingPolicy();

            // subscribables
            processSubscribables(group.getSubscribables());

            // nested groups
            processGroups(group.getGroups());
        }
    }

    private boolean isGroupDeployed (String groupName) throws CompositeApplicationDefinitionException {

        ServiceGroupDefinition serviceGroupDefinition = null;

        try {
            serviceGroupDefinition = dataInsertionAndRetrievalManager.getServiceGroupDefinition(groupName);

        } catch (PersistenceManagerException e) {
            throw new CompositeApplicationDefinitionException(e);
        }

        return serviceGroupDefinition != null;
    }

    // TODO: should return the relevant object type to send to CC
    private void processSubscribables (List<SubscribableDefinition> subscribables) throws CompositeApplicationDefinitionException {

        if (subscribables == null) {
            return;
        }

        for (SubscribableDefinition subscribable : subscribables) {

            String cartridgeType = subscribable.getType();
            String subscriptionAlias = subscribable.getAlias();

            // neither cartridge type nor alias can be empty
            if (cartridgeType == null || cartridgeType.isEmpty()) {
                throw new CompositeApplicationDefinitionException("Cartridge Type is invalid");
            }
            if (subscriptionAlias == null || subscriptionAlias.isEmpty()) {
                throw new CompositeApplicationDefinitionException("Subscription Alias is invalid");
            }


        }
    }

    // TODO: should return the relevant object type to send to CC
    private void getSubscriptionInformation (List<SubscribableInfo> subscribables, String subscriptionAlias) throws CompositeApplicationDefinitionException {

        for (SubscribableInfo subscribable : subscribables) {

            if (subscribable.getAlias().equals(subscriptionAlias)) {
                // match found, retrieve the information
                String deploymentPolicy = subscribable.getDeploymentPolicy();
                String autoscalingPolicy = subscribable.getAutoscalingPolicy();
                String repoUrl = subscribable.getRepoUrl();
                if (repoUrl != null && !repoUrl.isEmpty()) {
                    boolean privateRepo = subscribable.isPrivateRepo();
                    String repoUsername = subscribable.getUsername();
                    String repoPassword = subscribable.getPassword();
                }
            }
        }
    }

}
