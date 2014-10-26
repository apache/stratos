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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.ClusterContext;
import org.apache.stratos.cloud.controller.pojo.ContainerClusterContext;
import org.apache.stratos.cloud.controller.pojo.KubernetesClusterContext;
import org.apache.stratos.cloud.controller.pojo.PortMapping;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.kubernetes.client.model.Selector;
import org.apache.stratos.kubernetes.client.model.Service;

import com.google.common.base.Function;

/**
 * Is responsible for converting a {@link ContainerClusterContext} object to a list of Kubernetes
 * {@link Service} Objects.
 */
public class ContainerClusterContextToKubernetesService implements Function<ContainerClusterContext, List<Service>> {

    private static final Log LOG = LogFactory.getLog(ContainerClusterContextToKubernetesService.class);
    private FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();

    @Override
    public List<Service> apply(ContainerClusterContext context) {

        List<Service> services = new ArrayList<Service>();
        String clusterId = context.getClusterId();
        ClusterContext clusterContext = dataHolder.getClusterContext(clusterId);
        
        Cartridge cartridge = dataHolder.getCartridge(clusterContext.getCartridgeType());

        if (cartridge == null) {
            LOG.error("Cannot find a Cartridge of type : " + clusterContext.getCartridgeType());
            return services;
        }

        String kubernetesClusterId = CloudControllerUtil.getProperty(
                clusterContext.getProperties(), StratosConstants.KUBERNETES_CLUSTER_ID);
        KubernetesClusterContext kubClusterContext = dataHolder
                .getKubernetesClusterContext(kubernetesClusterId);

        // For each Cartridge port, we generate a Kubernetes service proxy
        for (PortMapping portMapping : cartridge.getPortMappings()) {
            // gets the container port
            String containerPort = portMapping.getPort();
            
            // build the Service
            Service service = new Service();
            service.setApiVersion("v1beta1");
            // id of the service generated using "clusterId-containerPort"
            service.setId(CloudControllerUtil.getCompatibleId(clusterId+"-"+containerPort));
            service.setKind("Service");
            service.setContainerPort(containerPort);
            int hostPort = kubClusterContext.getAnAvailableHostPort();
            service.setPort(hostPort);
            Selector selector = new Selector();
            selector.setName(clusterId);
            service.setSelector(selector);
            
            services.add(service);
        }

        return services;
    }

}
