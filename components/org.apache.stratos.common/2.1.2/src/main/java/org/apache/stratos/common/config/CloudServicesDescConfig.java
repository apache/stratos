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

import javax.xml.namespace.QName;
import java.util.*;

public class CloudServicesDescConfig {
    private static final Log log = LogFactory.getLog(CloudServicesDescConfig.class);
    private static final String CONFIG_NS = "http://wso2.com/carbon/cloud/mgt/services";
    private static final String CLOUD_SERVICE_ELEMENT_NAME = "cloudService";

    Map<String, CloudServiceConfig> cloudServiceConfigs;

    public CloudServicesDescConfig(OMElement configEle) {
        // as the cloud service configs are kept in an order, we use an ordered map.
        cloudServiceConfigs = new LinkedHashMap<String, CloudServiceConfig>();
        serialize(configEle);
    }

    public void serialize(OMElement configEle) {
        Iterator configChildIt = configEle.getChildElements();
        while (configChildIt.hasNext()) {
            Object configChildObj = configChildIt.next();
            if (!( configChildObj instanceof OMElement)) {
                continue;
            }
            OMElement configChildEle = (OMElement)configChildObj;
            if (new QName(CONFIG_NS, CLOUD_SERVICE_ELEMENT_NAME, "").
                    equals(configChildEle.getQName())) {
                CloudServiceConfig cloudServiceConfig = new CloudServiceConfig(configChildEle);
                String name = cloudServiceConfig.getName();
                cloudServiceConfigs.put(name, cloudServiceConfig);
            }
        }
    }

    public Map<String, CloudServiceConfig> getCloudServiceConfigs() {
        return cloudServiceConfigs;
    }
}
