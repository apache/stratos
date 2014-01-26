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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.concurrent.ThreadExecutor;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.interfaces.CloudControllerService;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.jcloud.ComputeServiceBuilderUtil;
import org.apache.stratos.cloud.controller.persist.Deserializer;
import org.apache.stratos.cloud.controller.pojo.*;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.topology.TopologyBuilder;
import org.apache.stratos.cloud.controller.topology.TopologyManager;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.apache.stratos.messaging.domain.topology.Member;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
					currentData.setClusterIdToContext(serializedObj.getClusterIdToContext());
					currentData.setMemberIdToContext(serializedObj.getMemberIdToContext());
					currentData.setClusterIdToMemberContext(serializedObj.getClusterIdToMemberContext());
					currentData.setCartridges(serializedObj.getCartridges());

					if(log.isDebugEnabled()) {
					    
					    log.debug("Cloud Controller Data is retrieved from registry.");
					}
				} else {
				    if(log.isDebugEnabled()) {
				        
				        log.debug("Cloud Controller Data cannot be found in registry.");
				    }
				}
			} catch (Exception e) {

				String msg = "Unable to acquire data from Registry. Hence, any historical data will not get reflected.";
				log.warn(msg, e);
			}

		}
	}

    public void deployCartridgeDefinition(CartridgeConfig cartridgeConfig) throws InvalidCartridgeDefinitionException, InvalidIaasProviderException {

        if (cartridgeConfig == null) {
            String msg = "Invalid Cartridge Definition: Definition is null.";
            log.error(msg);
            throw new IllegalArgumentException(msg);

        }

        if(log.isDebugEnabled()){
            log.debug("Cartridge definition: " + cartridgeConfig.toString());
        }

        Cartridge cartridge = null;
        try {
            cartridge = CloudControllerUtil.toCartridge(cartridgeConfig);
        } catch (Exception e) {
            String msg =
                         "Invalid Cartridge Definition: Cartridge Type: " +
                                 cartridgeConfig.getType()+
                                 ". Cause: Cannot instantiate a Cartridge Instance with the given Config.";
            log.error(msg, e);
            throw new InvalidCartridgeDefinitionException(msg, e);
        }

        List<IaasProvider> iaases = cartridge.getIaases();
        
        if (iaases == null || iaases.isEmpty()) {
            String msg =
                         "Invalid Cartridge Definition: Cartridge Type: " +
                                 cartridgeConfig.getType()+
                                 ". Cause: Iaases of this Cartridge is null or empty.";
            log.error(msg);
            throw new InvalidCartridgeDefinitionException(msg);
        }
        
        for (IaasProvider iaasProvider : iaases) {
            setIaas(iaasProvider);
        }
        
        // TODO transaction begins
        String cartridgeType = cartridge.getType();
        if(dataHolder.getCartridge(cartridgeType) != null) {
            if (dataHolder.getCartridges().remove(cartridge)) {
                log.info("Successfully undeployed the Cartridge definition: " + cartridgeType);
            }
        }
        
        dataHolder.addCartridge(cartridge);
        
        // persist
        persist();

        List<Cartridge> cartridgeList = new ArrayList<Cartridge>();
        cartridgeList.add(cartridge);

        TopologyBuilder.handleServiceCreated(cartridgeList);
        // transaction ends
        
        log.info("Successfully deployed the Cartridge definition: " + cartridgeType);
    }

	private Iaas setIaas(IaasProvider iaasProvider) throws InvalidIaasProviderException {

		Iaas iaas;
		try {
			Constructor<?> c = Class.forName(iaasProvider.getClassName())
					.getConstructor(IaasProvider.class);
			iaas = (Iaas) c.newInstance(iaasProvider);
		} catch (Exception e) {
			String msg = "Class [" + iaasProvider.getClassName()
					+ "] which represents the iaas of type: ["
					+ iaasProvider.getType() + "] has failed to instantiate.";
			log.error(msg, e);
			throw new InvalidIaasProviderException(msg, e);
		}

		try {
			iaas.buildComputeServiceAndTemplate();
			iaasProvider.setIaas(iaas);
			return iaas;
		} catch (Exception e) {
			String msg = "Unable to build the jclouds object for iaas "
					+ "of type: " + iaasProvider.getType();
			log.error(msg, e);
			throw new InvalidIaasProviderException(msg, e);
		}
	}

    public void undeployCartridgeDefinition(String cartridgeType) {

        Cartridge cartridge = null;
        if((cartridge = dataHolder.getCartridge(cartridgeType)) != null) {
            if (dataHolder.getCartridges().remove(cartridge)) {
                persist();
                log.info("Successfully undeployed the Cartridge definition: " + cartridgeType);
            }
        }
    }
    
    @Override
    public MemberContext startInstance(MemberContext memberContext) throws IllegalArgumentException,
        UnregisteredCartridgeException {

        if (memberContext == null) {
            String msg = "Instance start-up failed. Member is null.";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        String clusterId = memberContext.getClusterId();
        Partition partition = memberContext.getPartition();

		log.debug("Received an instance spawn request : " + memberContext.toString());

        ComputeService computeService = null;
        Template template = null;

        if (partition == null) {
            String msg =
                         "Instance start-up failed. Specified Partition is null. " +
                                 memberContext.toString();
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        String partitionId = partition.getId();
        ClusterContext ctxt = dataHolder.getClusterContext(clusterId);

        if (ctxt == null) {
            String msg = "Instance start-up failed. Invalid cluster id. " + memberContext.toString();
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        String cartridgeType = ctxt.getCartridgeType();

        Cartridge cartridge = dataHolder.getCartridge(cartridgeType);

        if (cartridge == null) {
            String msg =
                         "Instance start-up failed. No valid Cartridge found. " +
                                 memberContext.toString();
            log.error(msg);
            throw new UnregisteredCartridgeException(msg);
        }

        memberContext.setCartridgeType(cartridgeType);


        IaasProvider iaasProvider = cartridge.getIaasProviderOfPartition(partitionId);
        if (iaasProvider == null) {
            String msg =
                         "Instance start-up failed. " + "There's no IaaS provided for the partition: " + partitionId +
                         " and for the Cartridge type: " + cartridgeType+". Only following "
                  		+ "partitions can be found in this Cartridge: "
                  		+cartridge.getPartitionToIaasProvider().keySet().toString()+ memberContext.toString() + ". ";
            log.fatal(msg);
            throw new CloudControllerException(msg);
        }
        String type = iaasProvider.getType();
        try {
            // generating the Unique member ID...
            String memberID = generateMemberId(clusterId);
            memberContext.setMemberId(memberID);
            // have to add memberID to the payload
            StringBuilder payload = new StringBuilder(ctxt.getPayload());
            addToPayload(payload, "MEMBER_ID", memberID);
            addToPayload(payload, "LB_CLUSTER_ID", memberContext.getLbClusterId());
            addToPayload(payload, "NETWORK_PARTITION_ID", memberContext.getNetworkPartitionId());
            addToPayload(payload, "PARTITION_ID", partitionId);
                        
            addToPayload(payload, "PERSISTANCE_MAPPING", getPersistancePayload(cartridge).toString());
            
            if (log.isDebugEnabled()) {
                log.debug("Payload: " + payload.toString());
            }
            // reloading the payload with memberID
            iaasProvider.setPayload(payload.toString().getBytes());

            Iaas iaas = iaasProvider.getIaas();
            
            if (iaas == null) {
                if(log.isDebugEnabled()) {
                    log.debug("Iaas is null of Iaas Provider: "+type+". Trying to build IaaS...");
                }
                try {
                    iaas = setIaas(iaasProvider);
                } catch (InvalidIaasProviderException e) {
                    String msg ="Instance start up failed. "+memberContext.toString()+
                            "Unable to build Iaas of this IaasProvider [Provider] : " + type;
                    log.error(msg, e);
                    throw new CloudControllerException(msg, e);
                }
                
            }
            
            iaas.setDynamicPayload();
            // get the pre built ComputeService from provider or region or zone or host
            computeService = iaasProvider.getComputeService();
            template = iaasProvider.getTemplate();
                        
            if (template == null) {
                String msg =
                             "Failed to start an instance. " +
                                     memberContext.toString() +
                                     ". Reason : Template is null. You have not specify a matching service " +
                                     "element in the configuration file of Autoscaler.\n Hence, will try to " +
                                     "start in another IaaS if available.";
                log.error(msg);
                throw new CloudControllerException(msg);
            }

            // generate the group id from domain name and sub domain
            // name.
            // Should have lower-case ASCII letters, numbers, or dashes.
            // Should have a length between 3-15
            String str = clusterId.length() > 10 ? clusterId.substring(0, 10) : clusterId.substring(0, clusterId.length());
            String group = str.replaceAll("[^a-z0-9-]", "");
            NodeMetadata node;

//            create and start a node
            Set<? extends NodeMetadata> nodes =
                                                computeService.createNodesInGroup(group, 1,
                                                                                  template);

            node = nodes.iterator().next();
            //Start allocating ip as a new job

            ThreadExecutor exec = ThreadExecutor.getInstance();
            exec.execute(new IpAllocator(memberContext, iaasProvider, cartridgeType, node));


            // node id
            String nodeId = node.getId();
            if (nodeId == null) {
                String msg = "Node id of the starting instance is null.\n" + memberContext.toString();
                log.fatal(msg);
                throw new CloudControllerException(msg);
            }
                memberContext.setNodeId(nodeId);
                if(log.isDebugEnabled()) {
                    log.debug("Node id was set. "+memberContext.toString());
                }

            log.info("Instance is successfully starting up. "+memberContext.toString());

            return memberContext;

        } catch (Exception e) {
            String msg = "Failed to start an instance. " + memberContext.toString();
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }

    }

	private StringBuilder getPersistancePayload(Cartridge cartridge) {
		StringBuilder persistancePayload = new StringBuilder();
		if(isPersistanceMappingAvailable(cartridge)){
			int i=0;
			for(; i<cartridge.getPeristanceMappings().size()-1;i++){
				if(log.isDebugEnabled()){
					log.debug("Adding persistance mapping " + cartridge.getPeristanceMappings().get(i).toString());
				}
				persistancePayload.append(cartridge.getPeristanceMappings().get(i).getDevice());
				persistancePayload.append("|");
			}
			persistancePayload.append(cartridge.getPeristanceMappings().get(i).getDevice());
		}
		return persistancePayload;
	}

	private boolean isPersistanceMappingAvailable(Cartridge cartridge) {
		return cartridge.getPeristanceMappings() != null && !cartridge.getPeristanceMappings().isEmpty();
	}

	private void addToPayload(StringBuilder payload, String name, String value) {
	    payload.append(",");
        payload.append(name+"=" + value);
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

    @Override
    public void terminateInstance(String memberId) throws InvalidMemberException, InvalidCartridgeTypeException, 
    IllegalArgumentException{

        if(memberId == null) {
            String msg = "Termination failed. Null member id.";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        
        MemberContext ctxt = dataHolder.getMemberContextOfMemberId(memberId);
        
        if(ctxt == null) {
            String msg = "Termination failed. Invalid Member Id: "+memberId;
            log.error(msg);
            throw new InvalidMemberException(msg);
        }
        
        ThreadExecutor exec = ThreadExecutor.getInstance();
        exec.execute(new InstanceTerminator(ctxt));

	}
    
    private class InstanceTerminator implements Runnable {

        private MemberContext ctxt;

        public InstanceTerminator(MemberContext ctxt) {
            this.ctxt = ctxt;
        }

        @Override
        public void run() {

            String memberId = ctxt.getMemberId();
            String clusterId = ctxt.getClusterId();
            String partitionId = ctxt.getPartition().getId();
            String cartridgeType = ctxt.getCartridgeType();
            String nodeId = ctxt.getNodeId();

            try {
                // these will never be null, since we do not add null values for these.
                Cartridge cartridge = dataHolder.getCartridge(cartridgeType);

                log.info("Starting to terminate an instance with member id : " + memberId +
                         " in partition id: " + partitionId + " of cluster id: " + clusterId +
                         " and of cartridge type: " + cartridgeType);

                if (cartridge == null) {
                    String msg =
                                 "Termination of Member Id: " + memberId + " failed. " +
                                         "Cannot find a matching Cartridge for type: " +
                                         cartridgeType;
                    log.error(msg);
                    throw new InvalidCartridgeTypeException(msg);
                }

                // if no matching node id can be found.
                if (nodeId == null) {

                    String msg =
                                 "Termination failed. Cannot find a node id for Member Id: " +
                                         memberId;
                    log.error(msg);
                    throw new InvalidMemberException(msg);
                }

                IaasProvider iaasProvider = cartridge.getIaasProviderOfPartition(partitionId);

                // terminate it!
                terminate(iaasProvider, nodeId, ctxt);

                // log information
                logTermination(ctxt);

            } catch (Exception e) {
                String msg =
                             "Instance termination failed. "+ctxt.toString();
                log.error(msg, e);
                throw new CloudControllerException(msg, e);
            }

        }
    }

    private class IpAllocator implements Runnable {

        private MemberContext memberContext;
        private IaasProvider iaasProvider;
        private String cartridgeType;
        NodeMetadata node;

        public IpAllocator(MemberContext memberContext, IaasProvider iaasProvider, 
        		String cartridgeType, NodeMetadata node) {
            this.memberContext = memberContext;
            this.iaasProvider = iaasProvider;
            this.cartridgeType = cartridgeType;
            this.node = node;
        }

        @Override
        public void run() {


            String clusterId = memberContext.getClusterId();
            Partition partition = memberContext.getPartition();

            try{

                String autoAssignIpProp =
                                          iaasProvider.getProperty(CloudControllerConstants.AUTO_ASSIGN_IP_PROPERTY);

                    // reset ip
                    String ip = "";
                    // default behavior is autoIpAssign=false
                    if (autoAssignIpProp == null ||
                        (autoAssignIpProp != null && autoAssignIpProp.equals("false"))) {

                        Iaas iaas = iaasProvider.getIaas();
                        // allocate an IP address - manual IP assigning mode
                        ip = iaas.associateAddress(node);
                        memberContext.setAllocatedIpAddress(ip);
                        log.info("Allocated an ip address: " + memberContext.toString());
                    }

                    // public ip
                    if (node.getPublicAddresses() != null &&
                        node.getPublicAddresses().iterator().hasNext()) {
                        ip = node.getPublicAddresses().iterator().next();
                        memberContext.setPublicIpAddress(ip);
                        log.info("Public IP Address has been set. " + memberContext.toString());
                    }

                    // private IP
                    if (node.getPrivateAddresses() != null &&
                        node.getPrivateAddresses().iterator().hasNext()) {
                        ip = node.getPrivateAddresses().iterator().next();
                        memberContext.setPrivateIpAddress(ip);
                        log.info("Private IP Address has been set. " + memberContext.toString());
                    }

                    dataHolder.addMemberContext(memberContext);

                    // persist in registry
                    persist();

                    String memberID = memberContext.getMemberId();

                    // trigger topology
                    TopologyBuilder.handleMemberSpawned(memberID, cartridgeType, clusterId, memberContext.getNetworkPartitionId(),
                            partition.getId(), ip, memberContext.getLbClusterId());

                    // update the topology with the newly spawned member
                    // publish data
                    if (log.isDebugEnabled()) {
                        log.debug("Node details: \n" + node.toString());
                    }

            } catch (Exception e) {
                String msg = "Error occurred while allocating an ip address. " + memberContext.toString();
                log.error(msg, e);
                throw new CloudControllerException(msg, e);
            }


        }
    }

//    private
//        void
//        terminateInstance(MemberContext ctxt) throws InvalidCartridgeTypeException,
//            InvalidMemberException {
//        // these will never be null, since we do not add null values for these.
//        String memberId = ctxt.getMemberId();
//        String clusterId = ctxt.getClusterId();
//        String partitionId = ctxt.getPartitionId();
//        String cartridgeType = ctxt.getCartridgeType();
//        String nodeId = ctxt.getNodeId();
//        
//        Cartridge cartridge = dataHolder.getCartridge(cartridgeType);
//        
//        log.info("Starting to terminate an instance with member id : " + memberId+
//                 " in partition id: "+partitionId+" of cluster id: "+clusterId+ " and of cartridge type: "+cartridgeType);
//        
//        if(cartridge == null) {
//            String msg = "Termination of Member Id: "+memberId+" failed. " +
//                    "Cannot find a matching Cartridge for type: "+cartridgeType;
//            log.error(msg);
//            throw new InvalidCartridgeTypeException(msg);
//        }
//        
////        Scope scope = partition.getScope();
////        String provider = partition.getProperty("provider");
//
//		// if no matching node id can be found.
//        if (nodeId == null) {
//
//            String msg = "Termination failed. Cannot find a node id for Member Id: "+memberId;
//            log.error(msg);
//            throw new InvalidMemberException(msg);
//        }
////		ServiceContext serviceCtxt = dataHolder
////				.getServiceContextFromDomain(clusterId);
////
////		if (serviceCtxt == null) {
////			String msg = "Not a registered service: domain - " + clusterId;
////			log.fatal(msg);
////			throw new CloudControllerException(msg);
////		}
////
////		// load Cartridge, if null
////		//if (serviceCtxt.getCartridge() == null) {
////			serviceCtxt.setCartridge(loadCartridge(
////					serviceCtxt.getCartridgeType(),
////					dataHolder.getCartridges()));
////		//}
////
////		// if still, Cartridge is null
////		if (serviceCtxt.getCartridge() == null) {
////			String msg = "There's no registered Cartridge found. Domain - "
////					+ clusterId;
////			log.fatal(msg);
////			throw new CloudControllerException(msg);
////		}
//
////        for (IaasProvider iaas : serviceCtxt.getCartridge().getIaases()) {
//
//		IaasProvider iaas = cartridge.getIaasProviderOfPartition(partitionId);
//		
////			String msg = "Failed to terminate an instance in "
////					+ iaas.getType()
////					+ ". Hence, will try to terminate an instance in another IaaS if possible.";
////            //TODO adding more locations and retrieve it from the request received
////                String nodeId = null;
//
////                IaasContext ctxt = serviceCtxt.getIaasContext(iaas.getType());
//
////                // terminate the last instance first
////                for (String id : Lists.reverse(ctxt.getNodeIds())) {
////                    if (id != null) {
////                        nodeId = id;
////                        break;
////                    }
////                }
//
//                
//
//                // terminate it!
//                terminate(iaas, nodeId, ctxt);
//
//                // log information
//                logTermination(nodeId, ctxt);
//    }

//    @Override
//    public boolean terminateInstances(String[] memberIds) throws IllegalArgumentException, InvalidMemberException, InvalidCartridgeTypeException {
//        for (String memberId : memberIds) {
//            terminateInstance(memberId);
//        }
//    }

//    @Override
//    public boolean terminateUnhealthyInstances(List<String> instancesToBeTerminated) {
//        log.info("vvvvvvvvvvdddvvvvvvv");
//        return false;  //TODO
//    }

	@Override
	public void terminateAllInstances(String clusterId) throws IllegalArgumentException, InvalidClusterException {

		log.info("Starting to terminate all instances of cluster : "
				+ clusterId);
		
		if(clusterId == null) {
		    String msg = "Instance termination failed. Cluster id is null.";
		    log.error(msg);
		    throw new IllegalArgumentException(msg);
		}
		
		List<MemberContext> ctxts = dataHolder.getMemberContextsOfClusterId(clusterId);
		
		if(ctxts == null) {
		    String msg = "Instance termination failed. No members found for cluster id: "+clusterId;
		    log.error(msg);
		    throw new InvalidClusterException(msg);
		}
		
		ThreadExecutor exec = ThreadExecutor.getInstance();
		for (MemberContext memberContext : ctxts) {
            exec.execute(new InstanceTerminator(memberContext));
        }
		

//		ServiceContext serviceCtxt = dataHolder
//				.getServiceContextFromDomain(clusterId);
//
//		if (serviceCtxt == null) {
//			String msg = "Not a registered service: domain - " + clusterId;
//			log.fatal(msg);
//			throw new CloudControllerException(msg);
//		}
//
//		// load Cartridge, if null
//		serviceCtxt.setCartridge(loadCartridge(
//					serviceCtxt.getCartridgeType(),
//					dataHolder.getCartridges()));
//		//}
//
//		if (serviceCtxt.getCartridge() == null) {
//			String msg = "There's no registered Cartridge found. Domain - "
//					+ clusterId;
//			log.fatal(msg);
//			throw new CloudControllerException(msg);
//		}

//        for (IaasProvider iaas : serviceCtxt.getCartridge().getIaases()) {
//
//			IaasContext ctxt = serviceCtxt.getIaasContext(iaas.getType());
//
//			if (ctxt == null) {
//				log.error("Iaas Context for " + iaas.getType()
//						+ " not found. Cannot terminate instances");
//				continue;
//			}
//
//			ArrayList<String> temp = new ArrayList<String>(ctxt.getNodeIds());
//			for (String id : temp) {
//				if (id != null) {
//					// terminate it!
//                    //TODO need to enable once partition added to the topology
//                    /*Collection<Member> members = TopologyManager.getInstance().getTopology().
//                            getService(serviceCtxt.getCartridgeType()).
//                            getCluster(serviceCtxt.getClusterId()).getMembers();
//                    for (Iterator iterator = members.iterator(); iterator.hasNext();) {
//                         Member member = (Member) iterator.next();
//                         terminate(iaas, ctxt, member.getIaasNodeId(), member.getPartition());
//                    }*/
//
//					// log information
//					logTermination(id, ctxt, serviceCtxt);
//
//					isAtLeastOneTerminated = true;
//				}
//			}
//		}
//
//		if (isAtLeastOneTerminated) {
//			return true;
//		}
//
//		log.info("Termination of an instance which is belong to domain '"
//				+ clusterId + "', failed! Reason: No matching "
//				+ "running instance found in lastly used IaaS.");
//
//		return false;

	}


	/**
	 * A helper method to terminate an instance.
     * @param iaasProvider
     * @param ctxt
     * @param nodeId
     * @return will return the IaaSProvider
     */
	private IaasProvider terminate(IaasProvider iaasProvider, 
			String nodeId, MemberContext ctxt) {
	    Iaas iaas = iaasProvider.getIaas();
	    if (iaas == null) {
	        
	        try {
	            iaas = setIaas(iaasProvider);
	        } catch (InvalidIaasProviderException e) {
	            String msg =
	                    "Instance termination failed. " +ctxt.toString()  +
	                    ". Cause: Unable to build Iaas of this " + iaasProvider.toString();
	            log.error(msg, e);
	            throw new CloudControllerException(msg, e);
	        }
	        
	    }
		// destroy the node
		iaasProvider.getComputeService().destroyNode(nodeId);

		// release allocated IP address
		if (ctxt.getAllocatedIpAddress() != null) {
            iaas.releaseAddress(ctxt.getAllocatedIpAddress());
		}
		
		// publish data to BAM
//		CartridgeInstanceDataPublisherTask.publish();

		log.info("Member is terminated: "+ctxt.toString());
		return iaasProvider;
	}

	private void logTermination(MemberContext memberContext) {

        //updating the topology
        TopologyBuilder.handleMemberTerminated(memberContext.getCartridgeType(), memberContext.getClusterId(), memberContext.getNetworkPartitionId(), memberContext.getPartition().getId(), memberContext.getMemberId());

		// persist
		persist();

	}

	@Override
	public boolean registerService(Registrant registrant)
			throws UnregisteredCartridgeException, IllegalArgumentException {

	    String cartridgeType = registrant.getCartridgeType();
	    String clusterId = registrant.getClusterId();
        String payload = registrant.getPayload();
        String hostName = registrant.getHostName();
        
        if(cartridgeType == null || clusterId == null || payload == null || hostName == null) {
	        String msg = "Null Argument/s detected: Cartridge type: "+cartridgeType+", " +
	                "Cluster Id: "+clusterId+", Payload: "+payload+", Host name: "+hostName;
	        log.error(msg);
	        throw new IllegalArgumentException(msg);
	    }
	    
        if (dataHolder.getCartridge(cartridgeType) == null) {

            String msg = "Registration of cluster: "+clusterId+
                    " failed. - Unregistered Cartridge type: " + cartridgeType;
            log.error(msg);
            throw new UnregisteredCartridgeException(msg);
        }
        
	    dataHolder.addClusterContext(new ClusterContext(clusterId, cartridgeType, payload, hostName));
	    TopologyBuilder.handleClusterCreated(registrant);
	    
	    persist();
	    
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
	public void unregisterService(String clusterId) throws UnregisteredClusterException {
        final String clusterId_ = clusterId;
            Runnable r = new Runnable() {
                 public void run() {
                     ClusterContext ctxt = dataHolder.getClusterContext(clusterId_);
                     Collection<Member> members = TopologyManager.getTopology().
                             getService(ctxt.getCartridgeType()).getCluster(clusterId_).getMembers();
                     while(members.size() > 0) {
                        //waiting until all the members got removed from the Topology
                        CloudControllerUtil.sleep(1000);
                     }
                    if(ctxt == null) {
                        String msg = "Unregistration of service cluster failed. Cluster not found: " + clusterId_;
                        log.error(msg);
                    }
                     log.info("Unregistration of service cluster: " + clusterId_);
                     TopologyBuilder.handleClusterRemoved(ctxt);
                     dataHolder.removeClusterContext(clusterId_);
                     dataHolder.removeMemberContext(clusterId_);
                     persist();
                 }
            };
        new Thread(r).start();
        
	}

		

    @Override
    public boolean validateDeploymentPolicy(String cartridgeType, Partition[] partitions) 
            throws InvalidPartitionException, InvalidCartridgeTypeException {

        Map<String, IaasProvider> partitionToIaasProviders =
                                                             new ConcurrentHashMap<String, IaasProvider>();

        Cartridge cartridge = dataHolder.getCartridge(cartridgeType);

        if (cartridge == null) {
            String msg = "Invalid Cartridge Type: " + cartridgeType;
            log.error(msg);
            throw new InvalidCartridgeTypeException(msg);
        }

        for (Partition partition : partitions) {
            String provider = partition.getProvider();
            IaasProvider iaasProvider = cartridge.getIaasProvider(provider);

            if (iaasProvider == null) {
                String msg =
                             "Invalid Partition - " + partition.toString() +
                                     ". Cause: Iaas Provider is null for Provider: " + provider;
                log.error(msg);
                throw new InvalidPartitionException(msg);
            }

            Iaas iaas = iaasProvider.getIaas();
            
            if (iaas == null) {
                
                try {
                    iaas = setIaas(iaasProvider);
                } catch (InvalidIaasProviderException e) {
                    String msg =
                            "Invalid Partition - " + partition.toString() +
                            ". Cause: Unable to build Iaas of this IaasProvider [Provider] : " + provider;
                    log.error(msg, e);
                    throw new InvalidPartitionException(msg, e);
                }
                
            }
            
            PartitionValidator validator = iaas.getPartitionValidator();
            validator.setIaasProvider(iaasProvider);
            IaasProvider updatedIaasProvider =
                                               validator.validate(partition.getId(),
                                                                  CloudControllerUtil.toJavaUtilProperties(partition.getProperties()));
            // add to a temporary Map
            partitionToIaasProviders.put(partition.getId(), updatedIaasProvider);
            
            if (log.isDebugEnabled()) {
            	log.debug("Partition "+partition.toString()+ " is validated successfully "
            			+ "against the Cartridge: "+cartridgeType);
            }

        }

        // if and only if the deployment policy valid
        cartridge.addIaasProviders(partitionToIaasProviders);
        
        // persist data
        persist();
        
        log.info("All partitions "+CloudControllerUtil.getPartitionIds(partitions)+
        		" were validated successfully, against the Cartridge: "+cartridgeType);
        
        return true;
    }

    @Override
    public boolean validatePartition(Partition partition) throws InvalidPartitionException {
    	//FIXME add logs
        String provider = partition.getProvider();
        IaasProvider iaasProvider = dataHolder.getIaasProvider(provider);

        if (iaasProvider == null) {
            String msg =
                         "Invalid Partition - " + partition.toString()+". Cause: Iaas Provider " +
                                 "is null for Partition Provider: "+provider;
            log.error(msg);
            throw new InvalidPartitionException(msg);
        }
        
        Iaas iaas = iaasProvider.getIaas();
        
        if (iaas == null) {
            
        	try {
                iaas = setIaas(iaasProvider);
            } catch (InvalidIaasProviderException e) {
                String msg =
                        "Invalid Partition - " + partition.toString() +
                        ". Cause: Unable to build Iaas of this IaasProvider [Provider] : " + provider;
                log.error(msg, e);
                throw new InvalidPartitionException(msg, e);
            }
            
        }

        PartitionValidator validator = iaas.getPartitionValidator();
        validator.setIaasProvider(iaasProvider);
        validator.validate(partition.getId(),
                           CloudControllerUtil.toJavaUtilProperties(partition.getProperties()));
        
        return true;
    }

}

