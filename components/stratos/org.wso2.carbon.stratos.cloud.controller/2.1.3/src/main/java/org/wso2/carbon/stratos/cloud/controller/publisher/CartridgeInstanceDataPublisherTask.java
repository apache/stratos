/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.stratos.cloud.controller.publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.internal.NodeMetadataImpl;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.DataPublisher;
import org.wso2.carbon.databridge.agent.thrift.conf.AgentConfiguration;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.NoStreamDefinitionExistException;
import org.wso2.carbon.ntask.core.Task;
import org.wso2.carbon.stratos.cloud.controller.exception.CloudControllerException;
import org.wso2.carbon.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerConstants;
import org.wso2.carbon.stratos.cloud.controller.util.CartridgeInstanceData;
import org.wso2.carbon.stratos.cloud.controller.util.IaasContext;
import org.wso2.carbon.stratos.cloud.controller.util.IaasProvider;
import org.wso2.carbon.stratos.cloud.controller.util.ServiceContext;
import org.wso2.carbon.utils.CarbonUtils;

import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;

public class CartridgeInstanceDataPublisherTask implements Task{
    
    private static final Log log = LogFactory.getLog(CartridgeInstanceDataPublisherTask.class);
    private static DataPublisher dataPublisher;
    private static String streamId;
    private static final String cloudControllerEventStreamVersion = "1.0.0";
    private static List<CartridgeInstanceData> dataToBePublished ;

    protected enum NodeStatus {
        PENDING, RUNNING, SUSPENDED, TERMINATED, ERROR, UNRECOGNIZED
    };   

    @Override
    public void execute() {
        
        publish();
    }
    
