/*
 *
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
 *
 */
package org.apache.stratos.kubernetes.client.interfaces;

import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.model.*;

import java.util.List;


public interface KubernetesAPIClientInterface {

	/**
	 * Create pod.
	 * @param podId
	 * @param podLabel
	 * @param dockerImage
	 * @param ports
	 * @throws KubernetesClientException
	 */
	public void createPod(String podId, String podLabel, String dockerImage, List<Port> ports,
                          EnvironmentVariable[] environmentVariables)
			throws KubernetesClientException;

	/**
	 * Get information of a Pod given the PodID
	 * @param podId id of the pod
	 * @return {@link Pod}
	 * @throws KubernetesClientException
	 */
	public Pod getPod(String podId) throws KubernetesClientException;

	/**
	 * Get all Pods
	 * @return Pods
	 * @throws KubernetesClientException
	 */
	public List<Pod> getPods() throws KubernetesClientException;

	/**
	 * Run a label query and retrieve a sub set of Pods.
	 * @param labels of labels for the label query
	 * @return Pods selected Pods by executing the label query.
	 * @throws KubernetesClientException
	 */
	public List<Pod> queryPods(Labels[] labels) throws KubernetesClientException;

	/**
	 * Delete a Pod
	 * @param podId Id of the Pod to be deleted
	 * @throws KubernetesClientException
	 */
	public void deletePod(String podId) throws KubernetesClientException;

	/**
	 * Create replication controller.
	 * @param replicationControllerId
	 * @param replicationControllerName
	 * @param dockerImage
	 * @param ports
	 * @param replicas
	 * @throws KubernetesClientException
	 */
	public void createReplicationController(String replicationControllerId,
                                            String replicationControllerName,
                                            String dockerImage,
                                            List<Port> ports,
                                            EnvironmentVariable[] environmentVariables,
                                            int replicas) throws KubernetesClientException;

	/**
	 * Get a Replication Controller Info
	 * @param controllerId id of the Replication Controller
	 * @return {@link ReplicationController}
	 * @throws KubernetesClientException
	 */
	public ReplicationController getReplicationController(String controllerId) throws KubernetesClientException;
	
	/**
	 * Get all Replication Controllers.
	 * @return {@link ReplicationController}s
	 * @throws KubernetesClientException
	 */
	public List<ReplicationController> getReplicationControllers() throws KubernetesClientException;

	/**
	 * Update a Replication Controller (update the number of replicas).
	 * @param replicationController replication controller to be updated
	 * @throws KubernetesClientException
	 */
	public void updateReplicationController(ReplicationController replicationController) throws KubernetesClientException;
	
	/**
	 * Delete a Replication Controller.
	 * @param replicationControllerId controller id controller id to be deleted.
	 * @throws KubernetesClientException
	 */
	public void deleteReplicationController(String replicationControllerId) throws KubernetesClientException;

	/**
	 * Create service.
	 * @param serviceId
	 * @param serviceLabel
	 * @param servicePort
	 * @param containerPortName
	 * @param publicIPs
     * @throws KubernetesClientException
	 */
	public void createService(String serviceId, String serviceLabel, int servicePort,
                              String containerPortName, String[] publicIPs) throws KubernetesClientException;

	/**
	 * Get the Service with the given id.
	 * @param serviceId id of the service.
	 * @return {@link Service}
	 * @throws KubernetesClientException
	 */
	public Service getService(String serviceId) throws KubernetesClientException;
	
	/**
	 * Get services.
	 * @return array of {@link Service}s
	 * @throws KubernetesClientException
	 */
	public List<Service> getServices() throws KubernetesClientException;
	
	/**
	 * Delete a service.
	 * @param serviceId service id to be deleted.
 	 * @throws KubernetesClientException
	 */
	public void deleteService(String serviceId) throws KubernetesClientException;
}
