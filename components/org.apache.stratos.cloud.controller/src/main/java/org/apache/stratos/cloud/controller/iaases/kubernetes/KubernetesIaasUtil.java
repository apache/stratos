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

import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.ClusterContext;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.PortMapping;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.domain.NameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Kubernetes IaaS utility methods.
 */
public class KubernetesIaasUtil {

    private static final Log log = LogFactory.getLog(KubernetesIaas.class);

    /**
     * Generate kubernetes service id using clusterId, port protocol and port.
     *
     * @param portMapping
     * @return
     */
    public static String generateKubernetesServiceId(String clusterId, PortMapping portMapping) {
        return String.format("%s-%s-%s", clusterId, portMapping.getProtocol(), portMapping.getPort());
    }

    /**
     * Prepare and returns environment variables for the given member.
     *
     * @param clusterContext
     * @param memberContext
     * @return
     */
    public static List<EnvVar> prepareEnvironmentVariables(ClusterContext clusterContext, MemberContext memberContext) {

        String kubernetesClusterId = clusterContext.getKubernetesClusterId();
        List<EnvVar> environmentVariables = new ArrayList<EnvVar>();

        // Set dynamic payload
        NameValuePair[] payload = memberContext.getDynamicPayload();
        if (payload != null && payload.length != 0) {
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

        return environmentVariables;
    }

    /**
     * Add name value pair to kubernetes environment variables.
     *
     * @param environmentVariables
     * @param name
     * @param value
     */
    private static void addToEnvironmentVariables(List<EnvVar> environmentVariables, String name, String value) {
        EnvVar environmentVariable = new EnvVar();
        environmentVariable.setName(name);
        environmentVariable.setValue(value);
        environmentVariables.add(environmentVariable);
    }

    /**
     * Convert stratos port mappings to kubernetes container ports
     *
     * @param portMappings
     * @return
     */
    public static List<ContainerPort> convertPortMappings(List<PortMapping> portMappings) {
        List<ContainerPort> ports = new ArrayList<ContainerPort>();
        for (PortMapping portMapping : portMappings) {
            ContainerPort containerPort = new ContainerPort();
            containerPort.setName(preparePortNameFromPortMapping(portMapping));
            containerPort.setContainerPort(portMapping.getPort());
            ports.add(containerPort);
        }
        return ports;
    }

    /**
     * Prepare port name for port mapping.
     *
     * @param portMapping
     * @return
     */
    public static String preparePortNameFromPortMapping(PortMapping portMapping) {
        return String.format("%s-%d", portMapping.getProtocol(), portMapping.getPort());
    }

    /**
     * Replace dot and underscore with dash character.
     *
     * @param id
     * @return
     */
    public static String fixSpecialCharacters(String id) {
        id = id.replace(".", "-");
        id = id.replace("_", "-");
        return id;
    }
}
