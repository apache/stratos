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
import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.messaging.domain.application.signup.ArtifactRepository;
import org.apache.stratos.manager.exception.ArtifactDistributionCoordinatorException;
import org.apache.stratos.manager.messaging.publisher.InstanceNotificationPublisher;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.Applications;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;

import java.util.List;

/**
 * Artifact distribution coordinator.
 */
public class ArtifactDistributionCoordinator {

    private static final Log log = LogFactory.getLog(ArtifactDistributionCoordinator.class);

    private ApplicationSignUpHandler applicationSignUpManager;
    private InstanceNotificationPublisher publisher;


    public ArtifactDistributionCoordinator() {
        applicationSignUpManager = new ApplicationSignUpHandler();
        publisher = new InstanceNotificationPublisher();
    }

    /**
     * Notify artifact updated event for an application signup.
     * @param applicationId
     * @param tenantId
     * @throws ArtifactDistributionCoordinatorException
     */
    public void notifyArtifactUpdatedEventForSignUp(String applicationId, int tenantId) throws ArtifactDistributionCoordinatorException {
        notifyArtifactUpdatedEventForSignUp(applicationId, tenantId, null);
    }

    /**
     * Notify artifact updated event for an application signup, cluster.
     * @param applicationId
     * @param tenantId
     * @param clusterId
     * @throws ArtifactDistributionCoordinatorException
     */
    public void notifyArtifactUpdatedEventForSignUp(String applicationId, int tenantId, String clusterId)
            throws ArtifactDistributionCoordinatorException {

        try {
            ApplicationSignUp applicationSignUp = applicationSignUpManager.getApplicationSignUp(applicationId, tenantId);
            if (applicationSignUp == null) {
                throw new RuntimeException(String.format("Application signup not found: [application-id] %s " +
                        "[tenant-id] %d", applicationId, tenantId));
            }

            if (!artifactRepositoriesExist(applicationSignUp)) {
                log.warn(String.format("Artifact repositories not found for application signup, " +
                                "artifact updated event not sent: [application-id] %s [tenant-id] %s ",
                        applicationId, tenantId));
            } else {
                for (ArtifactRepository artifactRepository : applicationSignUp.getArtifactRepositories()) {
                    if (artifactRepository != null) {
                        String artifactRepositoryClusterId = findClusterId(applicationId, artifactRepository.getAlias());
                        if(StringUtils.isBlank(clusterId) || (clusterId.equals(artifactRepositoryClusterId))) {

                            publisher.publishArtifactUpdatedEvent(artifactRepositoryClusterId,
                                    String.valueOf(applicationSignUp.getTenantId()),
                                    artifactRepository.getRepoUrl(),
                                    artifactRepository.getRepoUsername(),
                                    artifactRepository.getRepoPassword(), false);

                            if (log.isInfoEnabled()) {
                                log.info(String.format("Artifact updated event published: [application-id] %s " +
                                                "[tenant-id] %d [cartridge-type] %s [alias] %s [repo-url] %s",
                                        applicationId,
                                        tenantId,
                                        artifactRepository.getCartridgeType(),
                                        artifactRepository.getAlias(),
                                        artifactRepository.getRepoUrl()));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
                String message = "Could not notify artifact updated event";
                log.error(message, e);
            throw new ArtifactDistributionCoordinatorException(message, e);
        }
    }

    /**
     * Notify artifact updated event for artifact repository.
     * @param repoUrl
     */
    public void notifyArtifactUpdatedEventForRepository(String repoUrl) throws ArtifactDistributionCoordinatorException {
        try {
            List<ApplicationSignUp> applicationSignUps = applicationSignUpManager.getApplicationSignUpsForRepository(repoUrl);
            if((applicationSignUps == null) || (applicationSignUps.size() == 0)) {
                if(log.isWarnEnabled()) {
                    log.warn(String.format("Artifact updated event not sent, " +
                            "application signups not found for repository: [repo-url] %s", repoUrl));
                    return;
                }

                for(ApplicationSignUp applicationSignUp : applicationSignUps) {
                    if(applicationSignUp.getArtifactRepositories() != null) {
                        for(ArtifactRepository artifactRepository : applicationSignUp.getArtifactRepositories()) {
                            if((artifactRepository != null) && (artifactRepository.getRepoUrl().equals(repoUrl))) {

                                String applicationId = applicationSignUp.getApplicationId();
                                int tenantId = applicationSignUp.getTenantId();
                                String clusterId = findClusterId(applicationId, artifactRepository.getAlias());

                                publisher.publishArtifactUpdatedEvent(clusterId,
                                        String.valueOf(applicationSignUp.getTenantId()),
                                        artifactRepository.getRepoUrl(),
                                        artifactRepository.getRepoUsername(),
                                        artifactRepository.getRepoPassword(), false);

                                if (log.isInfoEnabled()) {
                                    log.info(String.format("Artifact updated event published: [application-id] %s " +
                                                    "[tenant-id] %d [cartridge-type] %s [alias] %s [repo-url] %s",
                                            applicationId,
                                            tenantId,
                                            artifactRepository.getCartridgeType(),
                                            artifactRepository.getAlias(),
                                            artifactRepository.getRepoUrl()));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            String message = "Could not notify artifact updated event";
            log.error(message, e);
            throw new ArtifactDistributionCoordinatorException(message, e);
        }
    }

    private String findClusterId(String applicationId, String alias) {
        Applications applications = ApplicationManager.getApplications();
        if (applications == null) {
            throw new RuntimeException("Applications not found in application manager");
        }

        Application application = applications.getApplication(applicationId);
        if (application == null) {
            throw new RuntimeException(String.format("Application not found: [application-id] %s", applicationId));
        }

        ClusterDataHolder clusterData = application.getClusterData(alias);
        if(clusterData == null) {
            throw new RuntimeException(String.format("Cluster data not found for alias: [application-id] %s [alias] %s",
                    applicationId, alias));
        }
        return clusterData.getClusterId();
    }

    private boolean artifactRepositoriesExist(ApplicationSignUp applicationSignUp) {
        ArtifactRepository[] artifactRepositories = applicationSignUp.getArtifactRepositories();
        return ((artifactRepositories != null) && (artifactRepositories.length > 0) &&
                (artifactRepositories[0] != null));
    }
}
