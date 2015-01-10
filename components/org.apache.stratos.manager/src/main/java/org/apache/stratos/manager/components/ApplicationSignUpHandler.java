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
import org.apache.stratos.manager.messaging.publisher.ApplicationSignUpEventPublisher;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.messaging.domain.application.signup.ArtifactRepository;
import org.apache.stratos.manager.exception.ApplicationSignUpException;
import org.apache.stratos.manager.registry.RegistryManager;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Application signup handler.
 */
public class ApplicationSignUpHandler {

    private static final Log log = LogFactory.getLog(ApplicationSignUpHandler.class);

    private static final String APPLICATION_SIGNUP_RESOURCE_PATH = "/stratos.manager/application.signups/";

    private String prepareApplicationSignupResourcePath(String applicationId, int tenantId) {
        return APPLICATION_SIGNUP_RESOURCE_PATH + applicationId + "-tenant-" + tenantId;
    }

    /**
     * Add application signup.
     *
     * @param applicationSignUp
     * @throws ApplicationSignUpException
     */
    public void addApplicationSignUp(ApplicationSignUp applicationSignUp) throws ApplicationSignUpException {
        if (applicationSignUp == null) {
            throw new ApplicationSignUpException("Application signup is null");
        }

        String applicationId = applicationSignUp.getApplicationId();
        int tenantId = applicationSignUp.getTenantId();
        List<String> clusterIds = applicationSignUp.getClusterIds();

        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Adding application signup: [application-id] %s [tenant-id] %d",
                        applicationId, tenantId));
            }

            if (applicationSignUpExist(applicationId, tenantId)) {
                throw new RuntimeException(String.format("Tenant has already signed up for application: " +
                        "[application-id] %s [tenant-id] %d", applicationId, tenantId));
            }

            // Persist application signup
            String resourcePath = prepareApplicationSignupResourcePath(applicationId, tenantId);
            RegistryManager.getInstance().persist(resourcePath, applicationSignUp);

            ApplicationSignUpEventPublisher.publishApplicationSignUpAddedEvent(applicationId, tenantId, clusterIds);

            if (log.isInfoEnabled()) {
                log.info(String.format("Application signup added successfully: [application-id] %s [tenant-id] %d",
                        applicationId, tenantId));
            }
        } catch (Exception e) {
            String message = "Could not add application signup";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    /**
     * Check application signup availability.
     * @param applicationId
     * @param tenantId
     * @return
     * @throws ApplicationSignUpException
     */
    public boolean applicationSignUpExist(String applicationId, int tenantId) throws ApplicationSignUpException {
        try {
            String resourcePath = prepareApplicationSignupResourcePath(applicationId, tenantId);
            ApplicationSignUp applicationSignUp = (ApplicationSignUp) RegistryManager.getInstance().read(resourcePath);
            return (applicationSignUp != null);
        } catch (Exception e) {
            String message = "Could not check application signup availability";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    /**
     * Remove application signup by application id, tenant id.
     *
     * @param applicationId
     * @param tenantId
     * @throws ApplicationSignUpException
     */
    public void removeApplicationSignUp(String applicationId, int tenantId) throws ApplicationSignUpException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Removing application signup: [application-id] %s [tenant-id] %d",
                        applicationId, tenantId));
            }

            if (!applicationSignUpExist(applicationId, tenantId)) {
                throw new RuntimeException(String.format("Application signup not found: [application-id] %s " +
                        "[tenant-id] %d", applicationId, tenantId));
            }

            String resourcePath = prepareApplicationSignupResourcePath(applicationId, tenantId);
            RegistryManager.getInstance().remove(resourcePath);

            ApplicationSignUpEventPublisher.publishApplicationSignUpRemovedEvent(applicationId, tenantId);

            if (log.isInfoEnabled()) {
                log.info(String.format("Application signup removed successfully: [application-id] %s [tenant-id] %d",
                        applicationId, tenantId));
            }
        } catch (Exception e) {
            String message = "Could not remove application signup";
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    /**
     * Get application signup by application id, tenant id.
     *
     * @param applicationId
     * @param tenantId
     * @return
     * @throws ApplicationSignUpException
     */
    public ApplicationSignUp getApplicationSignUp(String applicationId, int tenantId) throws ApplicationSignUpException {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Get application signup: [application-id] %s [tenant-id] %d",
                        applicationId, tenantId));
            }

            String resourcePath = prepareApplicationSignupResourcePath(applicationId, tenantId);
            ApplicationSignUp applicationSignUp = (ApplicationSignUp) RegistryManager.getInstance().read(resourcePath);
            return applicationSignUp;
        } catch (Exception e) {
            String message = String.format("Could not get application signup: [application-id] %s [tenant-id] %d",
                    applicationId, tenantId);
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    /**
     * Get application singups available of an application by application id.
     *
     * @param applicationId
     * @return
     * @throws ApplicationSignUpException
     */
    public ApplicationSignUp[] getApplicationSignUps(String applicationId) throws ApplicationSignUpException {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Get application signups: [application-id] %s", applicationId));
            }

            if (StringUtils.isBlank(applicationId)) {
                throw new RuntimeException("Application id is null");
            }
            List<ApplicationSignUp> applicationSignUps = new ArrayList<ApplicationSignUp>();

            String[] resourcePaths = (String[]) RegistryManager.getInstance().read(APPLICATION_SIGNUP_RESOURCE_PATH);
            if (resourcePaths != null) {
                for (String resourcePath : resourcePaths) {
                    if (resourcePath != null) {
                        ApplicationSignUp applicationSignUp = (ApplicationSignUp)
                                RegistryManager.getInstance().read(resourcePath);
                        if (applicationId.equals(applicationSignUp.getApplicationId())) {
                            applicationSignUps.add(applicationSignUp);
                        }
                    }
                }
            }

            return applicationSignUps.toArray(new ApplicationSignUp[applicationSignUps.size()]);
        } catch (Exception e) {
            String message = "Could not get application signups: [application-id] " + applicationId;
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }

    /**
     * Get all application signups.
     * @return
     * @throws ApplicationSignUpException
     */
    public List<ApplicationSignUp> getApplicationSignUps() throws ApplicationSignUpException {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Reading application signups"));
            }

            List<ApplicationSignUp> applicationSignUps = new ArrayList<ApplicationSignUp>();

            String[] resourcePaths = (String[]) RegistryManager.getInstance().read(APPLICATION_SIGNUP_RESOURCE_PATH);
            if (resourcePaths != null) {
                for (String resourcePath : resourcePaths) {
                    if (resourcePath != null) {
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
     *
     * @param repoUrl
     * @return
     * @throws ApplicationSignUpException
     */
    public List<ApplicationSignUp> getApplicationSignUpsForRepository(String repoUrl) throws ApplicationSignUpException {
        try {
            List<ApplicationSignUp> filteredResult = new ArrayList<ApplicationSignUp>();

            List<ApplicationSignUp> applicationSignUps = getApplicationSignUps();
            for (ApplicationSignUp applicationSignUp : applicationSignUps) {
                if (applicationSignUp.getArtifactRepositories() != null) {
                    for (ArtifactRepository artifactRepository : applicationSignUp.getArtifactRepositories()) {
                        if (artifactRepository != null) {
                            if (artifactRepository.getRepoUrl().equals(repoUrl)) {
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

    public void updateApplicationSignUp(ApplicationSignUp applicationSignUp) throws ApplicationSignUpException {

        String applicationId = applicationSignUp.getApplicationId();
        int tenantId = applicationSignUp.getTenantId();

        try {
            // Persist application signup
            String resourcePath = prepareApplicationSignupResourcePath(applicationId, tenantId);
            RegistryManager.getInstance().persist(resourcePath, applicationSignUp);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Application signup updated successfully: [application-id] %s [tenant-id] %d",
                        applicationId, tenantId));
            }
        } catch (Exception e) {
            String message = String.format("Could not get application signup: [application-id] %s [tenant-id] %d",
                    applicationId, tenantId);
            log.error(message, e);
            throw new ApplicationSignUpException(message, e);
        }
    }
}
