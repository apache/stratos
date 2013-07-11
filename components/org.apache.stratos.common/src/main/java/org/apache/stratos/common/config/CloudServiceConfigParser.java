/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.common.config;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.util.CommonUtil;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.FileInputStream;

public class CloudServiceConfigParser {

    private static Log log = LogFactory.getLog(CloudServiceConfigParser.class);

    private static class SynchronizingClass {
    }

    private static final SynchronizingClass loadlock = new SynchronizingClass();

    private static CloudServicesDescConfig cloudServicesDescConfig = null;

    private static final String CONFIG_FILENAME = "cloud-services-desc.xml";

    public static CloudServicesDescConfig loadCloudServicesConfiguration() throws Exception {
        if (cloudServicesDescConfig != null) {
            return cloudServicesDescConfig;
        }

        synchronized (loadlock) {
            if (cloudServicesDescConfig == null) {
                try {
                    String configFileName = CarbonUtils.getCarbonConfigDirPath() + File.separator + 
                            StratosConstants.MULTITENANCY_CONFIG_FOLDER + File.separator + CONFIG_FILENAME;
                    OMElement configElement = CommonUtil.buildOMElement(new FileInputStream(configFileName));
                    cloudServicesDescConfig = new CloudServicesDescConfig(configElement);
                } catch (Exception e) {
                    String msg = "Error in building the cloud service configuration.";
                    log.error(msg, e);
                    throw new Exception(msg, e);
                }
            }
        }
        return cloudServicesDescConfig;
    }

}