    public static void publish(){
        if(FasterLookUpDataHolder.getInstance().isPublisherRunning() ||
                // this is a temporary fix to avoid task execution - limitation with ntask
                !FasterLookUpDataHolder.getInstance().getEnableBAMDataPublisher()){
            return;
        }
        
        log.debug(CloudControllerConstants.DATA_PUB_TASK_NAME+" cycle started.");
        FasterLookUpDataHolder.getInstance().setPublisherRunning(true);
        dataToBePublished = new ArrayList<CartridgeInstanceData>();

        if(dataPublisher==null){
            createDataPublisher();

            //If we cannot create a data publisher we should give up
            //this means data will not be published
            if(dataPublisher == null){
                log.error("Data Publisher cannot be created or found.");
                release();
                return;
            }
        }

        if(streamId == null){
            try{
                streamId = dataPublisher.findStream(CloudControllerConstants.CLOUD_CONTROLLER_EVENT_STREAM, cloudControllerEventStreamVersion);
            }catch (NoStreamDefinitionExistException e){
                log.info("Defining the event stream because it was not found in BAM");
                try{
                    defineStream();
                } catch(Exception ex){
                    String msg = "Error occurred while defining the event stream for publishing Cloud Controller data. " + ex.getMessage();
                    log.error(msg, ex);
                    //We do not want to proceed without an event stream. Therefore we return.
                    release();
                    return;
                }
            }catch (Exception exc){
                log.error("Error occurred while searching for stream id. " + exc.getMessage(), exc);
                //We do not want to proceed without an event stream. Therefore we return.
                release();
                return;
            }
        }
        
        // build the new node - state Map
        Map<String, String> newNodeToStateMap;
        try{
            newNodeToStateMap = getNodeIdToStatusMap();
        }catch (Exception e) {

            release();
            throw new CloudControllerException(e.getMessage(), e);
            
        } 
        
        // compare it with old map and populate data to be published with ones newly added
        // and once whose state got changed
        populateNewlyAddedOrStateChangedNodes(newNodeToStateMap);
        
        // issue events for the ones obtained from above
        for (CartridgeInstanceData dataObj : dataToBePublished) {
            StringBuffer temp = new StringBuffer("");
            
            String privateIpAddresses="";
            // Concatenate private IP addresses
            for (String ip : dataObj.getMetaData().getPrivateAddresses()) {
                temp.append(ip+",");
            }
            
            if(!"".equals(temp.toString())){
                // remove comma at the end of the string
                privateIpAddresses = temp.toString().substring(0, temp.toString().length()-1);
            }
            
            temp = new StringBuffer("");
            String publicIpAddresses="";
            // Concatenate public IP addresses
            for (String ip : dataObj.getMetaData().getPublicAddresses()) {
                temp.append(ip+",");
            }
            
            if(!"".equals(temp.toString())){
                // remove comma at the end of the string
                publicIpAddresses = temp.toString().substring(0, temp.toString().length()-1);
            }
            
            try {

                Event cloudControllerEvent = new Event(streamId, System.currentTimeMillis(), new Object[]{}, null,
                                            new Object[]{dataObj.getNodeId(),
                                                         dataObj.getType(),
                                                         dataObj.getDomain(),
                                                         dataObj.getSubDomain(),
                                                         dataObj.getAlias(),
                                                         dataObj.getTenantRange(),
                                                         String.valueOf(dataObj.isMultiTenant()),
                                                         dataObj.getIaas(),
                                                         dataObj.getStatus(),
                                                         dataObj.getMetaData().getHostname(),
                                                         dataObj.getMetaData().getHardware().getHypervisor(),
                                                         String.valueOf(dataObj.getMetaData().getHardware().getRam()),
                                                         dataObj.getMetaData().getImageId(),
                                                         String.valueOf(dataObj.getMetaData().getLoginPort()),
                                                         dataObj.getMetaData().getOperatingSystem().getName(),
                                                         dataObj.getMetaData().getOperatingSystem().getVersion(),
                                                         dataObj.getMetaData().getOperatingSystem().getArch(),
                                                         String.valueOf(dataObj.getMetaData().getOperatingSystem().is64Bit()),
                                                         privateIpAddresses,
                                                         publicIpAddresses});

                dataPublisher.publish(cloudControllerEvent);
                
                log.debug("Data published : "+cloudControllerEvent.toString());

            } catch (Exception e) {
                String msg = "Error occurred while publishing Cartridge instance event to BAM. ";
                log.error(msg, e);
                release();
                throw new CloudControllerException(msg, e);
            }
            
        }
        
        // replace old map with new one only if data is published
        FasterLookUpDataHolder.getInstance().setNodeIdToStatusMap(newNodeToStateMap);
        
        //TODO remove
//        CassandraDataRetriever.init();
//        CassandraDataRetriever.connect();
//        HiveQueryExecutor hive = new HiveQueryExecutor();
//        hive.createHiveTable();
//        System.out.println("***********");
//        for (String str : hive.getRunningNodeIds()) {
//         
//            System.out.println(str);
//        }
//        System.out.println("***********");
        release();
    }
    
    private static void release(){
        FasterLookUpDataHolder.getInstance().setPublisherRunning(false);
    }
    
    private static void defineStream() throws Exception {
        streamId = dataPublisher.
                defineStream("{" +
                        "  'name':'" + CloudControllerConstants.CLOUD_CONTROLLER_EVENT_STREAM +"'," +
                        "  'version':'" + cloudControllerEventStreamVersion +"'," +
                        "  'nickName': 'cloud.controller'," +
                        "  'description': 'Instances booted up by the Cloud Controller '," +
                        "  'metaData':[]," +
                        "  'payloadData':[" +
                        "          {'name':'"+CloudControllerConstants.NODE_ID_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.CARTRIDGE_TYPE_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.DOMAIN_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.SUB_DOMAIN_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.ALIAS_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.TENANT_RANGE_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.IS_MULTI_TENANT_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.IAAS_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.STATUS_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.HOST_NAME_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.HYPERVISOR_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.RAM_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.IMAGE_ID_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.LOGIN_PORT_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.OS_NAME_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.OS_VERSION_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.OS_ARCH_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.OS_BIT_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.PRIV_IP_COL+"','type':'STRING'}," +
                        "          {'name':'"+CloudControllerConstants.PUB_IP_COL+"','type':'STRING'}" +
                        "  ]" +
                        "}");
        
    }

