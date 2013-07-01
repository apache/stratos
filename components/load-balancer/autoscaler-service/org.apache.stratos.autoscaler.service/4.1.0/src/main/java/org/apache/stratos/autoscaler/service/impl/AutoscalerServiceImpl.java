/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.jclouds.aws.ec2.compute.AWSEC2TemplateOptions;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadata.Status;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.domain.internal.NodeMetadataImpl;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.apache.stratos.autoscaler.service.IAutoscalerService;
import org.apache.stratos.autoscaler.service.exception.AutoscalerServiceException;
import org.apache.stratos.autoscaler.service.exception.DeserializationException;
import org.apache.stratos.autoscaler.service.exception.SerializationException;
import org.apache.stratos.autoscaler.service.io.Deserializer;
import org.apache.stratos.autoscaler.service.io.Serializer;
import org.apache.stratos.autoscaler.service.jcloud.ComputeServiceBuilder;
import org.apache.stratos.autoscaler.service.util.AutoscalerConstant;
import org.apache.stratos.autoscaler.service.util.IaasContext;
import org.apache.stratos.autoscaler.service.util.IaasProvider;
import org.apache.stratos.autoscaler.service.util.InstanceContext;
import org.apache.stratos.autoscaler.service.util.ServiceTemplate;
import org.apache.stratos.autoscaler.service.xml.ElasticScalerConfigFileReader;
import org.apache.stratos.lb.common.conf.util.Constants;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Autoscaler Service is responsible for starting up new server instances, terminating already
 * started instances, providing pending instance count.
 * 
 */
public class AutoscalerServiceImpl implements IAutoscalerService {

	
	private static final Log log = LogFactory.getLog(AutoscalerServiceImpl.class);

    /**
     * pointer to Carbon Home directory.
     */
    private static final String CARBON_HOME = CarbonUtils.getCarbonHome();

    /**
     * pointer to Carbon Temp directory.
     */
    private static final String CARBON_TEMP = CarbonUtils.getTmpDir();

    /**
     * Tenant id Delimiter
     */
	private static final String TENANT_ID_DELIMITER = "/t/";

    /**
     * This directory will be used to store serialized objects.
     */
    private String serializationDir;

    /**
     * List of all <code>IaaSProviders</code> specified in the config file.
     */
    private List<IaasProvider> iaasProviders;

    /**
     * List of all <code>ServiceTemplate</code> objects.
     */
    private List<ServiceTemplate> serviceTemps;

    /**
     * We keep an enum which contains all supported IaaSes.
     */
    public enum Iaases {
        ec2, openstack
    };

    /**
     * List which keeps <code>IaasContext</code> objects.
     */
    private List<IaasContext> iaasContextList = new ArrayList<IaasContext>();

    /**
     * We keep track of the IaaS where the last instance of a domain and a sub domain combination is spawned.
     * This is required when there are multiple <code>IaasProvider</code>s defined.
     * Key - domain
     * Value - a map which has a Key - sub domain and Value - name of the {@link IaasProvider}.
     */
    private Map<String, Map<String, String>> lastlyUsedIaasMap = new HashMap<String, Map<String, String>>();

    /**
     * To track whether the {@link #initAutoscaler(boolean)} method has been called.
     */
    boolean isInitialized = false;

