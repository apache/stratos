/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.integration.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.integration.tests.rest.RestClient;

/**
 * Test to handle application CRUD operations, deploy and undeploy
 */
public class ApplicationTest {
    private static final Log log = LogFactory.getLog(ApplicationTest.class);
    String applications = "/applications/simple/single-cartridge-app/";
    String applicationsUpdate = "/applications/simple/single-cartridge-app/update/";
    private static final String entityName = "application";

    public boolean addApplication(String applicationId, RestClient restClient) {
        return restClient.addEntity(applications + "/" + applicationId,
                RestConstants.APPLICATIONS, entityName);
    }

    public ApplicationBean getApplication(String applicationId,
                                          RestClient restClient) {
        ApplicationBean bean = (ApplicationBean) restClient.
                getEntity(RestConstants.APPLICATIONS, applicationId,
                        ApplicationBean.class, entityName);
        return bean;
    }

    public boolean updateApplication(String applicationId, RestClient restClient) {
        return restClient.updateEntity(applicationsUpdate + "/" + applicationId,
                RestConstants.APPLICATIONS, entityName);
    }

    public boolean removeApplication(String applicationId, RestClient restClient) {
        return restClient.removeEntity(RestConstants.APPLICATIONS, applicationId, entityName);

    }

    public boolean deployApplication(String applicationId, String applicationPolicyId,
                                     RestClient restClient) {
        return restClient.deployEntity(RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_DEPLOY + "/" + applicationPolicyId, entityName);
    }

    public boolean undeployApplication(String applicationId,
                                       RestClient restClient) {
        return restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_UNDEPLOY, entityName);
    }

    public boolean forceUndeployApplication(String applicationId,
                                            RestClient restClient) {
        return restClient.undeployEntity(RestConstants.APPLICATIONS + "/" + applicationId +
                RestConstants.APPLICATIONS_UNDEPLOY + "?force=true", entityName);
    }

}
