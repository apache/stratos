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
import org.apache.stratos.cloud.controller.pojo.PortMapping;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.kubernetes.client.model.Container;
import org.apache.stratos.kubernetes.client.model.EnvironmentVariable;
import org.apache.stratos.kubernetes.client.model.Port;

import com.google.common.base.Function;

/**
 * Is responsible for converting a {@link ContainerClusterContext} object to a Kubernetes
 * {@link Container} Object.
 */
public class ContainerClusterContextToKubernetesContainer implements Function<ContainerClusterContext, Container> {

    private static final Log log = LogFactory.getLog(ContainerClusterContextToKubernetesContainer.class);
    private FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();

    @Override
    public Container apply(ContainerClusterContext memberContext) {
        String clusterId = memberContext.getClusterId();
        ClusterContext clusterContext = dataHolder.getClusterContext(clusterId);

        Container container = new Container();
        container.setName(getCompatibleName(clusterContext.getHostName()));

        Cartridge cartridge = dataHolder.getCartridge(clusterContext.getCartridgeType());

        if (cartridge == null) {
            log.error("Cannot find a Cartridge of type : " + clusterContext.getCartridgeType());
            return null;
        }

        container.setImage(cartridge.getContainer().getImageName());

        container.setPorts(getPorts(clusterContext, cartridge));

        container.setEnv(getEnvironmentVars(memberContext, clusterContext));

        return container;
    }

    private String getCompatibleName(String hostName) {
        if (hostName.indexOf('.') != -1) {
            hostName = hostName.replace('.', '-');
        }
        return hostName;
    }

    private Port[] getPorts(ClusterContext ctxt, Cartridge cartridge) {
        Port[] ports = new Port[cartridge.getPortMappings().size()];
        List<Port> portList = new ArrayList<Port>();

        for (PortMapping portMapping : cartridge.getPortMappings()) {
            Port p = new Port();
            p.setContainerPort(Integer.parseInt(portMapping.getPort()));
            // In kubernetes transport protocol always be 'tcp'
            p.setProtocol("tcp");
            p.setName(p.getProtocol() + p.getContainerPort());
            portList.add(p);
        }

        return portList.toArray(ports);
    }

    private EnvironmentVariable[] getEnvironmentVars(ContainerClusterContext memberCtxt, ClusterContext ctxt) {

        String kubernetesClusterId = CloudControllerUtil.getProperty(ctxt.getProperties(),
                StratosConstants.KUBERNETES_CLUSTER_ID);

        List<EnvironmentVariable> envVars = new ArrayList<EnvironmentVariable>();
        addToEnvironment(envVars, ctxt.getPayload());
        addToEnvironment(envVars, StratosConstants.KUBERNETES_CLUSTER_ID, kubernetesClusterId);
        if (memberCtxt.getProperties() != null) {
            Properties props1 = memberCtxt.getProperties();
            if (props1 != null) {
                for (Property prop : props1.getProperties()) {
                    addToEnvironment(envVars, prop.getName(), prop.getValue());
                }
            }
        }

        EnvironmentVariable[] vars = new EnvironmentVariable[envVars.size()];

        return envVars.toArray(vars);

    }

    private void addToEnvironment(List<EnvironmentVariable> envVars, String payload) {

        if (payload != null) {
            String[] entries = payload.split(",");
            for (String entry : entries) {
                String[] var = entry.split("=");
                if (var.length != 2) {
                    continue;
                }
                addToEnvironment(envVars, var[0], var[1]);
            }
        }
    }

    private void addToEnvironment(List<EnvironmentVariable> envVars, String name, String value) {

        EnvironmentVariable var = new EnvironmentVariable();
        var.setName(name);
        var.setValue(value);
        envVars.add(var);
    }

}
