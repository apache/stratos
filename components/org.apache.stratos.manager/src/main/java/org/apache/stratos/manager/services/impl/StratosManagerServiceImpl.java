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
import org.apache.stratos.manager.components.ApplicationSignUpManager;
import org.apache.stratos.manager.components.ArtifactDistributionCoordinator;
import org.apache.stratos.manager.domain.ApplicationSignUp;
import org.apache.stratos.manager.exception.ApplicationSignUpException;
import org.apache.stratos.manager.exception.ArtifactDistributionCoordinatorException;
import org.apache.stratos.manager.services.StratosManagerService;

import java.util.List;

/**
 * Stratos manager service implementation.
 */
public class StratosManagerServiceImpl implements StratosManagerService {

    private static final Log log = LogFactory.getLog(StratosManagerServiceImpl.class);

    private ApplicationSignUpManager signUpManager;
    private ArtifactDistributionCoordinator artifactDistributionCoordinator;

    public StratosManagerServiceImpl() {
        signUpManager = new ApplicationSignUpManager();
        artifactDistributionCoordinator = new ArtifactDistributionCoordinator();
    }

    @Override
    public String addApplicationSignUp(ApplicationSignUp applicationSignUp) throws ApplicationSignUpException {
        return signUpManager.addApplicationSignUp(applicationSignUp);
    }

    @Override
    public void removeApplicationSignUp(String signUpId) throws ApplicationSignUpException {
        signUpManager.removeApplicationSignUp(signUpId);
    }

    @Override
    public ApplicationSignUp getApplicationSignUp(String signUpId) throws ApplicationSignUpException {
        return signUpManager.getApplicationSignUp(signUpId);
    }

    @Override
    public List<ApplicationSignUp> getApplicationSignUps(String applicationId) throws ApplicationSignUpException {
        return signUpManager.getApplicationSignUps(applicationId);
    }

    @Override
    public void notifyArtifactUpdatedEventForSignUp(String signUpId) throws ArtifactDistributionCoordinatorException {
        artifactDistributionCoordinator.notifyArtifactUpdatedEventForSignUp(signUpId);
    }

    @Override
    public void notifyArtifactUpdatedEventForRepository(String repoUrl) throws ArtifactDistributionCoordinatorException {
        artifactDistributionCoordinator.notifyArtifactUpdatedEventForRepository(repoUrl);
    }
}
