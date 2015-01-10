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

package org.apache.stratos.cloud.controller.iaases.kubernetes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.domain.ClusterContext;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.PortMapping;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.domain.NameValuePair;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.kubernetes.client.model.EnvironmentVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Kubernetes IaaS utility methods.
 */
public class KubernetesIaasUtil {

    private static final Log log = LogFactory.getLog(KubernetesIaas.class);

    /**
     * Prepare and returns the list of ports defined in the given cartridge.
     * @param cartridge
     * @return
     */
    public static List<Integer> prepareCartridgePorts(Cartridge cartridge) {
        List<Integer> portList = new ArrayList<Integer>();
        for (PortMapping portMapping : cartridge.getPortMappings()) {
            portList.add(Integer.valueOf(portMapping.getPort()));
        }
        return portList;
    }

    /**
     * Prepare and returns kubernetes service id using clusterId, port protocol and port.
     * @param portMapping
     * @return
     */
    public static String prepareKubernetesServiceId(String clusterId, PortMapping portMapping) {
        return String.format("%s-%s-%s", clusterId, portMapping.getProtocol(), portMapping.getPort());
    }

    /**
     * Prepare and returns environment variables for the given member.
     * @param clusterContext
     * @param memberContext
     * @return
     */
    public static EnvironmentVariable[] prepareEnvironmentVariables(ClusterContext clusterContext,
                                                                     MemberContext memberContext) {

        String kubernetesClusterId = clusterContext.getKubernetesClusterId();
        List<EnvironmentVariable> environmentVariables = new ArrayList<EnvironmentVariable>();

        // Set dynamic payload
        List<NameValuePair> payload = memberContext.getDynamicPayload();
        if (payload != null) {
            for (NameValuePair parameter : payload) {
                addToEnvironmentVariables(environmentVariables, parameter.getName(), parameter.getValue());
            }
        }

        // Set member properties
        Properties properties = memberContext.getProperties();
        if (properties != null) {
            for (Property property : properties.getProperties()) {
                addToEnvironmentVariables(environmentVariables, property.getName(),
                        property.getValue());
            }
        }

        // Set kubernetes cluster id
        addToEnvironmentVariables(environmentVariables, StratosConstants.KUBERNETES_CLUSTER_ID,
                kubernetesClusterId);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Environment variables: [cluster-id] %s [member-id] %s [variables] %s",
                    memberContext.getClusterId(), memberContext.getMemberId(), environmentVariables.toString()));
        }

        EnvironmentVariable[] array = new EnvironmentVariable[environmentVariables.size()];
        return environmentVariables.toArray(array);
    }

    private static void addToEnvironment(List<EnvironmentVariable> envVars, String payload) {
        if (payload != null) {
            String[] entries = payload.split(",");
            for (String entry : entries) {
                String[] var = entry.split("=");
                if (var.length != 2) {
                    continue;
                }
                addToEnvironmentVariables(envVars, var[0], var[1]);
            }
        }
    }

    private static void addToEnvironmentVariables(List<EnvironmentVariable> envVars, String name, String value) {
        EnvironmentVariable var = new EnvironmentVariable();
        var.setName(name);
        var.setValue(value);
        envVars.add(var);
    }
}