    @Override
    public boolean initAutoscaler(boolean isSpi) {

        if (!isInitialized) {

            log.debug("InitAutoscaler has started ...  IsSPI : " + isSpi);

            // load configuration file
            ElasticScalerConfigFileReader configReader = new ElasticScalerConfigFileReader();

            // read serialization directory from config file if specified, else will use the
            // default.
            if ("".equals(serializationDir = configReader.getSerializationDir())) {
                serializationDir = CARBON_TEMP;

                log.debug("Directory to be used to serialize objects: " + serializationDir);
            }

            // let's deserialize and load the serialized objects.
            deserialize();

            // from config file, we grab the details unique to IaaS providers.
            iaasProviders = configReader.getIaasProvidersList();

            // from config file, we grab the details related to each service domain.
            serviceTemps = configReader.getTemplates();

            // we iterate through each IaaSProvider which is loaded from the config file.
            for (IaasProvider iaas : iaasProviders) {

                // build the JClouds specific ComputeService object
                ComputeService computeService = ComputeServiceBuilder.buildComputeService(iaas);
                IaasContext entity;

                // let's see whether there's a serialized entity
                entity = findIaasContext(iaas.getType());

                if (entity != null) {

                    log.debug("Serializable object is loaded for IaaS " + iaas.getType());

                    // ComputeService isn't serializable, hence we need to set it in the
                    // deserialized
                    // object.
                    entity.setComputeService(computeService);
                }

                // build JClouds Template objects according to different IaaSes
                if (iaas.getType().equalsIgnoreCase(Iaases.ec2.toString())) {

                    // initiate the IaasContext object, if it is null.
                    entity =
                             (entity == null) ? (entity =
                                                          new IaasContext(Iaases.ec2,
                                                                          computeService)) : entity;

                    // we should build the templates only if this is not SPI stuff
                    if (!isSpi) {
                        // Build the Template
                        buildEC2Templates(entity, iaas.getTemplate(), isSpi);

                    } else {
                        // add to data structure
                        iaasContextList.add(entity);
                    }

                } else if (iaas.getType().equalsIgnoreCase(Iaases.openstack.toString())) {

                    // initiate the IaasContext object, if it is null.
                    entity =
                             (entity == null) ? (entity =
                                                          new IaasContext(Iaases.openstack,
                                                                          computeService)) : entity;

                    // we should build the templates only if this is not SPI stuff
                    if (!isSpi) {
                        // Build the Template
                        buildLXCTemplates(entity, iaas.getTemplate(), isSpi, null);

                    } else {
                        // add to data structure
                        iaasContextList.add(entity);
                    }

                } else {
                    // unsupported IaaS detected. We only complain, since there could be other
                    // IaaSes.
                    String msg =
                                 "Unsupported IaasProvider is specified in the config file: " +
                                         iaas.getType() + ". Supported IaasProviders are " +
                                         print(Iaases.values());
                    log.warn(msg);
                    continue;
                }

                // populate scale up order
                fillInScaleUpOrder();

                // populate scale down order
                fillInScaleDownOrder();

                // serialize the objects
                serialize();
            }

            // we couldn't locate any valid IaaS providers from config file, thus shouldn't proceed.
            if (iaasContextList.size() == 0) {
                String msg = "No valid IaaS provider specified in the config file!";
                log.error(msg);
                throw new AutoscalerServiceException(msg);
            }

            // initialization completed.
            isInitialized = true;

            log.info("Autoscaler service initialized successfully!!");

            return true;
        }
        
        log.debug("Autoscaler Service is already initialized!");
        return false;
    }

    
    @Override
    public boolean startInstance(String domainName, String subDomainName) {

        // initialize the service, if it's not already initialized.
        initAutoscaler(false);

        ComputeService computeService;
        Template template;
        
        subDomainName = checkSubDomain(subDomainName);

        log.info("Starting new instance of domain : " + domainName+" and sub domain : "+subDomainName);

        // sort the IaasContext entities according to scale up order
        Collections.sort(iaasContextList,
                         IaasContextComparator.ascending(IaasContextComparator.getComparator(IaasContextComparator.SCALE_UP_SORT)));

        // traverse through IaasContext object instances in scale up order
        for (IaasContext iaasCtxt : iaasContextList) {

            // get the ComputeService
            computeService = iaasCtxt.getComputeService();

            // from the list grab the Template corresponds to this domain
            template = iaasCtxt.getTemplate(domainName, subDomainName);
            
            if(template == null){
                String msg = "Failed to start an instance in " + iaasCtxt.getName().toString() +
                        ". Reason : Template is null. You have not specify a matching service " +
                        "element in the configuration file of Autoscaler.\n Hence, will try to " +
                        "start in another IaaS if available.";
                log.error(msg);
                continue;
            }

            // generate the group id from domain name and sub domain name.
            // Should have lower-case ASCII letters, numbers, or dashes.
            String str = domainName.concat("-"+subDomainName);
            String group = str.replaceAll("[^a-z0-9-]", "");

            try {
                // create and start a node
                Set<? extends NodeMetadata> nodes = computeService.createNodesInGroup(group,
                                                                                      1,
                                                                                      template);

                NodeMetadata node = nodes.iterator().next();

//                // add the details of the started node to maps
//                iaasCtxt.addNodeIdToDomainMap(node.getId(), domainName);
                
                String ip="";

                // set public Ip, if it's available
                if (node.getPublicAddresses().size() > 0) {
                    ip = node.getPublicAddresses().iterator().next();
//                    iaasCtxt.addPublicIpToDomainMap(publicIp, domainName);
//                    iaasCtxt.addPublicIpToNodeIdMap(publicIp, node.getId());
                } else if(node.getPrivateAddresses().size() > 0) { // set private IPs if no public IP s are returned
                	ip = node.getPrivateAddresses().iterator().next();
//                    iaasCtxt.addPublicIpToDomainMap(privateIp, domainName);
//                    iaasCtxt.addPublicIpToNodeIdMap(privateIp, node.getId());
                } else{
                    log.debug("Public IP of the node : "+node.getId()+" cannot be found.");
                }
                
        
                if (iaasCtxt.getInstanceContext(domainName, subDomainName) == null) {
                    String msg = "Failed to start an instance in " + iaasCtxt.getName().toString() +
                            ". Reason : InstanceContext is null. Hence, will try to start in another IaaS if available.";
                    log.error(msg);
                    continue;
                }
                
                if(node.getId() == null){
                    String msg = "Node id of the starting instance is null.\n"+ node.toString();
                    log.fatal(msg);
                    throw new AutoscalerServiceException(msg);
                }

                // add run time data to InstanceContext
                iaasCtxt.addNodeDetails(domainName, subDomainName, node.getId(), ip);
                
                // since we modified the IaasContext instance, let's replace it.
                replaceIaasContext(iaasCtxt);

                // update the lastlyUsedIaasMap
                addToLastlyUsedIaasMap(domainName, subDomainName, iaasCtxt.getName().toString());
//                lastlyUsedIaasMap.put(domainName, iaasCtxt.getName().toString());

                if (log.isDebugEnabled()) {
                    log.debug("Node details: \n" + node.toString() +
                              "\n***************\n");
                }

            } catch (RunNodesException e) {
                log.warn("Failed to start an instance in " + iaasCtxt.getName().toString() +
                         ". Hence, will try to start in another IaaS if available.", e);
                continue;
            }

            log.info("Instance is successfully starting up in IaaS " + iaasCtxt.getName()
                                                                               .toString() + " ...");

            // serialize the objects
            serialize();

            return true;
        }

        log.info("Failed to start instance, in any available IaaS.");

        return false;

    }

