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

package org.apache.stratos.manager.services.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.components.ApplicationSignUpHandler;
import org.apache.stratos.manager.components.ArtifactDistributionCoordinator;
import org.apache.stratos.manager.components.DomainMappingHandler;
import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.manager.exception.ApplicationSignUpException;
import org.apache.stratos.manager.exception.ArtifactDistributionCoordinatorException;
import org.apache.stratos.manager.exception.DomainMappingException;
import org.apache.stratos.manager.services.StratosManagerService;
import org.apache.stratos.messaging.domain.application.signup.DomainMapping;

/**
 * Stratos manager service implementation.
 */
public class StratosManagerServiceImpl implements StratosManagerService {

    private static final Log log = LogFactory.getLog(StratosManagerServiceImpl.class);

    private ApplicationSignUpHandler signUpHandler;
    private ArtifactDistributionCoordinator artifactDistributionCoordinator;
    private DomainMappingHandler domainMappingHandler;

    public StratosManagerServiceImpl() {
        signUpHandler = new ApplicationSignUpHandler();
        artifactDistributionCoordinator = new ArtifactDistributionCoordinator();
        domainMappingHandler = new DomainMappingHandler();
    }

    @Override
    public void addApplicationSignUp(ApplicationSignUp applicationSignUp) throws ApplicationSignUpException {
        signUpHandler.addApplicationSignUp(applicationSignUp);
    }

    @Override
    public void removeApplicationSignUp(String applicationId, int tenantId) throws ApplicationSignUpException {
        signUpHandler.removeApplicationSignUp(applicationId, tenantId);
    }

    @Override
    public ApplicationSignUp getApplicationSignUp(String applicationId, int tenantId) throws ApplicationSignUpException {
        return signUpHandler.getApplicationSignUp(applicationId, tenantId);
    }

    @Override
    public ApplicationSignUp[] getApplicationSignUps(String applicationId) throws ApplicationSignUpException {
        return signUpHandler.getApplicationSignUps(applicationId);
    }

    @Override
    public void notifyArtifactUpdatedEventForSignUp(String applicationId, int tenantId) throws ArtifactDistributionCoordinatorException {
        artifactDistributionCoordinator.notifyArtifactUpdatedEventForSignUp(applicationId, tenantId);
    }

    @Override
    public void notifyArtifactUpdatedEventForRepository(String repoUrl) throws ArtifactDistributionCoordinatorException {
        artifactDistributionCoordinator.notifyArtifactUpdatedEventForRepository(repoUrl);
    }

    @Override
    public void addDomainMapping(DomainMapping domainMapping) throws DomainMappingException {
        domainMappingHandler.addDomainMapping(domainMapping);
    }

    @Override
    public DomainMapping[] getDomainMappings(String applicationId, int tenantId) throws DomainMappingException {
        return domainMappingHandler.getDomainMappings(applicationId, tenantId);
    }

    @Override
    public void removeDomainMapping(String applicationId, int tenantId, String domainName) throws DomainMappingException {
        domainMappingHandler.removeDomainMapping(applicationId, tenantId, domainName);
    }
}
