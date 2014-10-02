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
package org.apache.stratos.kubernetes.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.interfaces.KubernetesAPIClientInterface;
import org.apache.stratos.kubernetes.client.model.Pod;
import org.apache.stratos.kubernetes.client.model.PodList;
import org.apache.stratos.kubernetes.client.model.ReplicationController;
import org.apache.stratos.kubernetes.client.model.ReplicationControllerList;
import org.apache.stratos.kubernetes.client.model.Service;
import org.apache.stratos.kubernetes.client.model.ServiceList;
import org.apache.stratos.kubernetes.client.rest.RestClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class KubernetesApiClient implements KubernetesAPIClientInterface {
	
	private static final Log log = LogFactory.getLog(KubernetesApiClient.class);
	private RestClient restClient;
	
	public KubernetesApiClient(String endpointUrl) {
		restClient = new RestClient(endpointUrl);
	}

	@Override
	public Pod getPod(String podId) throws KubernetesClientException{
		try {
            HttpResponse res = restClient.doGet("pods/"+podId);
            
            handleNullResponse("Pod ["+podId+"] retrieval failed.", res);
            
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Pod ["+podId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
            String content = getHttpResponseString(res);
            
            GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			Pod pod = gson.fromJson(content, Pod.class);
			
			return pod;
		} catch (KubernetesClientException e) {
			throw e;
		} catch (Exception e) {
			String msg = "Error while retrieving Pod info with Pod ID: "+podId;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}
	
	@Override
	public Pod[] getAllPods() throws KubernetesClientException {
		
		try {
			HttpResponse res = restClient.doGet("pods");
            
			handleNullResponse("Pod retrieval failed.", res);
			
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	return new Pod[0];
            }
            
            String content = getHttpResponseString(res);
            
            GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			PodList podList = gson.fromJson(content, PodList.class);
			return podList.getItems();
			
		} catch (Exception e) {
			String msg = "Error while retrieving Pods.";
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public void createPod(Pod pod) throws KubernetesClientException {

		try {
			GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			String content = gson.toJson(pod);
			if (log.isDebugEnabled()) {
				log.debug("CreatePod Request Body : "+content);
			}
			HttpResponse res = restClient.doPost("pods", content);
			
			handleNullResponse("Pod "+pod+" creation failed.", res);
			
			if (res.getStatusLine().getStatusCode() == HttpStatus.SC_CONFLICT) {
				log.warn("Pod already created. "+pod);
				return;
			}
            
			if (res.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Pod ["+pod+"] creation failed. Error: "+	
								res.getStatusLine().getReasonPhrase();
				log.error(msg);
				throw new KubernetesClientException(msg);
			}
		} catch (KubernetesClientException e) {
			throw e;
		} catch (Exception e) {
			String msg = "Error while creating Pod: "+pod;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public void deletePod(String podId) throws KubernetesClientException {

		try {
			HttpResponse res = restClient.doDelete("pods/"+podId);
            
			handleNullResponse("Pod ["+podId+"] deletion failed.", res);
			
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Pod ["+podId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
			if (res.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Pod ["+podId+"] deletion failed. Error: "+
						res.getStatusLine().getReasonPhrase();
				log.error(msg);
				throw new KubernetesClientException(msg);
			}
		} catch (KubernetesClientException e) {
			throw e;
		} catch (Exception e) {
			String msg = "Error while retrieving Pod info of Pod ID: "+podId;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public ReplicationController getReplicationController(String controllerId)
			throws KubernetesClientException {

		try {
			HttpResponse res = restClient.doGet("replicationControllers/"+controllerId);
			
			handleNullResponse("Replication Controller ["+controllerId+"] retrieval failed.", res);
            
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Replication Controller ["+controllerId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
            String content = getHttpResponseString(res);
            
            GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			ReplicationController controller = gson.fromJson(content, ReplicationController.class);
			return controller;
		} catch (KubernetesClientException e) {
			throw e;
		} catch (Exception e) {
			String msg = "Error while retrieving Replication Controller info with ID: "+controllerId;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public ReplicationController[] getAllReplicationControllers()
			throws KubernetesClientException {
		
		try {
			HttpResponse res = restClient.doGet("replicationControllers");
            
			handleNullResponse("Replication Controller retrieval failed.", res);
			
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	return new ReplicationController[0];
            }
            
            String content = getHttpResponseString(res);
            
            GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			ReplicationControllerList podList = gson.fromJson(content, ReplicationControllerList.class);
			
			return podList.getItems();
		} catch (Exception e) {
			String msg = "Error while retrieving Replication Controllers.";
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public void createReplicationController(ReplicationController controller)
			throws KubernetesClientException {

		try {
			
			GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			String content = gson.toJson(controller);
			if (log.isDebugEnabled()) {
				log.debug("CreateReplicationController Request Body : "+content);
			}
			HttpResponse res = restClient.doPost("replicationControllers", content);
			
			handleNullResponse("Replication Controller "+controller+" creation failed.", res);
            
			if (res.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Replication Controller [" + controller
						+ "] creation failed. Error: "
						+ res.getStatusLine().getReasonPhrase();
				log.error(msg);
				throw new KubernetesClientException(msg);
			}
			
		} catch (KubernetesClientException e) {
			throw e;
		} catch (Exception e) {
			String msg = "Error while creating Replication Controller: "
					+ controller;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);

		}
	}

	@Override
	public void updateReplicationController(String controllerId, int replicas)
			throws KubernetesClientException {
		ReplicationController controller = null;
		
		// gets the current controller
		controller = getReplicationController(controllerId);
		
		try {

			// update the number of replicas
			controller.getDesiredState().setReplicas(replicas);
			
			GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			String content = gson.toJson(controller);
			if (log.isDebugEnabled()) {
				log.debug("UpdateReplicationController Request Body : "
						+ content);
			}
			HttpResponse res = restClient.doPut("replicationControllers/"+controller.getId(),
					content);
			
			handleNullResponse("Replication Controller ["+controllerId+"] update failed.", res);

			if (res.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED
					&& res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Replication Controller [" + controller
						+ "] update failed. Error: "
						+ res.getStatusLine().getReasonPhrase();
				log.error(msg);
				throw new KubernetesClientException(msg);
			}

		} catch (KubernetesClientException e) {
			throw e;
		} catch (Exception e) {
			String msg = "Error while updating Replication Controller: "
					+ controller;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);

		}
	}
	
	@Override
	public void deleteReplicationController(String controllerId)
			throws KubernetesClientException {
		
		try {
			HttpResponse res = restClient.doDelete("replicationControllers/"+controllerId);
            
			handleNullResponse("Replication Controller ["+controllerId+"] deletion failed.", res);
			
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Replication Controller ["+controllerId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
			if (res.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Replication Controller ["+controllerId+"] deletion failed. Error: "+
						res.getStatusLine().getReasonPhrase();
				log.error(msg);
				throw new KubernetesClientException(msg);
			}
		} catch (KubernetesClientException e) {
			throw e;
		} catch (Exception e) {
			String msg = "Error while retrieving Replication Controller info of Controller ID: "+controllerId;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public Service getService(String serviceId)
			throws KubernetesClientException {
		try {
			HttpResponse res = restClient.doGet("services/"+serviceId);
			
			handleNullResponse("Service ["+serviceId+"] retrieval failed.", res);
            
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Service ["+serviceId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
            String content = getHttpResponseString(res);
            
            GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			Service service = gson.fromJson(content, Service.class);
			return service;
			
		} catch (Exception e) {
			String msg = "Error while retrieving Service info with Service ID: "+serviceId;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public Service[] getAllServices() throws KubernetesClientException {
		try {
			HttpResponse res = restClient.doGet("services");
            
			handleNullResponse("Service retrieval failed.", res);
			
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	return new Service[0];
            }
            
            String content = getHttpResponseString(res);
            
            GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			ServiceList serviceList = gson.fromJson(content, ServiceList.class);
			return serviceList.getItems();
		} catch (Exception e) {
			String msg = "Error while retrieving Services.";
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public void createService(Service service) throws KubernetesClientException {

		try {
			GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			String content = gson.toJson(service);
			if (log.isDebugEnabled()) {
				log.debug("CreateService Request Body : "+content);
			}
			
			HttpResponse res = restClient.doPost("services", content);
			
			handleNullResponse("Service "+service+" creation failed.", res);
			
			if (res.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Service ["+service+"] creation failed. Error: "+
						res.getStatusLine().getReasonPhrase();
				log.error(msg);
				throw new KubernetesClientException(msg);
			}
		} catch (KubernetesClientException e) {
			throw e;
			
		} catch (Exception e) {
			String msg = "Error while creating the Service: "+service;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public void deleteService(String serviceId)
			throws KubernetesClientException {

		try {
			HttpResponse res = restClient.doDelete("services/"+serviceId);
			
			handleNullResponse("Service ["+serviceId+"] deletion failed.", res);
            
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Service ["+serviceId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
			if (res.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Service ["+serviceId+"] deletion failed. Error: "+
						res.getStatusLine().getReasonPhrase();
				log.error(msg);
				throw new KubernetesClientException(msg);
			}
		} catch (KubernetesClientException e) {
			throw e;
			
		} catch (Exception e) {
			String msg = "Error while retrieving Service info of Service ID: "+serviceId;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

    private void handleNullResponse(String message, HttpResponse res)
            throws KubernetesClientException {
        if (res == null) {
            log.error(message+ " Null response receieved.");
            throw new KubernetesClientException(message);
        }
    }
	
	// This method gives the HTTP response string
	private String getHttpResponseString(HttpResponse response) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					(response.getEntity().getContent())));

			String output;
			String result = "";

			while ((output = reader.readLine()) != null) {
				result += output;
			}

			return result;
		} catch (SocketException e) {
			log.error("Connection problem");
			return null;
		} catch (NullPointerException e) {
			log.error("Null value return from server");
			return null;
		} catch (IOException e) {
			log.error("IO error");
			return null;
		}
	}

}