    private String checkSubDomain(String subDomainName) {
        // if sub domain is null, we assume it as default one.
        if (subDomainName == null || "null".equalsIgnoreCase(subDomainName)) {
            subDomainName = Constants.DEFAULT_SUB_DOMAIN;
            log.debug("Sub domain is null, hence using the default value : " + subDomainName);
        }
        
        return subDomainName;
    }


    private void addToLastlyUsedIaasMap(String domainName, String subDomainName, String iaasName) {

        Map<String, String> map;
        
        if(lastlyUsedIaasMap.get(domainName) == null){
            map = new HashMap<String, String>();
            
        } else{
            map = lastlyUsedIaasMap.get(domainName);
        }
        
        map.put(subDomainName, iaasName);
        lastlyUsedIaasMap.put(domainName, map);
        
    }


    @Override
    public String startSpiInstance(String domainName, String subDomainName, String imageId) {

        log.debug("Starting an SPI instance ... | domain: " + domainName + " | sub domain: " +
        subDomainName + " | ImageId: " + imageId);
        
        // initialize the service, if it's not already initialized.
        initAutoscaler(true);
        
        String tenantId = null;
        String spiDomainName = null;       
        
        if(domainName != null) {
        	// domainName will have the pattern <domainName>/t/<tenantId>
        	String[] arr = domainName.split(TENANT_ID_DELIMITER);
        	if(arr.length != 2) {
        		String msg = "Domain name does not match with the expected pattern. Expected " +
        				"pattern is <domainName>/t/<tenantId>";
        		log.error(msg);
        		throw new AutoscalerServiceException(msg);
        	}
        	spiDomainName = arr[0];
        	tenantId = arr[1];
        }
        
        IaasContext entry;

        // FIXME: Build the Templates, for now we're doing a hack here. I don't know whether
        // there's a proper fix.
        // handle openstack case
        if (imageId.startsWith("nova") && ((entry = findIaasContext(Iaases.openstack)) != null)) {

            buildLXCTemplates(entry, imageId, true, tenantId);

        } else if (((entry = findIaasContext(Iaases.ec2)) != null)) {

            buildEC2Templates(entry, imageId, true);

        } else {
            String msg = "Invalid image id: " + imageId;
            log.error(msg);
            throw new AutoscalerServiceException(msg);
        }

        // let's start the instance
        if (startInstance(spiDomainName, subDomainName)) {

            // if it's successful, get the public IP of the started instance.
            // FIXME remove --> String publicIP =
            // findIaasContext(iaas).getLastMatchingPublicIp(domainName);
            String publicIP = entry.getLastMatchingPublicIp(spiDomainName, subDomainName);

            // if public IP is null, return an empty string, else return public IP.
            return (publicIP == null) ? "" : publicIP;

        }

        return "";

    }

	@Override
    public boolean terminateInstance(String domainName, String subDomainName) {

        // initialize the service, if it's not already initialized.
	    initAutoscaler(false);
        
        subDomainName = checkSubDomain(subDomainName);

        log.info("Starting to terminate an instance of domain : " + domainName + " and sub domain : "+subDomainName);

        // sort the IaasContext entities according to scale down order.
        Collections.sort(iaasContextList,
                         IaasContextComparator.ascending(IaasContextComparator.getComparator(IaasContextComparator.SCALE_DOWN_SORT)));

        // traverse in scale down order
        for (IaasContext iaasTemp : iaasContextList) {

            String msg = "Failed to terminate an instance in " + iaasTemp.getName().toString() +
                         ". Hence, will try to terminate an instance in another IaaS if possible.";

            String nodeId = null;

            // grab the node ids related to the given domain and traverse
            for (String id : iaasTemp.getNodeIds(domainName, subDomainName)) {
                if (id != null) {
                    nodeId = id;
                    break;
                }
            }

            // if no matching node id can be found.
            if (nodeId == null) {

                log.warn(msg + " : Reason- No matching instance found for domain: " +
                         domainName + " and sub domain: "+subDomainName+
                         ".");
                continue;
            }

            // terminate it!
            terminate(iaasTemp, nodeId);

            return true;

        }

        log.info("Termination of an instance which is belong to domain '" + domainName +
                 "' and sub domain '"+subDomainName+"' , failed!\n Reason: No matching " +
                 		"running instance found in any available IaaS.");

        return false;

    }

