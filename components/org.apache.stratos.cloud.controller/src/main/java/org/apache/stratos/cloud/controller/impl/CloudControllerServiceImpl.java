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
package org.apache.stratos.cloud.controller.impl;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.exception.UnregisteredCartridgeException;
import org.apache.stratos.cloud.controller.exception.UnregisteredServiceException;
import org.apache.stratos.cloud.controller.interfaces.CloudControllerService;
import org.apache.stratos.cloud.controller.persist.Deserializer;
import org.apache.stratos.cloud.controller.publisher.CartridgeInstanceDataPublisherTask;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.topic.TopologySynchronizerTask;
import org.apache.stratos.cloud.controller.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.topology.TopologyEventMessageDelegator;
import org.apache.stratos.cloud.controller.util.*;
import org.apache.stratos.cloud.controller.util.Properties;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadata.Status;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.internal.NodeMetadataImpl;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskInfo.TriggerInfo;
import org.wso2.carbon.ntask.core.TaskManager;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Cloud Controller Service is responsible for starting up new server instances,
 * terminating already started instances, providing pending instance count etc.
 * 
 */
public class CloudControllerServiceImpl implements CloudControllerService {

	private static final Log log = LogFactory
			.getLog(CloudControllerServiceImpl.class);
	private FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder
			.getInstance();

	public CloudControllerServiceImpl() {

		// acquire serialized data from registry
		acquireData();

		// gets the task service
		TaskService taskService = ServiceReferenceHolder
				.getInstance().getTaskService();

		if (dataHolder.getEnableBAMDataPublisher()) {

			// register and schedule, BAM data publisher task
			registerAndScheduleDataPublisherTask(taskService);
		}

		if (dataHolder.getEnableTopologySync()) {

			// start the topology builder thread
			startTopologyBuilder();

			// register and schedule, topology synchronizer task
			registerAndScheduleTopologySyncerTask(taskService);
		}
	}

	private void registerAndScheduleTopologySyncerTask(TaskService taskService) {
		TaskInfo taskInfo;
		TaskManager tm = null;
		try {

			if (!taskService.getRegisteredTaskTypes().contains(
					CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE)) {

				// topology sync
				taskService
						.registerTaskType(CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE);

				tm = taskService
						.getTaskManager(CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE);
				
				String cron = dataHolder.getTopologyConfig().getProperty(CloudControllerConstants.CRON_ELEMENT);

				cron = ( cron == null ? CloudControllerConstants.PUB_CRON_EXPRESSION : cron ); 
				
				TriggerInfo triggerInfo = new TriggerInfo(cron);
				taskInfo = new TaskInfo(
						CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME,
						TopologySynchronizerTask.class.getName(),
						new HashMap<String, String>(), triggerInfo);
				tm.registerTask(taskInfo);
			}

		} catch (Exception e) {
			String msg = "Error scheduling task: "
					+ CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME;
			log.error(msg, e);
			if (tm != null) {
				try {
					tm.deleteTask(CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME);
				} catch (TaskException e1) {
					log.error(e1);
				}
			}
			throw new CloudControllerException(msg, e);
		}
	}

	private void startTopologyBuilder() {
		// initialize TopologyEventMessageProcessor Consumer
		Thread topologyBuilder = new Thread(new TopologyEventMessageDelegator());
		// start consumer
		topologyBuilder.start();
	}

