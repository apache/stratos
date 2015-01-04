/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.stratos.manager.utils;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.Property;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * This class contains utility methods used by ApplicationManagementService.
 */
public class ApplicationManagementUtil {

    private static Log log = LogFactory.getLog(ApplicationManagementUtil.class);

    public static int getTenantId(ConfigurationContext configurationContext) {
        int tenantId = MultitenantUtils.getTenantId(configurationContext);
        if(log.isDebugEnabled()) {
            log.debug("Returning tenant ID : " + tenantId);
        }
        return tenantId;
    }
    
    public static org.apache.stratos.cloud.controller.stub.Properties toCCStubProperties(
            org.apache.stratos.common.Properties properties) {
        org.apache.stratos.cloud.controller.stub.Properties stubProps = new org.apache.stratos.cloud.controller.stub.Properties();

        if (properties != null && properties.getProperties() != null) {

            for (Property property : properties.getProperties()) {
                if ((property != null) && (property.getValue() != null)) {
                    org.apache.stratos.cloud.controller.stub.Property newProperty = new org.apache.stratos.cloud.controller.stub.Property();
                    newProperty.setName(property.getName());
                    newProperty.setValue(String.valueOf(property.getValue()));
                    stubProps.addProperties(newProperty);
                }
            }

        }
        return stubProps;
    }

    public static org.apache.stratos.common.Properties toCommonProperties(
            org.apache.stratos.cloud.controller.stub.Properties properties) {
        org.apache.stratos.common.Properties commonProps = new org.apache.stratos.common.Properties();

        if (properties != null && properties.getProperties() != null) {

            for (org.apache.stratos.cloud.controller.stub.Property property : properties.getProperties()) {
                if ((property != null) && (property.getValue() != null)) {
                    Property newProperty = new Property();
                    newProperty.setName(property.getName());
                    newProperty.setValue(property.getValue());
                    commonProps.addProperty(newProperty);
                }
            }

        }
        return commonProps;
    }

    private static String convertRepoURL(String gitURL) {
        String convertedHttpURL = null;
        if (gitURL != null && gitURL.startsWith("git@")) {
            StringBuilder httpRepoUrl = new StringBuilder();
            httpRepoUrl.append("http://");
            String[] urls = gitURL.split(":");
            String[] hostNameArray = urls[0].split("@");
            String hostName = hostNameArray[1];
            httpRepoUrl.append(hostName).append("/").append(urls[1]);
            convertedHttpURL = httpRepoUrl.toString();
        } else if (gitURL != null && gitURL.startsWith("http")) {
            convertedHttpURL = gitURL;
        }
        return convertedHttpURL;
    }
}