    @Override
    public boolean terminateLastlySpawnedInstance(String domainName, String subDomainName) {

        // initialize the service, if it's not already initialized.
        initAutoscaler(false);
        
        subDomainName = checkSubDomain(subDomainName);

        // see whether there is a matching IaaS, where we spawn an instance belongs to given domain.
        if (lastlyUsedIaasMap.containsKey(domainName)) {

            // grab the name of the IaaS
            String iaas = lastlyUsedIaasMap.get(domainName).get(subDomainName);

            // find the corresponding IaasContext
            IaasContext iaasTemp = findIaasContext(iaas);

            String msg = "Failed to terminate the lastly spawned instance of '" + domainName +
                         "' service domain.";

            if (iaasTemp == null) {
                log.error(msg + " : Reason- Iaas' data cannot be located!");
                return false;
            }

            // find the instance spawned at last of this IaasContext
            String nodeId = iaasTemp.getLastMatchingNode(domainName, subDomainName);

            if (nodeId == null) {
                log.error(msg + " : Reason- No matching instance found for domain: " +
                        domainName + " and sub domain: "+subDomainName+
                        ".");
                return false;
            }

            // terminate it!
            terminate(iaasTemp, nodeId);

            return true;

        }

        log.info("Termination of an instance which is belong to domain '" + domainName +
                 "' and sub domain '"+subDomainName+"' , failed!\n Reason: No matching instance found.");

        return false;
    }

    @Override
    public boolean terminateSpiInstance(String publicIp) {

        // initialize the service, if it's not already initialized.
        initAutoscaler(true);

        // sort the IaasContext entities according to scale down order.
        Collections.sort(iaasContextList,
                         IaasContextComparator.ascending(IaasContextComparator.getComparator(IaasContextComparator.SCALE_DOWN_SORT)));

        // traverse in scale down order
        for (IaasContext iaasTemp : iaasContextList) {

            String msg = "Failed to terminate an instance in " + iaasTemp.getName().toString() +
                         "" +
                         ". Hence, will try to terminate an instance in another IaaS if possible.";

            // grab the node maps with the given public IP address
            String nodeId = iaasTemp.getNodeWithPublicIp(publicIp);

            if (nodeId == null) {
                log.warn(msg + " : Reason- No matching instance found for public ip '" +
                         publicIp +
                         "'.");
                continue;
            }

            // terminate it!
            terminate(iaasTemp, nodeId);

            return true;
        }

        log.info("Termination of an instance which has the public IP '" + publicIp + "', failed!");

        return false;
    }

    @Override
    public int getPendingInstanceCount(String domainName, String subDomain) {

        // initialize the service, if it's not already initialized.
        initAutoscaler(false);
        
        subDomain = checkSubDomain(subDomain);

        int pendingInstanceCount = 0;

        // traverse through IaasContexts
        for (IaasContext entry : iaasContextList) {

            ComputeService computeService = entry.getComputeService();

            // get list of node Ids which are belong to the requested domain
            List<String> nodeIds = entry.getNodeIds(domainName, subDomain);
            
            if(nodeIds.isEmpty()){
                log.debug("Zero nodes spawned in the IaaS "+entry.getName()+
                          " of domain: "+domainName+" and sub domain: "+subDomain);
                continue;
            }

            // get all the nodes spawned by this IaasContext
            Set<? extends ComputeMetadata> set = computeService.listNodes();

            Iterator<? extends ComputeMetadata> iterator = set.iterator();

            // traverse through all nodes of this ComputeService object
            while (iterator.hasNext()) {
                NodeMetadataImpl nodeMetadata = (NodeMetadataImpl) iterator.next();

                // if this node belongs to the requested domain
                if (nodeIds.contains(nodeMetadata.getId())) {

                    // get the status of the node
                    Status nodeStatus = nodeMetadata.getStatus();

                    // count nodes that are in pending state
                    if (nodeStatus.toString().equalsIgnoreCase("PENDING")) {
                        pendingInstanceCount++;
                    }
                }

            }
        }

        log.info("Pending instance count of domain '" + domainName + "' and sub domain '"+
        subDomain+"' is " + pendingInstanceCount);

        return pendingInstanceCount;

    }

    /**
     * Returns matching IaasContext for the given {@link Iaases} entry.
     */
    private IaasContext findIaasContext(Enum<Iaases> iaas) {

        for (IaasContext entry : iaasContextList) {
            if (entry.getName().equals(iaas)) {
                return entry;
            }
        }

        return null;
    }

    /**
     * Returns matching IaasContext for the given iaas type.
     */
    private IaasContext findIaasContext(String iaasType) {

        for (IaasContext entry : iaasContextList) {
            if (entry.getName().toString().equals(iaasType)) {
                return entry;	
            }
        }

        return null;
    }

