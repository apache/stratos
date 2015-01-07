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

import org.apache.stratos.manager.domain.ApplicationSignUp;
import org.apache.stratos.manager.exception.ApplicationSignUpException;
import org.apache.stratos.manager.exception.ArtifactDistributionCoordinatorException;

import java.util.List;

/**
 * Stratos manager service interface.
 */
public interface StratosManagerService {

    /**
     * Add application signup
     * @param applicationSignUp
     * @return signup id
     * @throws ApplicationSignUpException
     */
    public String addApplicationSignUp(ApplicationSignUp applicationSignUp) throws ApplicationSignUpException;

    /**
     * Remove application signup.
     * @param signUpId
     */
    public void removeApplicationSignUp(String signUpId) throws ApplicationSignUpException;

    /**
     * Get application signup.
     * @param signUpId
     * @return
     */
    public ApplicationSignUp getApplicationSignUp(String signUpId) throws ApplicationSignUpException;

    /**
     * Get application signups.
     * @return
     */
    public List<ApplicationSignUp> getApplicationSignUps(String applicationId) throws ApplicationSignUpException;

    /**
     * Notify artifact updated event for application signup.
     * @param signUpId
     * @throws ArtifactDistributionCoordinatorException
     */
    public void notifyArtifactUpdatedEventForSignUp(String signUpId) throws ArtifactDistributionCoordinatorException;

    /**
     * Notify artifact updated event for artifact repository.
     * @param repoUrl
     * @throws ArtifactDistributionCoordinatorException
     */
    public void notifyArtifactUpdatedEventForRepository(String repoUrl) throws ArtifactDistributionCoordinatorException;
}
