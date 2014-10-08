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
package org.apache.stratos.cloud.controller.functions;

import org.apache.stratos.cloud.controller.pojo.ClusterContext;
import org.apache.stratos.cloud.controller.pojo.ContainerClusterContext;
import org.apache.stratos.cloud.controller.pojo.KubernetesClusterContext;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.kubernetes.client.model.Selector;
import org.apache.stratos.kubernetes.client.model.Service;

import com.google.common.base.Function;

/**
 * Is responsible for converting a {@link ContainerClusterContext} object to a Kubernetes
 * {@link Service} Object.
 */
public class ContainerClusterContextToKubernetesService implements Function<ContainerClusterContext, Service> {

    private FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();

    @Override
    public Service apply(ContainerClusterContext memberContext) {

        String clusterId = memberContext.getClusterId();
        ClusterContext clusterContext = dataHolder.getClusterContext(clusterId);

        String kubernetesClusterId = CloudControllerUtil.getProperty(
                clusterContext.getProperties(), StratosConstants.KUBERNETES_CLUSTER_ID);
        KubernetesClusterContext kubClusterContext = dataHolder
                .getKubernetesClusterContext(kubernetesClusterId);

        Service service = new Service();
        service.setApiVersion("v1beta1");
        service.setId(CloudControllerUtil.getCompatibleId(clusterId));
        service.setKind("Service");
        int hostPort = kubClusterContext.getAnAvailableHostPort();
        service.setPort(hostPort);
        Selector selector = new Selector();
        selector.setName(clusterId);
        service.setSelector(selector);

        return service;
    }

}