    private void fillInScaleDownOrder() {

        for (IaasProvider iaas : iaasProviders) {
            if (findIaasContext(iaas.getType()) != null) {
                findIaasContext(iaas.getType()).setScaleDownOrder(iaas.getScaleDownOrder());
            }
        }

    }

    private void fillInScaleUpOrder() {

        for (IaasProvider iaas : iaasProviders) {
            if (findIaasContext(iaas.getType()) != null) {
                findIaasContext(iaas.getType()).setScaleUpOrder(iaas.getScaleUpOrder());
            }
        }

    }

    private byte[] getUserData(String payloadFileName, String tenantId) {
 
    	byte[] bytes = null;
    	File outputFile = null;
    	String tempfilename = UUID.randomUUID().toString();
        try {
            File file = new File(payloadFileName);
            if (!file.exists()) {
                handleException("Payload file " + payloadFileName + " does not exist");
            }
            if (!file.canRead()) {
                handleException("Payload file " + payloadFileName + " does cannot be read");
            }
            if(tenantId != null) {
            	// Tenant Id is available. This is an spi scenario. Edit the payload content
            	editPayload(tenantId,file,tempfilename);
            	outputFile = new File(CARBON_HOME + File.separator + AutoscalerConstant.RESOURCES_DIR + File.separator + tempfilename+".zip");
            } else {
            	outputFile = file;
            }
            bytes = getBytesFromFile(outputFile);

        } catch (IOException e) {
            handleException("Cannot read data from payload file " + payloadFileName, e);
        }
        
        // Remove temporary payload file
        if (tenantId != null) {
            outputFile.delete();
        }
        
        return bytes;
    }

    
    private void editPayload(String tenantName, File file, String tempfileName) {
		
    	unzipFile(file, tempfileName);
    	editContent(tenantName, file, tempfileName);
    	zipPayloadFile(tempfileName);    	
	}

    
    /**
	 * unzips the payload file
	 * 
	 * @param file
     * @param tempfileName 
	 */
	private void unzipFile(File file, String tempfileName) {
		
		int buffer = 2048;
		BufferedOutputStream dest = null;
		ZipInputStream zis = null;
		
		try {
			FileInputStream fis = new FileInputStream(file);
			zis = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry;
			
			while ((entry = zis.getNextEntry()) != null) {

				log.debug("Extracting: " + entry);
				
				int count;
				byte data[] = new byte[buffer];
				String outputFilename = tempfileName + File.separator + entry.getName();
				createDirIfNeeded(tempfileName, entry);

				// write the files to the disk
				if (!entry.isDirectory()) {
					FileOutputStream fos = new FileOutputStream(outputFilename);
					dest = new BufferedOutputStream(fos, buffer);
					while ((count = zis.read(data, 0, buffer)) != -1) {
						dest.write(data, 0, count);
					}
					dest.flush();
					dest.close();
				}
			}
			
		} catch (Exception e) {
			log.error("Exception is occurred in unzipping payload file. Reason:" + e.getMessage());
			throw new AutoscalerServiceException(e.getMessage(), e);
		} finally {
			closeStream(zis);			
			closeStream(dest);
		}
	}


