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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.interfaces.KubernetesAPIClientInterface;
import org.apache.stratos.kubernetes.client.model.*;
import org.apache.stratos.kubernetes.client.rest.KubernetesResponse;
import org.apache.stratos.kubernetes.client.rest.RestClient;

import java.net.URI;

public class KubernetesApiClient implements KubernetesAPIClientInterface {
	
	private static final Log log = LogFactory.getLog(KubernetesApiClient.class);
	private RestClient restClient;
	private String baseURL;
	
	public KubernetesApiClient(String endpointUrl) {
		restClient = new RestClient();
		baseURL = endpointUrl;
	}

	@Override
	public Pod getPod(String podId) throws KubernetesClientException{
		try {
		    URI uri = new URIBuilder(baseURL+"pods/"+podId).build();
            KubernetesResponse res = restClient.doGet(uri);
            
            handleNullResponse("Pod ["+podId+"] retrieval failed.", res);
            
            if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Pod ["+podId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
            String content = res.getContent();
            
            GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			return gson.fromJson(content, Pod.class);
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
		    URI uri = new URIBuilder(baseURL+"pods").build();
			KubernetesResponse res = restClient.doGet(uri);
            
			handleNullResponse("Pod retrieval failed.", res);
			
            if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	return new Pod[0];
            }
            
            String content = res.getContent();
            
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
			URI uri = new URIBuilder(baseURL+"pods").build();
			KubernetesResponse res = restClient.doPost(uri, content);
			
			handleNullResponse("Pod "+pod+" creation failed.", res);
			
			if (res.getStatusCode() == HttpStatus.SC_CONFLICT) {
				log.warn("Pod already created. "+pod);
				return;
			}
            
			if (res.getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Pod ["+pod+"] creation failed. Error: "+	
								res.getReason();
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
		    URI uri = new URIBuilder(baseURL+"pods/"+podId).build();
			KubernetesResponse res = restClient.doDelete(uri);
            
			handleNullResponse("Pod ["+podId+"] deletion failed.", res);
			
            if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Pod ["+podId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
			if (res.getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Pod ["+podId+"] deletion failed. Error: "+
						res.getReason();
				log.error(msg);
				throw new KubernetesClientException(msg);
			}
		} catch (KubernetesClientException e) {
			throw e;
		} catch (Exception e) {
			String msg = "Error while deleting Pod with ID: "+podId;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public ReplicationController getReplicationController(String controllerId)
			throws KubernetesClientException {

		try {
		    URI uri = new URIBuilder(baseURL+"replicationControllers/"+controllerId).build();
			KubernetesResponse res = restClient.doGet(uri);
			
			handleNullResponse("Replication Controller ["+controllerId+"] retrieval failed.", res);
            
            if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Replication Controller ["+controllerId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
            String content = res.getContent();
            
            GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			return gson.fromJson(content, ReplicationController.class);
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
		    URI uri = new URIBuilder(baseURL+"replicationControllers").build();
			KubernetesResponse res = restClient.doGet(uri);
            
			handleNullResponse("Replication Controller retrieval failed.", res);
			
            if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	return new ReplicationController[0];
            }
            
            String content = res.getContent();
            
            GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			ReplicationControllerList controllerList = gson.fromJson(content, ReplicationControllerList.class);
			
			return controllerList.getItems();
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
			
			URI uri = new URIBuilder(baseURL+"replicationControllers").build();
			KubernetesResponse res = restClient.doPost(uri, content);
			
			handleNullResponse("Replication Controller "+controller+" creation failed.", res);
			
			if (res.getStatusCode() == HttpStatus.SC_CONFLICT) {
                log.warn("Replication Controller already created. "+controller);
                return;
            }
            
			if (res.getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Replication Controller [" + controller
						+ "] creation failed. Error: "
						+ res.getReason();
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

		// gets the current controller
        ReplicationController controller = getReplicationController(controllerId);
		
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
			
			URI uri = new URIBuilder(baseURL+"replicationControllers/"+controllerId).build();
			KubernetesResponse res = restClient.doPut(uri, content);
			
			handleNullResponse("Replication Controller ["+controllerId+"] update failed.", res);

			if (res.getStatusCode() != HttpStatus.SC_ACCEPTED
					&& res.getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Replication Controller [" + controller
						+ "] update failed. Error: "
						+ res.getReason();
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
		    URI uri = new URIBuilder(baseURL+"replicationControllers/"+controllerId).build();
			KubernetesResponse res = restClient.doDelete(uri);
            
			handleNullResponse("Replication Controller ["+controllerId+"] deletion failed.", res);
			
            if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Replication Controller ["+controllerId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
			if (res.getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Replication Controller ["+controllerId+"] deletion failed. Error: "+
						res.getReason();
				log.error(msg);
				throw new KubernetesClientException(msg);
			}
		} catch (KubernetesClientException e) {
			throw e;
		} catch (Exception e) {
			String msg = "Error while deleting Replication Controller with Controller ID: "+controllerId;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public Service getService(String serviceId)
			throws KubernetesClientException {
		try {
		    URI uri = new URIBuilder(baseURL+"services/"+serviceId).build();
			KubernetesResponse res = restClient.doGet(uri);
			
			handleNullResponse("Service ["+serviceId+"] retrieval failed.", res);
            
            if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Service ["+serviceId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
            String content = res.getContent();
            
            GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			return gson.fromJson(content, Service.class);
		} catch (KubernetesClientException e) {
            throw e;
        } catch (Exception e) {
			String msg = "Error while retrieving Service info with Service ID: "+serviceId;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

	@Override
	public Service[] getAllServices() throws KubernetesClientException {
		try {
		    
		    URI uri = new URIBuilder(baseURL+"services").build();
			KubernetesResponse res = restClient.doGet(uri);
            
			handleNullResponse("Service retrieval failed.", res);
			
            if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	return new Service[0];
            }
            
            String content = res.getContent();
            
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
			
			URI uri = new URIBuilder(baseURL+"services").build();
			KubernetesResponse res = restClient.doPost(uri, content);
			
			handleNullResponse("Service "+service+" creation failed.", res);
			
			if (res.getStatusCode() == HttpStatus.SC_CONFLICT) {
                log.warn("Service already created. "+service);
                return;
            }
			
			if (res.getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Service ["+service+"] creation failed. Error: "+
						res.getReason();
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
		    URI uri = new URIBuilder(baseURL+"services/"+serviceId).build();
			KubernetesResponse res = restClient.doDelete(uri);
			
			handleNullResponse("Service ["+serviceId+"] deletion failed.", res);
            
            if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            	String msg = "Service ["+serviceId+"] doesn't exist.";
				log.error(msg);
				throw new KubernetesClientException(msg);
            }
            
			if (res.getStatusCode() != HttpStatus.SC_ACCEPTED && 
					res.getStatusCode() != HttpStatus.SC_OK) {
				String msg = "Service ["+serviceId+"] deletion failed. Error: "+
						res.getReason();
				log.error(msg);
				throw new KubernetesClientException(msg);
			}
		} catch (KubernetesClientException e) {
			throw e;
			
		} catch (Exception e) {
			String msg = "Error while deleting Service with Service ID: "+serviceId;
			log.error(msg, e);
			throw new KubernetesClientException(msg, e);
		}
	}

    @Override
    public Pod[] getSelectedPods(Label[] label) throws KubernetesClientException {
        
        try {
            String labelQuery = getLabelQuery(label);
            URI uri = new URIBuilder(baseURL + "pods").addParameter("labels", labelQuery).build();
            KubernetesResponse res = restClient.doGet(uri);
            
            handleNullResponse("Pod retrieval failed.", res);
            
            if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return new Pod[0];
            }
            
            String content = res.getContent();
            
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            PodList podList = gson.fromJson(content, PodList.class);
            if (podList == null || podList.getItems() == null) {
                return new Pod[0];
            }
            return podList.getItems();
            
        } catch (Exception e) {
            String msg = "Error while retrieving Pods.";
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    private String getLabelQuery(Label[] label) {
        String query = "";
        for (Label l : label) {
            query = query.concat("name="+l.getName()+",");
        }
        return query.endsWith(",") ? query.substring(0, query.length()-1) : query;
    }

    private void handleNullResponse(String message, KubernetesResponse res)
            throws KubernetesClientException {
        if (res == null) {
            log.error(message+ " Null response received.");
            throw new KubernetesClientException(message);
        }
    }
	
}