    @Override
    public void init() {

    	// this is a temporary fix to avoid task execution - limitation with ntask
		if(!FasterLookUpDataHolder.getInstance().getEnableBAMDataPublisher()){
			log.debug("BAM data publisher is disabled. ");
			return;
		}
		
        if((dataPublisher = FasterLookUpDataHolder.getInstance().getDataPublisher()) == null){
            createDataPublisher();
        }
        streamId = FasterLookUpDataHolder.getInstance().getStreamId();
        
    }

    @Override
    public void setProperties(Map<String, String> arg0) {}
    
    private static void createDataPublisher(){
        //creating the agent
        AgentConfiguration agentConfiguration = new AgentConfiguration();

        ServerConfiguration serverConfig =  CarbonUtils.getServerConfiguration();
        String trustStorePath = serverConfig.getFirstProperty("Security.TrustStore.Location");
        String trustStorePassword = serverConfig.getFirstProperty("Security.TrustStore.Password");
        String bamServerUrl = serverConfig.getFirstProperty("BamServerURL");
        String adminUsername = FasterLookUpDataHolder.getInstance().getBamUsername();
        String adminPassword = FasterLookUpDataHolder.getInstance().getBamPassword();

        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);

        Agent agent = new Agent(agentConfiguration);

        try {
            dataPublisher = new DataPublisher(bamServerUrl, adminUsername, adminPassword, agent);
            FasterLookUpDataHolder.getInstance().setDataPublisher(dataPublisher);
            
        } catch (Exception e) {
            String msg = "Unable to create a data publisher to " + bamServerUrl +
                    ". Usage Agent will not function properly. ";
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }
        
    }
    
    private static void bundleData(String key, String val, ServiceContext serviceCtxt) {
        
        CartridgeInstanceData instanceData = new CartridgeInstanceData();
        instanceData.setNodeId(key);
        instanceData.setStatus(val);
        instanceData.setDomain(serviceCtxt.getDomainName());
        instanceData.setSubDomain(serviceCtxt.getSubDomainName());
        instanceData.setAlias("".equals(serviceCtxt.getProperty(CloudControllerConstants.ALIAS_PROPERTY))
            ? "NULL"
                : serviceCtxt.getProperty(CloudControllerConstants.ALIAS_PROPERTY));
        instanceData.setTenantRange("".equals(serviceCtxt.getProperty(CloudControllerConstants.TENANT_ID_PROPERTY))
            ? serviceCtxt.getTenantRange()
                : serviceCtxt.getProperty(CloudControllerConstants.TENANT_ID_PROPERTY));
        
        if (serviceCtxt.getCartridge() != null) {
            instanceData.setMultiTenant(serviceCtxt.getCartridge().isMultiTenant());

            for (IaasProvider iaas : serviceCtxt.getCartridge().getIaases()) {

                IaasContext ctxt = null;
                if ((ctxt = serviceCtxt.getIaasContext(iaas.getType())) == null) {
                    ctxt = serviceCtxt.addIaasContext(iaas.getType());
                }

                if (ctxt.didISpawn(key)) {
                    instanceData.setIaas(iaas.getType());
                    instanceData.setMetaData(ctxt.getNode(key));

                    // clear to be removed data
                    ctxt.removeToBeRemovedNodeId(key);

                    // if the node is terminated
                    if (val.equals(NodeStatus.TERMINATED.toString())) {
                        // since this node is terminated
                        FasterLookUpDataHolder.getInstance().removeNodeId(key);

                        // remove node meta data
                        ctxt.removeNodeMetadata(ctxt.getNode(key));
                    }

                    break;
                }
            }

            instanceData.setType(serviceCtxt.getCartridge().getType());
        } else {
            log.warn("Cartridge is null for Service Context : (domain: " +
                serviceCtxt.getDomainName() +
                    ", sub domain: " +
                    serviceCtxt.getSubDomainName() +
                    ")");
        }
        
        dataToBePublished.add(instanceData);
        
    }
    
    private static Map<String, String> getNodeIdToStatusMap() throws Exception {
        
        Map<String, String> statusMap = new HashMap<String, String>();
        
        // iterate through all ServiceContexts
        for (Iterator<?> it1 = FasterLookUpDataHolder.getInstance().getServiceContexts().entrySet().iterator(); it1.hasNext();) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, Map<String, ServiceContext>> entry = (Map.Entry<String, Map<String, ServiceContext>>) it1.next();
            
            Map<String, ServiceContext> map = (Map<String, ServiceContext>) entry.getValue();
            
            for (Iterator<ServiceContext> it2 = map.values().iterator(); it2.hasNext();) {
                ServiceContext subjectedSerCtxt = (ServiceContext) it2.next();
                
                if (subjectedSerCtxt != null && subjectedSerCtxt.getCartridge() != null) {
                    List<IaasProvider> iaases = subjectedSerCtxt.getCartridge().getIaases();

                    for (IaasProvider iaas : iaases) {

                        ComputeService computeService = iaas.getComputeService();
                        
                        if(computeService == null){
                            continue;
                        }
                        
                        IaasContext ctxt = null;
                        if((ctxt = subjectedSerCtxt.getIaasContext(iaas.getType())) == null){
                        	ctxt = subjectedSerCtxt.addIaasContext(iaas.getType());
                        }

                        // get list of node Ids
                        List<String> nodeIds = ctxt.getAllNodeIds();

                        if (nodeIds.isEmpty()) {
                            
                            continue;
                        }
                        
                        try {

                            // get all the nodes spawned by this IaasContext
                            Set<? extends ComputeMetadata> set = computeService.listNodes();

                            Iterator<? extends ComputeMetadata> iterator = set.iterator();

                            // traverse through all nodes of this ComputeService object
                            while (iterator.hasNext()) {
                                NodeMetadata nodeMetadata = (NodeMetadataImpl) iterator.next();

                                // if this node belongs to the requested domain
                                if (nodeIds.contains(nodeMetadata.getId())) {

                                    statusMap.put(nodeMetadata.getId(), nodeMetadata.getStatus()
                                                                                    .toString());

                                    ctxt.addNodeMetadata(nodeMetadata);
                                }

                            }

                        }catch (Exception e) {
                            log.error(e.getMessage(), e);
                            throw e;
                        }

                    }
                }
            }
            
            
        }
        return statusMap;

    }
    
    private static void populateNewlyAddedOrStateChangedNodes(Map<String, String> newMap){
        
        MapDifference<String, String> diff = Maps.difference(newMap, 
                                                             FasterLookUpDataHolder.getInstance().getNodeIdToStatusMap());
        
        // adding newly added nodes
        Map<String, String> newlyAddedNodes = diff.entriesOnlyOnLeft();
        
        for (Iterator<?> it = newlyAddedNodes.entrySet().iterator(); it.hasNext();) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, String> entry = (Map.Entry<String, String>) it.next();
            String key = entry.getKey();
            String val = entry.getValue();
            ServiceContext ctxt = FasterLookUpDataHolder.getInstance().getServiceContext(key);
            
            log.debug("------ Node id: "+key+" --- node status: "+val+" -------- ctxt: "+ctxt);
            
            if (ctxt != null && key != null && val != null) {
                // bundle the data to be published
                bundleData(key, val, ctxt);
            }   
                    
        }
        
        // adding nodes with state changes
        Map<String, ValueDifference<String>> stateChangedNodes = diff.entriesDiffering();
        
        for (Iterator<?> it = stateChangedNodes.entrySet().iterator(); it.hasNext();) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, ValueDifference<String>> entry = (Map.Entry<String, ValueDifference<String>>) it.next();
            
            String key = entry.getKey();
            String newState = entry.getValue().leftValue();
            ServiceContext ctxt = FasterLookUpDataHolder.getInstance().getServiceContext(key);
            
            log.debug("------- Node id: "+key+" --- node status: "+newState+" -------- ctxt: "+ctxt);
            
            if (ctxt != null && key != null && newState != null) {
                // bundle the data to be published
                bundleData(key, newState, ctxt);
            }  
            
        }

    }
    
}