	private TaskManager registerAndScheduleDataPublisherTask(
			TaskService taskService) {
		TaskInfo taskInfo;
		TaskManager tm = null;
		// initialize and schedule the data publisher task
		try {

			if (!taskService.getRegisteredTaskTypes().contains(
					CloudControllerConstants.DATA_PUB_TASK_TYPE)) {

				taskService
						.registerTaskType(CloudControllerConstants.DATA_PUB_TASK_TYPE);

				tm = taskService
						.getTaskManager(CloudControllerConstants.DATA_PUB_TASK_TYPE);

				if (!tm.isTaskScheduled(CloudControllerConstants.DATA_PUB_TASK_NAME)) {

					TriggerInfo triggerInfo = new TriggerInfo(
							dataHolder
									.getDataPublisherCron());
					taskInfo = new TaskInfo(
							CloudControllerConstants.DATA_PUB_TASK_NAME,
							CartridgeInstanceDataPublisherTask.class.getName(),
							new HashMap<String, String>(), triggerInfo);
					tm.registerTask(taskInfo);

					// Following code is currently not required, due to an issue
					// in TS API.
					// tm.scheduleTask(taskInfo.getName());
				}
			}

		} catch (Exception e) {
			String msg = "Error scheduling task: "
					+ CloudControllerConstants.DATA_PUB_TASK_NAME;
			log.error(msg, e);
			if (tm != null) {
				try {
					tm.deleteTask(CloudControllerConstants.DATA_PUB_TASK_NAME);
				} catch (TaskException e1) {
					log.error(e1);
				}
			}
			throw new CloudControllerException(msg, e);
		}
		return tm;
	}

	private void acquireData() {

		Object obj = RegistryManager.getInstance().retrieve();
		if (obj != null) {
			try {
				Object dataObj = Deserializer
						.deserializeFromByteArray((byte[]) obj);
				if (dataObj instanceof FasterLookUpDataHolder) {
					FasterLookUpDataHolder serializedObj = (FasterLookUpDataHolder) dataObj;
					FasterLookUpDataHolder currentData = FasterLookUpDataHolder
							.getInstance();

					// assign necessary data
					currentData.setNodeIdToServiceContextMap(serializedObj
							.getNodeIdToServiceContextMap());

					// traverse through current Service Contexts
					for (ServiceContext ctxt : currentData.getServiceCtxtList()) {
						// traverse through serialized Service Contexts
						for (ServiceContext serializedCtxt : serializedObj
								.getServiceCtxtList()) {
							// if a matching Service Context found
							if (ctxt.equals(serializedCtxt)) {
								// persisted node ids of this Service Context
								List<Object> nodeIds = serializedObj
										.getNodeIdsOfServiceCtxt(serializedCtxt);
								for (Object nodeIdObj : nodeIds) {
									String nodeId = (String) nodeIdObj;

									// assign persisted data
									currentData.addNodeId(nodeId, ctxt);

								}

								ctxt.setIaasContextMap(serializedCtxt
										.getIaasCtxts());
								appendToPublicIpProperty(
										serializedCtxt
												.getProperty(CloudControllerConstants.PUBLIC_IP_PROPERTY),
										ctxt);

								// assign lastly used IaaS
								if (serializedCtxt.getCartridge() != null
										&& serializedCtxt.getCartridge()
												.getLastlyUsedIaas() != null) {

									if (ctxt.getCartridge() == null) {
										// load Cartridge
										ctxt.setCartridge(loadCartridge(
												ctxt.getCartridgeType(),
												serializedObj.getCartridges()));
									}

									IaasProvider serializedIaas = serializedCtxt
											.getCartridge().getLastlyUsedIaas();
									ctxt.getCartridge().setLastlyUsedIaas(
											serializedIaas);

								}
							}
						}
					}

					log.debug("Data is retrieved from registry.");
				} else {
					log.debug("No data is persisted in registry.");
				}
			} catch (Exception e) {

				String msg = "Unable to acquire data from Registry. Hence, any historical data will not get reflected.";
				log.warn(msg, e);
			}

		}
	}

