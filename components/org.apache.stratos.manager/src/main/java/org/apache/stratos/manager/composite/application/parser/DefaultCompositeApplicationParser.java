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

import org.apache.commons.lang3.StringUtils;
import org.apache.stratos.cloud.controller.stub.pojo.CompositeApplicationDefinition;
import org.apache.stratos.manager.composite.application.beans.*;
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
        if(StringUtils.isEmpty(compositeAppId)){
            throw new CompositeApplicationDefinitionException("Application ID can not be empty");
        }
        String compositeAppAlias = compositeAppDefinition.getAlias();

        // components
        processComponents(compositeAppDefinition.getComponents());

        return null;
    }

    // TODO: should return the relevant object type to send to CC
    private void processComponents(List<ComponentDefinition> components) throws CompositeApplicationDefinitionException {

        if (components == null) {
            return;
        }

        for (ComponentDefinition component : components) {
            // process the group definitions
            String groupName = component.getGroup();
            String groupAlias = component.getAlias();

            // neither group name nor alias can be empty
            if (StringUtils.isEmpty(groupName)) {
                throw new CompositeApplicationDefinitionException("Group Name is invalid");
            }
            if (StringUtils.isEmpty(groupAlias)) {
                throw new CompositeApplicationDefinitionException("Group Alias is invalid");
            }

            // check if the group is deployed. if not can't continue
            if (!isGroupDeployed(groupName)) {
                throw new CompositeApplicationDefinitionException(String.format("No Service Group found with name [ %s ]", groupName));
            }

            // get group level policy information
            String groupDepPolicy = component.getDeploymentPolicy();
            String groupScalePolicy = component.getAutoscalingPolicy();

            // subscribables
            processSubscribables(component.getSubscribables());
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
    private void processSubscribables (List<SubscribableInfo> subscribables) throws CompositeApplicationDefinitionException {

        if (subscribables == null) {
            return;
        }

        for (SubscribableInfo subscribable : subscribables) {

            String cartridgeType = subscribable.getType();
            String subscriptionAlias = subscribable.getAlias();

            // neither cartridge type nor alias can be empty
            if (cartridgeType == null || cartridgeType.isEmpty()) {
                throw new CompositeApplicationDefinitionException("Cartridge Type is invalid");
            }
            if (subscriptionAlias == null || subscriptionAlias.isEmpty()) {
                throw new CompositeApplicationDefinitionException("Subscription Alias is invalid");
            }
        // TODO should validate if there exist a cartridge with  $cartridgeType

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