	private void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				log.error(" Exception is occurred when closing stream. Reason :" + e.getMessage());
			}
		}
	}

	/**
	 * 
	 * Modify contents (tenantName) of the debian_cron_script.sh file
	 * 
	 * @param tenantName
	 * @param file
	 */
	private void editContent(String tenantName, File file, String tempfileName) {

		File f = new File(CARBON_HOME + File.separator + tempfileName
				+ File.separator + AutoscalerConstant.PAYLOAD_DIR + File.separator
				+ AutoscalerConstant.PARAMS_FILE_NAME);

		FileInputStream fs = null;
		InputStreamReader in = null;
		BufferedReader br = null;

		StringBuffer sb = new StringBuffer();

		String textinLine;

		try {
			fs = new FileInputStream(f);
			in = new InputStreamReader(fs);
			br = new BufferedReader(in);

			while (true) {
				
				textinLine=br.readLine();
	            if(textinLine==null)
	                break;
	            sb.append(editLine(textinLine, tenantName));
			}
		} catch (Exception e) {
			log.error("Exception is occurred in editing payload content. Reason: "+e.getMessage());
			throw new AutoscalerServiceException(e.getMessage(), e);
		} finally {
			closeStream(fs);
			closeStream(in);
			closeStream(br);
		}

		writeChangesBackToFile(f, sb);
	}


	private String editLine(String textinLine, String tenantName) {
		
		// Format of the line will be <IP>=<IPvalue>,<Path>=<PathValue>..
		
		StringBuffer outputBuffer = new StringBuffer();
		Map<String, String> paramMap = new HashMap<String, String>();
		String[] params = textinLine.split(AutoscalerConstant.ENTRY_SEPARATOR);

		for (int i = 0; i < params.length; i++) {

			// split the params one by one
			String param = params[i];
			String[] values = param.split(AutoscalerConstant.VALUE_SEPARATOR);
			
			if(values.length != 2) {
				throw new AutoscalerServiceException("Incorrect format in parameters file");
			}
			
			String key = values[0];
			String value = values[1];

			String updatedValue = value;

			if (AutoscalerConstant.TENANT_KEY.equals(key)) {
				updatedValue = tenantName;
			} else if (AutoscalerConstant.APP_PATH_KEY.equals(key)) {
				updatedValue = getAppPathForTenant(tenantName,value);
			} 
			paramMap.put(key, updatedValue);
		}

		// Loop through the map and put values into a string
		reOrganizeContent(outputBuffer, paramMap);

		// cleanup output buffer
		if (outputBuffer.substring(0, 1).equals(AutoscalerConstant.ENTRY_SEPARATOR)) {
			outputBuffer.delete(0, 1);
		}

		return outputBuffer.toString();
	}


	private void reOrganizeContent(StringBuffer outputBuffer, Map<String, String> paramMap) {
		
		for (Map.Entry<String, String> entry : paramMap.entrySet()) {
			outputBuffer.append(AutoscalerConstant.ENTRY_SEPARATOR).append(entry.getKey()).append(AutoscalerConstant.VALUE_SEPARATOR)
																		.append(entry.getValue());
		}
	}


	private String getAppPathForTenant(String tenantName, String appPath) {
		// Assumes app path is /opt/wso2-app/repository/
		StringBuffer updatedAppPath = new StringBuffer();
		if(tenantName.equals(AutoscalerConstant.SUPER_TENANT_ID)){ 
			updatedAppPath.append(appPath).append("deployment").append(File.separator).append("server")
															   .append(File.separator).append("phpapps");
		}else{
			updatedAppPath.append(appPath).append(tenantName).append(File.separator).append("phpapps");
		}
		return updatedAppPath.toString();
	}


	private void writeChangesBackToFile(File f, StringBuffer sb) {
		FileWriter fstream = null;
		BufferedWriter outobj = null;
		
		try {
			fstream = new FileWriter(f);
			outobj = new BufferedWriter(fstream);
			outobj.write(sb.toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			closeStream(outobj);
		}
	}
	
	/**
	 * 
	 * Compress the modified files back into payload.zip
	 * @param tempfileName 
	 * 
	 */
	private void zipPayloadFile(String tempfileName) {
		
		int buffer = 2048;
		BufferedInputStream origin = null;
		ZipOutputStream out = null;
		
		try {
	         
	         FileOutputStream dest = new 
	           FileOutputStream(CARBON_HOME + File.separator + AutoscalerConstant.RESOURCES_DIR + File.separator + tempfileName+".zip");
	         out = new ZipOutputStream(new BufferedOutputStream(dest));
	         byte data[] = new byte[buffer];
	         
	         File f = new File(CARBON_HOME + File.separator + tempfileName + File.separator + AutoscalerConstant.PAYLOAD_DIR);
	         String files[] = f.list();

	         for (int i=0; i<files.length; i++) {
	            FileInputStream fi = new FileInputStream(CARBON_HOME + File.separator + tempfileName
		 				+ File.separator + AutoscalerConstant.PAYLOAD_DIR + File.separator + files[i]);
	            origin = new BufferedInputStream(fi, buffer);
	            ZipEntry entry = new ZipEntry(AutoscalerConstant.PAYLOAD_DIR + File.separator + files[i]);
	            out.putNextEntry(entry);
	            
	            int count;
	            while((count = origin.read(data, 0, buffer)) != -1) {
	               out.write(data, 0, count);
	            }
	            origin.close();
	         }
	         
	         // delete temp files
	         deleteDir(f);
	         File fl = new File(CARBON_HOME + File.separator + tempfileName);
	         fl.delete();
	         
	      } catch(Exception e) {
	    	  log.error("Exception is occurred in zipping payload file after modification. Reason:" + e.getMessage());
	    	  throw new AutoscalerServiceException(e.getMessage(),e);
	      } finally {
	    	  closeStream(origin);	    	  
	    	  closeStream(out);
	      }
	}	

	private static boolean deleteDir(File dir) {
	    if (dir.isDirectory()) {
	        String[] children = dir.list();
	        for (int i=0; i<children.length; i++) {
	            boolean success = deleteDir(new File(dir, children[i]));
	            if (!success) {
	                return false;
	            }
	        }
	    }

	    // The directory is now empty so delete it
	    return dir.delete();
	}

	private void createDirIfNeeded(String destDirectory, ZipEntry entry) {
		
		String name = entry.getName();

        if(name.contains("/"))
        {
            log.debug("directory will need to be created");

            int index = name.lastIndexOf("/");
            String dirSequence = name.substring(0, index);

            File newDirs = new File(destDirectory + File.separator + dirSequence);

            //create the directory
            newDirs.mkdirs();
        }
		
	}


	/**
     * Returns the contents of the file in a byte array
     * 
     * @param file
     *            - Input File
     * @return Bytes from the file
     * @throws java.io.IOException
     *             , if retrieving the file contents failed.
     */
    private byte[] getBytesFromFile(File file) throws IOException {
        if (!file.exists()) {
            log.error("Payload file " + file.getAbsolutePath() + " does not exist");
            return null;
        }
        InputStream is = new FileInputStream(file);
        byte[] bytes;

        try {
            // Get the size of the file
            long length = file.length();

            // You cannot create an array using a long type.
            // It needs to be an int type.
            // Before converting to an int type, check
            // to ensure that file is not larger than Integer.MAX_VALUE.
            if (length > Integer.MAX_VALUE) {
                if (log.isDebugEnabled()) {
                    log.debug("File is too large");
                }
            }

            // Create the byte array to hold the data
            bytes = new byte[(int) length];

            // Read in the bytes
            int offset = 0;
            int numRead;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            // Ensure all the bytes have been read in
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
        } finally {
            // Close the input stream and return bytes
            is.close();
        }

        return bytes;
    }

    /**
     * handles the exception
     * 
     * @param msg
     *            exception message
     */
    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * handles the exception
     * 
     * @param msg
     *            exception message
     * @param e
     *            exception
     */
    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }


    /**
     * This will replace an existing entry in iaasEntities list, if there's such.
     * If not this will add the replacement value to the list.
     * 
     * @param replacement
     *            IaasContext entry to be added.
     */
    private void replaceIaasContext(IaasContext replacement) {
        for (IaasContext entry : iaasContextList) {
            if (entry.equals(replacement)) {
                int idx = iaasContextList.indexOf(entry);
                iaasContextList.remove(idx);
                iaasContextList.add(idx, replacement);
                return;
            }
        }
        iaasContextList.add(replacement);

    }

    /**
     * Builds the LXC Template object.
     */
    private void buildLXCTemplates(IaasContext entity, String imageId, boolean blockUntilRunning, String tenantId) {

        if (entity.getComputeService() == null) {
            throw new AutoscalerServiceException("Compute service is null for IaaS provider: " + entity.getName());
        }

//        // if domain to template map is null
//        if (entity.getDomainToTemplateMap() == null) {
//            // we initialize it
//            entity.setDomainToTemplateMap(new HashMap<String, Template>());
//        }

        TemplateBuilder templateBuilder = entity.getComputeService().templateBuilder();
        templateBuilder.imageId(imageId);

        // to avoid creation of template objects in each and every time, we create all
        // at once!
        for (ServiceTemplate temp : serviceTemps) {

            String instanceType;

            // set instance type 
            if (((instanceType = temp.getProperty("instanceType." + Iaases.openstack.toString())) != null) ) {

                templateBuilder.hardwareId(instanceType);
            }

            Template template = templateBuilder.build();

            template.getOptions().as(TemplateOptions.class).blockUntilRunning(blockUntilRunning);

            template.getOptions()
                    .as(NovaTemplateOptions.class)
                    .securityGroupNames(temp.getProperty("securityGroups").split(AutoscalerConstant.ENTRY_SEPARATOR));

            if (temp.getProperty(AutoscalerConstant.PAYLOAD_DIR) != null) {
                template.getOptions()
                        .as(NovaTemplateOptions.class)
                        .userData(getUserData(CARBON_HOME + File.separator +
                                              temp.getProperty(AutoscalerConstant.PAYLOAD_DIR), tenantId));
            }

            template.getOptions()
                    .as(NovaTemplateOptions.class)
                    .keyPairName(temp.getProperty("keyPair"));

            // add to the data structure
            if (entity.getInstanceContext(temp.getDomainName(), temp.getSubDomainName()) == null) {
                entity.addInstanceContext(new InstanceContext(temp.getDomainName(),
                                                              temp.getSubDomainName(), template));
            }
            else{
                entity.getInstanceContext(temp.getDomainName(), temp.getSubDomainName()).setTemplate(template);
            }

        }

        // since we modified the Context, we need to replace
        replaceIaasContext(entity);
    }

    /**
     * Builds EC2 Template object
     * 
     */
    private void buildEC2Templates(IaasContext entity, String imageId, boolean blockUntilRunning) {

        if (entity.getComputeService() == null) {
            throw new AutoscalerServiceException("Compute service is null for IaaS provider: " + entity.getName());
        }

//        // if domain to template map is null
//        if (entity.getDomainToTemplateMap() == null) {
//            // we initialize it
//            entity.setDomainToTemplateMap(new HashMap<String, Template>());
//        }

        TemplateBuilder templateBuilder = entity.getComputeService().templateBuilder();

        // set image id specified
        templateBuilder.imageId(imageId);

        // to avoid creation of template objects in each and every time, we create all
        // at once! FIXME we could use caching and lazy loading
        for (ServiceTemplate temp : serviceTemps) {

            if (temp.getProperty("instanceType." + Iaases.ec2.toString()) != null) {
                // set instance type eg: m1.large
                templateBuilder.hardwareId(temp.getProperty("instanceType." + Iaases.ec2.toString()));
            }

            // build the Template
            Template template = templateBuilder.build();

            // make it non blocking
            template.getOptions().as(TemplateOptions.class).blockUntilRunning(blockUntilRunning);

            // set EC2 specific options
            template.getOptions()
                    .as(AWSEC2TemplateOptions.class)
                    .placementGroup(temp.getProperty("availabilityZone"));

            template.getOptions()
                    .as(AWSEC2TemplateOptions.class)
                    .securityGroups(temp.getProperty("securityGroups").split(AutoscalerConstant.ENTRY_SEPARATOR));

            if (temp.getProperty(AutoscalerConstant.PAYLOAD_DIR) != null) {
                template.getOptions()
                        .as(AWSEC2TemplateOptions.class)
                        .userData(getUserData(CARBON_HOME + File.separator +
                                              temp.getProperty(AutoscalerConstant.PAYLOAD_DIR), null));
            }

            template.getOptions()
                    .as(AWSEC2TemplateOptions.class)
                    .keyPair(temp.getProperty("keyPair"));

            // add to the data structure
            if (entity.getInstanceContext(temp.getDomainName(), temp.getSubDomainName()) == null) {
                entity.addInstanceContext(new InstanceContext(temp.getDomainName(),
                                                              temp.getSubDomainName(), template));
            }
            else{
                entity.getInstanceContext(temp.getDomainName(), temp.getSubDomainName()).setTemplate(template);
            }

        }

        // since we modified the Context, we need to replace
        replaceIaasContext(entity);

    }

    private String print(Iaases[] values) {
        String str = "";
        for (Iaases iaases : values) {
            str = iaases.name() + ", ";
        }
        str = str.trim();
        return str.endsWith(AutoscalerConstant.ENTRY_SEPARATOR) ? str.substring(0, str.length() - 1) : str;
    }

    @SuppressWarnings("unchecked")
    private void deserialize() {

        String path;

        try {
            path = serializationDir + File.separator +
                   AutoscalerConstant.IAAS_CONTEXT_LIST_SERIALIZING_FILE;

            Object obj = Deserializer.deserialize(path);
            if (obj != null) {
                iaasContextList = (List<IaasContext>) obj;
                log.debug("Deserialization was successful from file: " + path);
            }

            path = serializationDir + File.separator +
                   AutoscalerConstant.LASTLY_USED_IAAS_MAP_SERIALIZING_FILE;

            obj = Deserializer.deserialize(path);

            if (obj != null) {
                lastlyUsedIaasMap = (Map<String, Map<String, String>>) obj;
                log.debug("Deserialization was successful from file: " + path);
            }

        } catch (Exception e) {
            String msg = "Deserialization of objects failed!";
            log.fatal(msg, e);
            throw new DeserializationException(msg, e);
        }

    }

    /**
     * Does all the serialization stuff!
     */
    private void serialize() {

        try {
            Serializer.serialize(iaasContextList,
                                 serializationDir + File.separator +
                                         AutoscalerConstant.IAAS_CONTEXT_LIST_SERIALIZING_FILE);

            Serializer.serialize(lastlyUsedIaasMap,
                                 serializationDir + File.separator +
                                         AutoscalerConstant.LASTLY_USED_IAAS_MAP_SERIALIZING_FILE);

        } catch (IOException e) {
            String msg = "Serialization of objects failed!";
            log.fatal(msg, e);
            throw new SerializationException(msg, e);
        }
    }

    /**
     * A helper method to terminate an instance.
     */
    private void terminate(IaasContext iaasTemp, String nodeId) {

        // this is just to be safe
        if (iaasTemp.getComputeService() == null) {
            String msg = "Unexpeced error occured! IaasContext's ComputeService is null!";
            log.error(msg);
            throw new AutoscalerServiceException(msg);
        }

        // destroy the node
        iaasTemp.getComputeService().destroyNode(nodeId);

        // remove the node id from the Context
        iaasTemp.removeNodeId(nodeId);

        // replace this IaasContext instance, as it reflects the new changes.
        replaceIaasContext(iaasTemp);

        // serialize the objects
        serialize();

        log.info("Node with Id: '" + nodeId + "' is terminated!");
    }

    /**
     * Comparator to compare IaasContexts on different attributes.
     */
    public enum IaasContextComparator implements Comparator<IaasContext> {
        SCALE_UP_SORT {
            public int compare(IaasContext o1, IaasContext o2) {
                return Integer.valueOf(o1.getScaleUpOrder()).compareTo(o2.getScaleUpOrder());
            }
        },
        SCALE_DOWN_SORT {
            public int compare(IaasContext o1, IaasContext o2) {
                return Integer.valueOf(o1.getScaleDownOrder()).compareTo(o2.getScaleDownOrder());
            }
        };

        public static Comparator<IaasContext> ascending(final Comparator<IaasContext> other) {
            return new Comparator<IaasContext>() {
                public int compare(IaasContext o1, IaasContext o2) {
                    return other.compare(o1, o2);
                }
            };
        }

        public static Comparator<IaasContext>
                getComparator(final IaasContextComparator... multipleOptions) {
            return new Comparator<IaasContext>() {
                public int compare(IaasContext o1, IaasContext o2) {
                    for (IaasContextComparator option : multipleOptions) {
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
    
}
