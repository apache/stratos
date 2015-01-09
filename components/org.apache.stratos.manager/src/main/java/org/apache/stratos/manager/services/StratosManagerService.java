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

import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.manager.exception.ApplicationSignUpException;
import org.apache.stratos.manager.exception.ArtifactDistributionCoordinatorException;
import org.apache.stratos.manager.exception.DomainMappingException;
import org.apache.stratos.messaging.domain.domain.mapping.DomainMapping;

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
}
