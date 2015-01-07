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

package org.apache.stratos.manager.components;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.domain.ArtifactRepository;
import org.apache.stratos.manager.registry.RegistryManager;
import org.apache.stratos.manager.domain.ApplicationSignUp;
import org.apache.stratos.manager.exception.ApplicationSignUpException;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.message.receiver.applications.ApplicationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application signup manager.
 */
public class ApplicationSignUpManager {

    private static final Log log = LogFactory.getLog(ApplicationSignUpManager.class);

    private static final String APPLICATION_SIGNUP_RESOURCE_PATH = "/stratos.manager/application.signups/";

    /**
     * Add application signup.
     * @param applicationSignUp
     * @throws ApplicationSignUpException
     */
    public String addApplicationSignUp(ApplicationSignUp applicationSignUp) throws ApplicationSignUpException {
        try {
            if(applicationSignUp == null) {
                throw new RuntimeException("Application signup is null");
            }

            if(log.isInfoEnabled()) {
                log.info(String.format("Adding application signup: [application-id] %s",
                        applicationSignUp.getApplicationId()));
            }

            // Generate application signup id
            String signUpId = UUID.randomUUID().toString();
            applicationSignUp.setSignUpId(signUpId);

            // Encrypt artifact repository passwords
            String applicationId = applicationSignUp.getApplicationId();
            Application application = ApplicationManager.getApplications().getApplication(applicationId);
            if(application == null) {
                throw new RuntimeException(String.format("Application not found: [application-id] %s", applicationId));
            }
            encryptRepositoryPasswords(applicationSignUp, application.getKey());

            // Persist application signup
            String resourcePath = APPLICATION_SIGNUP_RESOURCE_PATH + applicationSignUp.getSignUpId();
            RegistryManager.getInstance().persist(resourcePath, applicationSignUp);

            if(log.isInfoEnabled()) {
                log.info(String.format("Application signup added successfully: [application-id] %s [signup-id] %s",
                        applicationSignUp.getApplicationId(), applicationSignUp.getSignUpId()));
            }
            return signUpId;
        } catch (Exception e) {
            String message = "Could not add application signup";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    private void encryptRepositoryPasswords(ApplicationSignUp applicationSignUp, String applicationKey) {
        if(applicationSignUp.getArtifactRepositories() != null) {
            for(ArtifactRepository artifactRepository : applicationSignUp.getArtifactRepositories()) {
                String repoPassword = artifactRepository.getRepoPassword();
                if((artifactRepository != null) && (StringUtils.isNotBlank(repoPassword))) {
                    String encryptedRepoPassword = CommonUtil.encryptPassword(repoPassword,
                            applicationKey);
                    artifactRepository.setRepoPassword(encryptedRepoPassword);

                    if(log.isDebugEnabled()) {
                        log.debug(String.format("Artifact repository password encrypted: [application-id] %s " +
                                "[signup-id] %s [repo-url] %s", applicationSignUp.getApplicationId(),
                                applicationSignUp.getSignUpId(), artifactRepository.getRepoUrl()));
                    }
                }
            }
        }
    }

    /**
     * Remove application signup by signup id.
     * @param signUpId
     * @throws ApplicationSignUpException
     */
    public void removeApplicationSignUp(String signUpId) throws ApplicationSignUpException {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Removing application signup: [signup-id] %s", signUpId));
            }

            String resourcePath = APPLICATION_SIGNUP_RESOURCE_PATH + signUpId;
            ApplicationSignUp applicationSignUp = (ApplicationSignUp) RegistryManager.getInstance().read(resourcePath);
            if(applicationSignUp == null) {
                throw new RuntimeException(String.format("Application signup not found: [signup-id] %s", signUpId));
            }

            RegistryManager.getInstance().remove(resourcePath);

            if(log.isInfoEnabled()) {
                log.info(String.format("Application signup removed successfully: [application-id] %s [signup-id] %s",
                        applicationSignUp.getApplicationId(), applicationSignUp.getSignUpId()));
            }
        } catch (Exception e) {
            String message = "Could not add application signup";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    /**
     * Get application signup by signup id.
     * @param signUpId
     * @return
     * @throws ApplicationSignUpException
     */
    public ApplicationSignUp getApplicationSignUp(String signUpId) throws ApplicationSignUpException {
        try {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Reading application signup: [signup-id] %s", signUpId));
            }

            String resourcePath = APPLICATION_SIGNUP_RESOURCE_PATH + signUpId;
            ApplicationSignUp applicationSignUp = (ApplicationSignUp) RegistryManager.getInstance().read(resourcePath);
            return applicationSignUp;
        } catch (Exception e) {
            String message = "Could not get application signup";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    /**
     * Get application singups of an application by application id.
     * @param applicationId
     * @return
     * @throws ApplicationSignUpException
     */
    public List<ApplicationSignUp> getApplicationSignUps(String applicationId) throws ApplicationSignUpException {
        try {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Reading application signups: [application-id] %s", applicationId));
            }

            if(StringUtils.isBlank(applicationId)) {
                throw new RuntimeException("Application id is null");
            }
            List<ApplicationSignUp> applicationSignUps = new ArrayList<ApplicationSignUp>();

            String[] resourcePaths = (String[]) RegistryManager.getInstance().read(APPLICATION_SIGNUP_RESOURCE_PATH);
            if(resourcePaths != null) {
                for (String resourcePath : resourcePaths) {
                    if(resourcePath != null) {
                        ApplicationSignUp applicationSignUp = (ApplicationSignUp)
                                RegistryManager.getInstance().read(resourcePath);
                        if (applicationId.equals(applicationSignUp.getApplicationId())) {
                            applicationSignUps.add(applicationSignUp);
                        }
                    }
                }
            }

            return applicationSignUps;
        } catch (Exception e) {
            String message = "Could not get application signups";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    private List<ApplicationSignUp> getApplicationSignUps() throws ApplicationSignUpException {
        try {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Reading application signups"));
            }

            List<ApplicationSignUp> applicationSignUps = new ArrayList<ApplicationSignUp>();

            String[] resourcePaths = (String[]) RegistryManager.getInstance().read(APPLICATION_SIGNUP_RESOURCE_PATH);
            if(resourcePaths != null) {
                for (String resourcePath : resourcePaths) {
                    if(resourcePath != null) {
                        ApplicationSignUp applicationSignUp = (ApplicationSignUp)
                                RegistryManager.getInstance().read(resourcePath);
                            applicationSignUps.add(applicationSignUp);
                    }
                }
            }

            return applicationSignUps;
        } catch (Exception e) {
            String message = "Could not get application signups";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    /**
     * Get application signups for artifact repository
     * @param repoUrl
     * @return
     * @throws ApplicationSignUpException
     */
    public List<ApplicationSignUp> getApplicationSignUpsForRepository(String repoUrl) throws ApplicationSignUpException {
        try {
            List<ApplicationSignUp> filteredResult = new ArrayList<ApplicationSignUp>();

            List<ApplicationSignUp> applicationSignUps = getApplicationSignUps();
            for(ApplicationSignUp applicationSignUp : applicationSignUps) {
                if(applicationSignUp.getArtifactRepositories() != null) {
                    for(ArtifactRepository artifactRepository : applicationSignUp.getArtifactRepositories()) {
                        if(artifactRepository != null) {
                            if(artifactRepository.getRepoUrl().equals(repoUrl)) {
                                filteredResult.add(applicationSignUp);
                                break;
                            }
                        }
                    }
                }
            }

            return filteredResult;
        } catch (Exception e) {
            String message = "Could not get artifact repositories for repository: [repo-url] " + repoUrl;
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }
}
