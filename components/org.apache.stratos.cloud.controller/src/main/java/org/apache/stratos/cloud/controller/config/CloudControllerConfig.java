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

package org.apache.stratos.cloud.controller.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.DataPublisherConfig;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.TopologyConfig;

import java.util.List;

/**
 * Cloud controller config parser parse the cloud-controller.xml and stores
 * the configuration in this singleton class to be used in the runtime.
 */
public class CloudControllerConfig {

    private static final Log log = LogFactory.getLog(CloudControllerConfig.class);

    private static volatile CloudControllerConfig instance;

    private List<IaasProvider> iaasProviders;
    private boolean enableBAMDataPublisher;
    private DataPublisherConfig dataPubConfig;
    private boolean enableTopologySync;
    private TopologyConfig topologyConfig;

    private CloudControllerConfig() {
    }

    public static CloudControllerConfig getInstance() {
        if (instance == null) {
            synchronized (CloudControllerConfig.class) {
                if (instance == null) {
                    instance = new CloudControllerConfig();
                }
            }
        }
        return instance;
    }

    public void setIaasProviders(List<IaasProvider> iaasProviders) {
        this.iaasProviders = iaasProviders;
    }

    public List<IaasProvider> getIaasProviders() {
        return iaasProviders;
    }

    public IaasProvider getIaasProvider(String type) {
        if(type == null) {
            return null;
        }

        for (IaasProvider iaasProvider : iaasProviders) {
            if(type.equals(iaasProvider.getType())) {
                return iaasProvider;
            }
        }
        return null;
    }

    public void setEnableBAMDataPublisher(boolean enableBAMDataPublisher) {
        this.enableBAMDataPublisher = enableBAMDataPublisher;
    }

    public boolean isBAMDataPublisherEnabled() {
        return enableBAMDataPublisher;
    }

    public void setDataPubConfig(DataPublisherConfig dataPubConfig) {
        this.dataPubConfig = dataPubConfig;
    }

    public DataPublisherConfig getDataPubConfig() {
        return dataPubConfig;
    }

    public void setEnableTopologySync(boolean enableTopologySync) {
        this.enableTopologySync = enableTopologySync;
    }

    public boolean isTopologySyncEnabled() {
        return enableTopologySync;
    }

    public void setTopologyConfig(TopologyConfig topologyConfig) {
        this.topologyConfig = topologyConfig;
    }

    public TopologyConfig getTopologyConfig() {
        return topologyConfig;
    }
}
