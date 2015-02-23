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
import org.apache.stratos.cloud.controller.domain.ClusterContext;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.domain.PortMapping;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.domain.NameValuePair;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.kubernetes.client.model.EnvironmentVariable;
import org.apache.stratos.kubernetes.client.model.Port;

import java.util.ArrayList;
import java.util.List;

/**
 * Kubernetes IaaS utility methods.
 */
public class KubernetesIaasUtil {

    private static final Log log = LogFactory.getLog(KubernetesIaas.class);

    /**
     * Generate kubernetes service id using clusterId, port protocol and port.
     * @param portMapping
     * @return
     */
    public static String generateKubernetesServiceId(String clusterId, PortMapping portMapping) {
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

        EnvironmentVariable[] array = new EnvironmentVariable[environmentVariables.size()];
        return environmentVariables.toArray(array);
    }

    /**
     * Add name value pair to kubernetes environment variables.
     * @param environmentVariables
     * @param name
     * @param value
     */
    private static void addToEnvironmentVariables(List<EnvironmentVariable> environmentVariables, String name, String value) {
        EnvironmentVariable environmentVariable = new EnvironmentVariable();
        environmentVariable.setName(name);
        environmentVariable.setValue(value);
        environmentVariables.add(environmentVariable);
    }

    /**
     * Convert stratos port mappings to kubernetes ports
     * @param portMappings
     * @return
     */
    public static List<Port> convertPortMappings(List<PortMapping> portMappings) {
        List<Port> ports = new ArrayList<Port>();
        for(PortMapping portMapping : portMappings) {
            Port port = new Port();
            port.setName(preparePortNameFromPortMapping(portMapping));
            port.setContainerPort(portMapping.getPort());
            port.setHostPort(portMapping.getKubernetesServicePort());
            ports.add(port);
        }
        return ports;
    }

    /**
     * Prepare port name for port mapping.
     * @param portMapping
     * @return
     */
    public static String preparePortNameFromPortMapping(PortMapping portMapping) {
        return String.format("%s-%d", portMapping.getProtocol(), portMapping.getPort());
    }

    /**
     * Replace dot and underscore with dash character.
     * @param id
     * @return
     */
    public static String fixSpecialCharacters(String id) {
        id = id.replace(".", "-");
        id = id.replace("_", "-");
        return id;
    }
}
