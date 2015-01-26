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

package org.apache.stratos.manager.services;

import java.util.List;

import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.manager.exception.ApplicationSignUpException;
import org.apache.stratos.manager.exception.ArtifactDistributionCoordinatorException;
import org.apache.stratos.manager.exception.DomainMappingException;
import org.apache.stratos.messaging.domain.application.signup.DomainMapping;

/**
 * Stratos manager service interface.
 */
public interface StratosManagerService {

    /**
     * Add application signup
     * @param applicationSignUp
     * @throws ApplicationSignUpException
     */
    public void addApplicationSignUp(ApplicationSignUp applicationSignUp) throws ApplicationSignUpException;

    /**
     * Remove application signup.
     * @param applicationId
     * @param tenantId
     */
    public void removeApplicationSignUp(String applicationId, int tenantId) throws ApplicationSignUpException;

    /**
     * Get application signup.
     * @param applicationId
     * @param tenantId
     * @return
     */
    public ApplicationSignUp getApplicationSignUp(String applicationId, int tenantId) throws ApplicationSignUpException;

    /**
     * Get application signups available for an application.
     * @return
     */
    public ApplicationSignUp[] getApplicationSignUps(String applicationId) throws ApplicationSignUpException;

    /**
     * Notify artifact updated event for application signup.
     * @param applicationId
     * @param tenantId
     * @throws ArtifactDistributionCoordinatorException
     */
    public void notifyArtifactUpdatedEventForSignUp(String applicationId, int tenantId)
            throws ArtifactDistributionCoordinatorException;

    /**
     * Notify artifact updated event for artifact repository.
     * @param repoUrl
     * @throws ArtifactDistributionCoordinatorException
     */
    public void notifyArtifactUpdatedEventForRepository(String repoUrl) throws ArtifactDistributionCoordinatorException;

    /**
     * Add domain mapping
     * @param domainMapping
     * @throws DomainMappingException
     */
    public void addDomainMapping(DomainMapping domainMapping) throws DomainMappingException;

    /**
     * Get domain mappings available for application signup.
     * @param applicationId
     * @param tenantId
     * @return
     * @throws DomainMappingException
     */
    public DomainMapping[] getDomainMappings(String applicationId, int tenantId) throws DomainMappingException;

    /**
     * Remove domain mapping by domain name.
     * @param applicationId
     * @param tenantId
     * @param domainName
     * @throws DomainMappingException
     */
    public void removeDomainMapping(String applicationId, int tenantId, String domainName) throws DomainMappingException;
    
    /**
     * Adds the used cartridges in cartridge groups to cache structure.
     *
     * @param cartridgeGroupName the cartridge group name
     * @param cartridgeNames the cartridge names
     */
    public void addUsedCartridgesInCartridgeGroups(String cartridgeGroupName, String[] cartridgeNames);
    
    /**
     * Removes the used cartridges in cartridge groups from cache structure.
     *
     * @param cartridgeGroupName the cartridge group name
     * @param cartridgeNames the cartridge names
     */
    public void removeUsedCartridgesInCartridgeGroups(String cartridgeGroupName, String[] cartridgeNames);
    
    /**
     * Adds the used cartridges in applications to cache structure.
     *
     * @param applicationName the application name
     * @param cartridgeNames the cartridge names
     */
    public void addUsedCartridgesInApplications(String applicationName, String[] cartridgeNames);
    
    /**
     * Removes the used cartridges in applications from cache structure.
     *
     * @param applicationName the application name
     * @param cartridgeNames the cartridge names
     */
    public void removeUsedCartridgesInApplications(String applicationName, String[] cartridgeNames);
    
    /**
     * Verifies whether a cartridge can be removed.
     *
     * @param cartridgeName the cartridge name
     * @return true, if successful
     */
    public boolean canCartridgeBeRemoved(String cartridgeName);
    
    /**
     * Adds the used cartridge groups in cartridge sub groups to cache structure.
     *
     * @param cartridgeSubGroupName the cartridge sub group name
     * @param cartridgeGroupNames the cartridge group names
     */
    public void addUsedCartridgeGroupsInCartridgeSubGroups(String cartridgeSubGroupName, String[] cartridgeGroupNames);
    
    /**
     * Removes the used cartridge groups in cartridge sub groups from cache structure.
     *
     * @param cartridgeSubGroupName the cartridge sub group name
     * @param cartridgeGroupNames the cartridge group names
     */
    public void removeUsedCartridgeGroupsInCartridgeSubGroups(String cartridgeSubGroupName, String[] cartridgeGroupNames);
    
    /**
     * Adds the used cartridge groups in applications to cache structure.
     *
     * @param applicationName the application name
     * @param cartridgeGroupNames the cartridge group names
     */
    public void addUsedCartridgeGroupsInApplications(String applicationName, String[] cartridgeGroupNames);
    
    /**
     * Removes the used cartridge groups in applications from cache structure.
     *
     * @param applicationName the application name
     * @param cartridgeGroupNames the cartridge group names
     */
    public void removeUsedCartridgeGroupsInApplications(String applicationName, String[] cartridgeGroupNames);
    
    /**
     * Verifies whether a cartridge group can be removed.
     *
     * @param cartridgeGroupName the cartridge group name
     * @return true, if successful
     */
    public boolean canCartirdgeGroupBeRemoved(String cartridgeGroupName);
}
