/*
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
 */

package org.apache.stratos.kubernetes.client;

/**
 * Kubernetes constants.
 */
public class KubernetesConstants {

    public static final String POD_STATUS_RUNNING = "Running";
    public static final String POLICY_PULL_IF_NOT_PRESENT = "IfNotPresent";
    public static final String POLICY_PULL_ALWAYS = "Always";
    public static final String POLICY_PULL_NEVER = "Never";
    public static final String SESSION_AFFINITY_CLIENT_IP = "ClientIP";
    public static final String KIND_POD = "Pod";
    public static final String KIND_SERVICE = "Service";
    public static final String SERVICE_SELECTOR_LABEL = "serviceSelector";
    public static final String RESOURCE_CPU = "cpu";
    public static final String RESOURCE_MEMORY = "memory";
    public static final String NODE_PORT = "NodePort";
    public static final String CLUSTER_IP = "ClusterIP";
    public static final int MAX_LABEL_LENGTH = 63;
    public static final String SECRET_TYPE_DOCKERCFG = "kubernetes.io/dockercfg";
    public static final String DEFAULT_NAMESPACE = "default";
    public static final String PAYLOAD_PARAMETER_PREFIX = "payload_parameter.";
    public static final String PORT_MAPPINGS = "PORT_MAPPINGS";
    public static final String KUBERNETES_CONTAINER_CPU = "KUBERNETES_CONTAINER_CPU";
    public static final String KUBERNETES_CONTAINER_MEMORY = "KUBERNETES_CONTAINER_MEMORY";
    public static final String KUBERNETES_SERVICE_SESSION_AFFINITY = "KUBERNETES_SERVICE_SESSION_AFFINITY";
    public static final String KUBERNETES_CONTAINER_CPU_DEFAULT = "kubernetes.container.cpu.default";
    public static final String KUBERNETES_CONTAINER_MEMORY_DEFAULT = "kubernetes.container.memory.default";
    public static final String POD_ID_PREFIX = "pod";
    public static final String SERVICE_NAME_PREFIX = "service";
    public static final String IMAGE_PULL_SECRETS = "IMAGE_PULL_SECRETS";
    public static final String IMAGE_PULL_POLICY = "IMAGE_PULL_POLICY";
    public static final long DEFAULT_POD_ACTIVATION_TIMEOUT = 60000; // 1 min
    public static final String PAYLOAD_PARAMETER_SEPARATOR = ",";
    public static final String KUBERNETES_NAMESPACE = "KUBERNETES_NAMESPACE";
    public static final String PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR = "=";
}