    @Override
	public String startInstance(String clusterId, LocationScope locationScope) {

		ComputeService computeService;
		Template template;
		String ip;
        String cloud = locationScope.getCloud();
        String region = locationScope.getRegion();
		final Lock lock = new ReentrantLock();


		log.info("Starting new instance of domain : " + clusterId);

		// get the subjected ServiceContext
		ServiceContext serviceCtxt = dataHolder
				.getServiceContextFromDomain(clusterId);

		if (serviceCtxt == null) {
			String msg = "Not a registered service: domain - " + clusterId;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		// load Cartridge
		serviceCtxt.setCartridge(loadCartridge(serviceCtxt.getCartridgeType(),
				 dataHolder
						.getCartridges()));

		if (serviceCtxt.getCartridge() == null) {
			String msg = "There's no registered Cartridge found. Domain - "
					+ clusterId;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		if (serviceCtxt.getCartridge().getIaases().isEmpty()) {
			String msg = "There's no registered IaaSes found for Cartridge type: "
					+ serviceCtxt.getCartridge().getType();
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}


		for (IaasProvider iaas : serviceCtxt.getCartridge().getIaases()) {

            if(cloud == null) {
                String msg = "There's no IaaS provided for the cluster: "
					+ clusterId + " to start an instance";
                log.fatal(msg);
                throw new CloudControllerException(msg);
            }

            if(region == null) {
                log.info("Region is not provided. Hence the default region is taken into the spawning a new instance");
            }
            //checking for the cloud and the region
            //TODO adding more locations and retrieve it from the request received
            if(iaas.getType().equals(cloud)) {
                IaasContext ctxt;
                if ((ctxt = serviceCtxt.getIaasContext(iaas.getType())) == null) {
                    ctxt = serviceCtxt.addIaasContext(iaas.getType());
                }
                try {
                    //generating the Unique member ID...
                    String memberID = generateMemberId(clusterId);
                    //have to add memberID to the payload
                    serviceCtxt.getPayload().append(",");
                    serviceCtxt.getPayload().append("MEMBER_ID=" + memberID);
                    //reloading the payload with memberID
                    reloadPayload(serviceCtxt.getCartridge(), serviceCtxt.generatePayload());

                    iaas.getIaas().setDynamicPayload(iaas);

                    // get the ComputeService
                    computeService = iaas.getComputeService();

                    // corresponding Template
                    template = iaas.getTemplate();

                    if (template == null) {
                        String msg = "Failed to start an instance in "
                                + iaas.getType()
                                + ". Reason : Template is null. You have not specify a matching service "
                                + "element in the configuration file of Autoscaler.\n Hence, will try to "
                                + "start in another IaaS if available.";
                        log.error(msg);
                        continue;
                    }

                    // set instance name as the host name
                    // template.getOptions().userMetadata("Name",
                    // serviceCtxt.getHostName());
                    // template.getOptions().as(TemplateOptions.class).userMetadata("Name",
                    // serviceCtxt.getHostName());

                    // generate the group id from domain name and sub domain
                    // name.
                    // Should have lower-case ASCII letters, numbers, or dashes.
                    // Should have a length between 3-15
                    String str = clusterId.substring(0, 10);
                    String group = str.replaceAll("[^a-z0-9-]", "");

                    NodeMetadata node;

                    // create and start a node
                    Set<? extends NodeMetadata> nodes = computeService
                            .createNodesInGroup(group, 1, template);

                    node = nodes.iterator().next();

                    String autoAssignIpProp = iaas
                            .getProperty(CloudControllerConstants.AUTO_ASSIGN_IP_PROPERTY);

                    // acquire the lock
                    lock.lock();

                    try {
                        // reset ip
                        ip = "";
                        // default behavior is autoIpAssign=false
                        if (autoAssignIpProp == null
                                || (autoAssignIpProp != null && autoAssignIpProp
                                        .equals("false"))) {
                            // allocate an IP address - manual IP assigning mode
                            ip = iaas.getIaas().associateAddress(iaas, node);
                        }

                        if (ip.isEmpty()
                                && node.getPublicAddresses() != null
                                && node.getPublicAddresses().iterator()
                                        .hasNext()) {
                            ip = node.getPublicAddresses().iterator().next();
                        }

                        // if not public IP is assigned, we're using private IP
                        if (ip.isEmpty()
                                && node.getPrivateAddresses() != null
                                && node.getPrivateAddresses().iterator()
                                        .hasNext()) {
                            ip = node.getPrivateAddresses().iterator().next();
                        }

                        if (node.getId() == null) {
                            String msg = "Node id of the starting instance is null.\n"
                                    + node.toString();
                            log.fatal(msg);
                            throw new CloudControllerException(msg);
                        }

                        // add node ID
                        ctxt.addNodeId(node.getId());
                        ctxt.addNodeToPublicIp(node.getId(), ip);

                        // to faster look up
                        dataHolder.addNodeId(
                                node.getId(), serviceCtxt);

                        serviceCtxt.getCartridge().setLastlyUsedIaas(iaas);

                        // add this ip to the topology
                        appendToPublicIpProperty(ip, serviceCtxt);

                        dataHolder.updateActiveInstanceCount(iaas.getType(), 1);

                        // persist in registry
                        persist();

                        // trigger topology
                        TopologyBuilder.handleMemberSpawned(memberID, serviceCtxt.getCartridgeType(), clusterId,
                                 node.getId(), locationScope);

                        //update the topology with the newly spawned member
                        // publish data
                        CartridgeInstanceDataPublisherTask.publish();
                        if (log.isDebugEnabled()) {
                            log.debug("Node details: \n" + node.toString()
                                    + "\n***************\n");
                        }

                        log.info("Instance is successfully starting up in IaaS "
                                + iaas.getType()
                                + ".\tIP Address(public/private): "
                                + ip
                                + "\tNode Id: " + node.getId());

                        return ip;

                    } finally {
                        // release the lock
                        lock.unlock();
                    }

                    } catch (Exception e) {
                        log.warn(
                                "Failed to start an instance in "
                                        + iaas.getType()
                                        + ". Hence, will try to start in another IaaS if available.",
                                e);
                    }
            }
            else {
                if(region != null) {
                   log.error("Failed to start an instance, in any IaaS: " + cloud +
                           " of the region: " + region +  "for the cluster: "
				            + clusterId);
                }
            }
        }
        return null;

	}

    @Override
    public String startInstances(String clusterId, LocationScope locationScope, int noOfInstancesToBeSpawned) {
        log.info("aaaaaaaaaaaaaaaaaaaaaaaa");
        //TODO
        return null;
    }

    /**
	 * Appends this ip to the Service Context's
	 * {@link CloudControllerConstants#PUBLIC_IP_PROPERTY}
	 * 
	 * @param ip
	 * @param serviceCtxt
	 */
	private void appendToPublicIpProperty(String ip, ServiceContext serviceCtxt) {
		String ipStr = serviceCtxt
				.getProperty(CloudControllerConstants.PUBLIC_IP_PROPERTY);
		if (ip != null && !"".equals(ip)) {
			serviceCtxt.setProperty(
					CloudControllerConstants.PUBLIC_IP_PROPERTY,
					("".equals(ipStr) ? "" : ipStr
							+ CloudControllerConstants.ENTRY_SEPARATOR)
							+ ip);
		}
	}

	/**
	 * Persist data in registry.
	 */
	private void persist() {
		try {
			RegistryManager.getInstance().persist(
					dataHolder);
		} catch (RegistryException e) {

			String msg = "Failed to persist the Cloud Controller data in registry. Further, transaction roll back also failed.";
			log.fatal(msg);
			throw new CloudControllerException(msg, e);
		}
	}

    private String generateMemberId(String clusterId) {
        UUID memberId = UUID.randomUUID();
         return clusterId + memberId.toString();
    }

	private Cartridge loadCartridge(String cartridgeType,
			List<Cartridge> cartridges) {

		for (Cartridge cartridge : cartridges) {
			if (cartridge.getType().equals(cartridgeType)) {
                return cartridge;
			}
		}

		return null;
	}

    private void reloadPayload(Cartridge cartridge, byte[] payload) {
        for (IaasProvider iaas : cartridge.getIaases()) {
					iaas.setPayload(payload);
        }

    }

	@Override
	public boolean terminateInstance(String clusterId, LocationScope locationScope) {

         String cloud = locationScope.getCloud();
        String region = locationScope.getRegion();
		log.info("Starting to terminate an instance of domain : " + clusterId);

		ServiceContext serviceCtxt = dataHolder
				.getServiceContextFromDomain(clusterId);

		if (serviceCtxt == null) {
			String msg = "Not a registered service: domain - " + clusterId;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		// load Cartridge, if null
		if (serviceCtxt.getCartridge() == null) {
			serviceCtxt.setCartridge(loadCartridge(
					serviceCtxt.getCartridgeType(),
					dataHolder.getCartridges()));
		}

		// if still, Cartridge is null
		if (serviceCtxt.getCartridge() == null) {
			String msg = "There's no registered Cartridge found. Domain - "
					+ clusterId;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

        for (IaasProvider iaas : serviceCtxt.getCartridge().getIaases()) {

			String msg = "Failed to terminate an instance in "
					+ iaas.getType()
					+ ". Hence, will try to terminate an instance in another IaaS if possible.";
            //TODO adding more locations and retrieve it from the request received
            if(iaas.getName().equals(cloud) && (region == null || iaas.getRegion().equals(region))) {
                String nodeId = null;

                IaasContext ctxt = serviceCtxt.getIaasContext(iaas.getType());

                // terminate the last instance first
                for (String id : Lists.reverse(ctxt.getNodeIds())) {
                    if (id != null) {
                        nodeId = id;
                        break;
                    }
                }

                // if no matching node id can be found.
                if (nodeId == null) {

                    log.warn(msg
                            + " : Reason- No matching instance found for domain: "
                            + clusterId
                            + ".");
                    continue;
                }

                // terminate it!
                terminate(iaas, ctxt, nodeId);

                // log information
                logTermination(nodeId, ctxt, serviceCtxt);
            }



			return true;

		}

		log.info("Termination of an instance which is belong to domain '"
				+ clusterId
				+ "' , failed! Reason: No matching "
				+ "running instance found in any available IaaS.");

		return false;

	}

    @Override
    public boolean terminateInstances(int noOfInstances, String clusterId, LocationScope locationScope) {
        log.info("vvvvvvvvvvvvvvvvv");
        return false;  //TODO
    }

    @Override
    public boolean terminateUnhealthyInstances(List<String> instancesToBeTerminated) {
        log.info("vvvvvvvvvvdddvvvvvvv");
        return false;  //TODO
    }

    @Override
	public boolean terminateLastlySpawnedInstance(String clusterId) {
        log.info("Starting to terminate the last instance spawned, of domain : "
				+ clusterId);

		ServiceContext serviceCtxt = dataHolder
				.getServiceContextFromDomain(clusterId);

		if (serviceCtxt == null) {
			String msg = "Not a registered service: domain - " + clusterId;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		// load Cartridge, if null
		if (serviceCtxt.getCartridge() == null) {
			serviceCtxt.setCartridge(loadCartridge(
					serviceCtxt.getCartridgeType(),
					dataHolder.getCartridges()));
		}

		if (serviceCtxt.getCartridge() == null) {
			String msg = "There's no registered Cartridge found. Domain - "
					+ clusterId;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		IaasProvider iaas = serviceCtxt.getCartridge().getLastlyUsedIaas();
		// this is required since, we need to find the correct reference.
		// caz if the lastly used iaas retrieved from registry, it is not a
		// reference.
		iaas = serviceCtxt.getCartridge().getIaasProvider(iaas.getType());

		if (iaas != null) {

			String nodeId = null;
			IaasContext ctxt = serviceCtxt.getIaasContext(iaas.getType());

			int i;
			for (i = ctxt.getNodeIds().size() - 1; i >= 0; i--) {
				String id = ctxt.getNodeIds().get(i);
				if (id != null) {
					nodeId = id;
					break;
				}
			}

			if (nodeId != null) {

				// terminate it!
				iaas = terminate(iaas, ctxt, nodeId);

				// log information
				logTermination(nodeId, ctxt, serviceCtxt);

				return true;
			}

		}

		log.info("Termination of an instance which is belong to domain '"
				+ clusterId + ", failed! Reason: No matching "
				+ "running instance found in lastly used IaaS.");

		return false;

	}

	@Override
	public boolean terminateAllInstances(String clusterId) {

		boolean isAtLeastOneTerminated = false;


		log.info("Starting to terminate all instances of domain : "
				+ clusterId);

		ServiceContext serviceCtxt = dataHolder
				.getServiceContextFromDomain(clusterId);

		if (serviceCtxt == null) {
			String msg = "Not a registered service: domain - " + clusterId;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		// load Cartridge, if null
		if (serviceCtxt.getCartridge() == null) {
			serviceCtxt.setCartridge(loadCartridge(
					serviceCtxt.getCartridgeType(),
					dataHolder.getCartridges()));
		}

		if (serviceCtxt.getCartridge() == null) {
			String msg = "There's no registered Cartridge found. Domain - "
					+ clusterId;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

        for (IaasProvider iaas : serviceCtxt.getCartridge().getIaases()) {

			IaasContext ctxt = serviceCtxt.getIaasContext(iaas.getType());

			if (ctxt == null) {
				log.error("Iaas Context for " + iaas.getType()
						+ " not found. Cannot terminate instances");
				continue;
			}

			ArrayList<String> temp = new ArrayList<String>(ctxt.getNodeIds());
			for (String id : temp) {
				if (id != null) {
					// terminate it!
					terminate(iaas, ctxt, id);

					// log information
					logTermination(id, ctxt, serviceCtxt);

					isAtLeastOneTerminated = true;
				}
			}
		}

		if (isAtLeastOneTerminated) {
			return true;
		}

		log.info("Termination of an instance which is belong to domain '"
				+ clusterId + "', failed! Reason: No matching "
				+ "running instance found in lastly used IaaS.");

		return false;

	}

	public int getPendingInstanceCount(String clusterId) {
        int pendingInstanceCount = 0;

		ServiceContext subjectedSerCtxt = dataHolder
				.getServiceContextFromDomain(clusterId);

		if (subjectedSerCtxt != null
				&& subjectedSerCtxt.getCartridgeType() != null) {

			// load cartridge
			subjectedSerCtxt.setCartridge(loadCartridge(subjectedSerCtxt
					.getCartridgeType(),
					dataHolder.getCartridges()));

			if (subjectedSerCtxt.getCartridge() == null) {
				return pendingInstanceCount;
			}

			List<IaasProvider> iaases = subjectedSerCtxt.getCartridge()
					.getIaases();

			for (IaasProvider iaas : iaases) {

				ComputeService computeService = iaas.getComputeService();

				IaasContext ctxt;
				if ((ctxt = subjectedSerCtxt.getIaasContext(iaas.getType())) == null) {
					ctxt = subjectedSerCtxt.addIaasContext(iaas.getType());
				}

				// get list of node Ids which are belong to this domain- sub
				// domain
				List<String> nodeIds = ctxt.getNodeIds();

				if (nodeIds.isEmpty()) {
					log.debug("Zero nodes spawned in the IaaS "
							+ iaas.getType() + " of domain: " + clusterId);
					continue;
				}

				// get all the nodes spawned by this IaasContext
				Set<? extends ComputeMetadata> set = computeService.listNodes();

                // traverse through all nodes of this ComputeService object
                for (ComputeMetadata aSet : set) {
                    NodeMetadataImpl nodeMetadata = (NodeMetadataImpl) aSet;

                    // if this node belongs to the requested domain
                    if (nodeIds.contains(nodeMetadata.getId())) {

                        // get the status of the node
                        Status nodeStatus = nodeMetadata.getStatus();

                        // count nodes that are in pending state
                        if (nodeStatus.equals(Status.PENDING)) {
                            pendingInstanceCount++;
                        }
                    }

                }
			}
		}

		log.debug("Pending instance count of domain '" + clusterId
				+ " is "
				+ pendingInstanceCount);

		return pendingInstanceCount;

	}

	/**
	 * A helper method to terminate an instance.
     * @param iaasTemp
     * @param ctxt
     * @param nodeId
     * @return will return the IaaSProvider
     */
	private IaasProvider terminate(IaasProvider iaasTemp, IaasContext ctxt,
			String nodeId) {

		// this is just to be safe
		if (iaasTemp.getComputeService() == null) {
			String msg = "Unexpeced error occured! IaasContext's ComputeService is null!";
			log.error(msg);
			throw new CloudControllerException(msg);
		}

		// destroy the node
		iaasTemp.getComputeService().destroyNode(nodeId);

		String autoAssignIpProp = iaasTemp
				.getProperty(CloudControllerConstants.AUTO_ASSIGN_IP_PROPERTY);

		// release allocated IP address
		if (autoAssignIpProp == null
				|| (autoAssignIpProp
						.equals("false"))) {
			// allocate an IP address - manual IP assigning mode
			iaasTemp.getIaas().releaseAddress(iaasTemp,
					ctxt.getPublicIp(nodeId));
		}

		// remove the node id
		ctxt.removeNodeId(nodeId);

		dataHolder.updateActiveInstanceCount(iaasTemp.getType(), -1);

		// publish data to BAM
		CartridgeInstanceDataPublisherTask.publish();

		log.info("Node with Id: '" + nodeId + "' is terminated!");
		return iaasTemp;
	}

	private void logTermination(String nodeId, IaasContext ctxt,
			ServiceContext serviceCtxt) {

		// get the ip of the terminated node
		String ip = ctxt.getPublicIp(nodeId);
		String ipProp = CloudControllerConstants.PUBLIC_IP_PROPERTY;
		String ipStr = serviceCtxt.getProperty(ipProp);
		StringBuilder newIpStr = new StringBuilder("");

		for (String str : ipStr.split(CloudControllerConstants.ENTRY_SEPARATOR)) {
			if (!str.equals(ip)) {
				newIpStr.append(str + CloudControllerConstants.ENTRY_SEPARATOR);
			}
		}

		// add this ip to the topology
		serviceCtxt.setProperty(ipProp, newIpStr.length() == 0 ? "" : newIpStr
				.substring(0, newIpStr.length() - 1).toString());
        //updating the topology
        TopologyBuilder.handleMemberTerminated(serviceCtxt.getCartridgeType(), serviceCtxt.getClusterId(), nodeId);

		// remove the reference
		ctxt.removeNodeIdToPublicIp(nodeId);

		// persist
		persist();

		//handle the termination event


	}

	/**
	 * Comparator to compare {@link IaasProvider} on different attributes.
	 */
	public enum IaasProviderComparator implements Comparator<IaasProvider> {
		SCALE_UP_SORT {
			public int compare(IaasProvider o1, IaasProvider o2) {
				return Integer.valueOf(o1.getScaleUpOrder()).compareTo(
						o2.getScaleUpOrder());
			}
		},
		SCALE_DOWN_SORT {
			public int compare(IaasProvider o1, IaasProvider o2) {
				return Integer.valueOf(o1.getScaleDownOrder()).compareTo(
						o2.getScaleDownOrder());
			}
		};

		public static Comparator<IaasProvider> ascending(
				final Comparator<IaasProvider> other) {
			return new Comparator<IaasProvider>() {
				public int compare(IaasProvider o1, IaasProvider o2) {
					return other.compare(o1, o2);
				}
			};
		}

		public static Comparator<IaasProvider> getComparator(
				final IaasProviderComparator... multipleOptions) {
			return new Comparator<IaasProvider>() {
				public int compare(IaasProvider o1, IaasProvider o2) {
					for (IaasProviderComparator option : multipleOptions) {
						int result = option.compare(o1, o2);
						if (result != 0) {
							return result;
						}
					}
					return 0;
				}
			};
		}
	}

	@Override
	public boolean registerService(String clusterId,
			String tenantRange, String cartridgeType, String hostName,
			Properties properties, String payload, String autoScalerPolicyName)
			throws UnregisteredCartridgeException {

		// create a ServiceContext dynamically
		ServiceContext newServiceCtxt = new ServiceContext();
		newServiceCtxt.setClusterId(clusterId);
		//newServiceCtxt.setSubDomainName(subDomain);
		newServiceCtxt.setTenantRange(tenantRange);
		newServiceCtxt.setHostName(hostName);
        newServiceCtxt.setAutoScalerPolicyName(autoScalerPolicyName);
        newServiceCtxt.setPayload(new StringBuilder(payload));

		if (properties != null && properties.getProperties() != null) {
			// add properties
			for (Property property : properties.getProperties()) {
				if (property != null && property.getName() != null) {
					newServiceCtxt.setProperty(property.getName(),
							property.getValue());
				}
			}
		}

		newServiceCtxt.setCartridgeType(cartridgeType);

		for (Cartridge cartridge : dataHolder
				.getCartridges()) {
			if (cartridge.getType().equals(cartridgeType)) {
				newServiceCtxt.setCartridge(cartridge);
				break;
			}
		}

		if (newServiceCtxt.getCartridge() == null) {
			String msg = "Registration failed - Unregistered Cartridge type: "
					+ cartridgeType;
			log.error(msg);
			throw new UnregisteredCartridgeException(msg);
		}

/*

			// write payload file
			try {
				String uniqueName = clusterId + ".txt";
				FileUtils.forceMkdir(new File(
						CloudControllerConstants.PAYLOAD_DIR));
				File payloadFile = new File(
						CloudControllerConstants.PAYLOAD_DIR + uniqueName);
				FileUtils.writeByteArrayToFile(payloadFile, payload);
				newServiceCtxt.setPayloadFile(payloadFile.getPath());

			} catch (IOException e) {
				String msg = "Failed while persisting the payload of clusterId : "
						+ clusterId;
				log.error(msg, e);
				throw new CloudControllerException(msg, e);
			}

		} else {
			log.debug("Payload is null or empty for :\n "
					+ newServiceCtxt);
		}*/

		// persist
        String uniqueName = clusterId + "-"
                + UUID.randomUUID() + ".xml";
        try {
            FileUtils.writeStringToFile(new File(CloudControllerConstants.SERVICES_DIR + uniqueName),
                                            newServiceCtxt.toXml());
        } catch (IOException e) {
            //TODO
        }
        log.info("Service successfully registered! Domain - " + clusterId
                + ", Cartridge type - " + cartridgeType);

		return true;
	}

	@Override
	public String[] getRegisteredCartridges() {
		// get the list of cartridges registered
		List<Cartridge> cartridges = dataHolder
				.getCartridges();

		if (cartridges == null) {
			return new String[0];
		}

		String[] cartridgeTypes = new String[cartridges.size()];
		int i = 0;

		for (Cartridge cartridge : cartridges) {
			cartridgeTypes[i] = cartridge.getType();
			i++;
		}

		return cartridgeTypes;
	}

	/*private String checkSubDomain(String subDomainName) {
		// if sub domain is null, we assume it as default one.
		if (subDomainName == null || "null".equalsIgnoreCase(subDomainName)) {
			subDomainName = Constants.DEFAULT_SUB_DOMAIN;
			log.debug("Sub domain is null, hence using the default value : "
					+ subDomainName);
		}

		return subDomainName;
	}*/

	@Override
	public CartridgeInfo getCartridgeInfo(String cartridgeType)
			throws UnregisteredCartridgeException {
		Cartridge cartridge = dataHolder
				.getCartridge(cartridgeType);

		if (cartridge != null) {

			return CloudControllerUtil.toCartridgeInfo(cartridge);

		}

		String msg = "Cannot find a Cartridge having a type of "
				+ cartridgeType + ". Hence unable to find information.";
		log.error(msg);
		throw new UnregisteredCartridgeException(msg);
	}

    @Override
	public boolean unregisterService(String clusterId)
			throws UnregisteredServiceException {

		//subDomain = checkSubDomain(subDomain);

		// find the service context
		ServiceContext subjectedSerCtxt = dataHolder
				.getServiceContextFromDomain(clusterId);

		if (subjectedSerCtxt == null) {
			throw new UnregisteredServiceException(
					"No registered service found for domain: " + clusterId);
		}

		// get the service definition file.
		File serviceDefFile = subjectedSerCtxt.getFile();

		// delete that file, so that it gets automatically undeployed.
		return serviceDefFile.delete();
	}

}
