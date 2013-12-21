package org.apache.stratos.autoscaler.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.util.Deserializer;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.autoscaler.util.Serializer;

import java.util.ArrayList;
import java.util.List;

public class RegistryManager {

	private final static Log log = LogFactory.getLog(RegistryManager.class);
    private static Registry registryService;
    private static RegistryManager registryManager;

    public static RegistryManager getInstance() {

        registryService = ServiceReferenceHolder.getInstance().getRegistry();

        synchronized (RegistryManager.class) {
            if (registryManager == null) {
                if (registryService == null) {
                    // log.warn("Registry Service is null. Hence unable to fetch data from registry.");
                    return registryManager;
                }
                registryManager = new RegistryManager();
            }
        }
        return registryManager;
    }
    
    private RegistryManager() {
        try {
            if (!registryService.resourceExists(AutoScalerConstants.AUTOSCALER_RESOURCE)) {
                registryService.put(AutoScalerConstants.AUTOSCALER_RESOURCE,
                        registryService.newCollection());
            }
        } catch (RegistryException e) {
            String msg =
                    "Failed to create the registry resource " +
                    		AutoScalerConstants.AUTOSCALER_RESOURCE;
            log.error(msg, e);
            throw new AutoScalerException(msg, e);            
        }
    }
    
    /**
     * Persist an object in the local registry.
     *
     * @param dataObj object to be persisted.
     * @param resourcePath resource path to be persisted.
     */
    private void persist(Object dataObj, String resourcePath) throws AutoScalerException {
    	
        try {
            registryService.beginTransaction();

            Resource nodeResource = registryService.newResource();
            nodeResource.setContent(Serializer.serializeToByteArray(dataObj));            
            
            registryService.put(resourcePath, nodeResource);
            registryService.commitTransaction();
        } catch (Exception e) {
            try {
                registryService.rollbackTransaction();
            } catch (RegistryException e1) {
                if(log.isErrorEnabled()) {
                    log.error("Could not rollback transaction", e);
                }
            }
            throw new AutoScalerException("Could not persist data in registry", e);
        }
    }

    public void persistPartition(Partition partition) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.PARTITION_RESOURCE + "/"+partition.getId();
        persist(partition, resourcePath);
        if(log.isDebugEnabled()) {
            log.debug(String.format("Partition written to registry: [id] %s [provider] %s [min] %d [max] %d",
                    partition.getId(), partition.getProvider(), partition.getPartitionMin(), partition.getPartitionMax()));
        }
    }
    
    public void persistNetworkPartition(NetworkPartitionContext nwPartitionCtxt) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.NETWORK_PARTITION_RESOURCE + "/"+nwPartitionCtxt.getId();
        persist(nwPartitionCtxt, resourcePath);
        if(log.isDebugEnabled()) {
            log.debug("NetworkPartitionContext written to registry: "+nwPartitionCtxt.toString());
        }
    }

    public void persistAutoscalerPolicy(AutoscalePolicy autoscalePolicy) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE + "/" + autoscalePolicy.getId();
        persist(autoscalePolicy, resourcePath);
        if(log.isDebugEnabled()) {
            log.debug(String.format("Autoscaler policy written to registry: [id] %s [name] %s [description] %s",
                    autoscalePolicy.getId(), autoscalePolicy.getDisplayName(), autoscalePolicy.getDescription()));
        }
    }

    public void persistDeploymentPolicy(DeploymentPolicy deploymentPolicy) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE + "/" + deploymentPolicy.getId();
        persist(deploymentPolicy, resourcePath);
        if(log.isDebugEnabled()) {
            log.debug(deploymentPolicy.toString());
        }
    }
    
    private Object retrieve(String resourcePath) {
        try {
            Resource resource = registryService.get(resourcePath);
           
            return resource.getContent();

        } catch (ResourceNotFoundException ignore) {
            // this means, we've never persisted info in registry
            return null;
        } catch (RegistryException e) {
            String msg = "Failed to retrieve data from registry.";
            log.error(msg, e);
            throw new AutoScalerException(msg, e);
        }
    }

    public List<Partition> retrievePartitions() {
        List<Partition> partitionList = new ArrayList<Partition>();
        RegistryManager registryManager = RegistryManager.getInstance();
        String[] partitionsResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.PARTITION_RESOURCE);

        if (partitionsResourceList != null) {
            Partition partition;
            for (String resourcePath : partitionsResourceList) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {

                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof Partition) {
                            partition = (Partition) dataObj;
                            if(log.isDebugEnabled()) {
                                log.debug(String.format("Partition read from registry: [id] %s [provider] %s [min] %d [max] %d",
                                        partition.getId(), partition.getProvider(), partition.getPartitionMin(), partition.getPartitionMax()));
                            }
                            partitionList.add(partition);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical partitions will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return partitionList;
    }
    
    public List<NetworkPartitionContext> retrieveNetworkPartitions() {
        List<NetworkPartitionContext> nwPartitionList = new ArrayList<NetworkPartitionContext>();
        RegistryManager registryManager = RegistryManager.getInstance();
        String[] partitionsResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + 
                                                                              AutoScalerConstants.NETWORK_PARTITION_RESOURCE);

        if (partitionsResourceList != null) {
            NetworkPartitionContext nwPartition;
            for (String resourcePath : partitionsResourceList) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {

                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof NetworkPartitionContext) {
                            nwPartition = (NetworkPartitionContext) dataObj;
                            if(log.isDebugEnabled()) {
                                log.debug(String.format("NetworkPartitionContext read from registry: "+nwPartition.toString()));
                            }
                            nwPartitionList.add(nwPartition);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical NetworkPartitionContext will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return nwPartitionList;
    }

    public List<AutoscalePolicy> retrieveASPolicies() {
        List<AutoscalePolicy> asPolicyList = new ArrayList<AutoscalePolicy>();
        RegistryManager registryManager = RegistryManager.getInstance();
        String[] partitionsResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE);

        if (partitionsResourceList != null) {
            AutoscalePolicy asPolicy;
            for (String resourcePath : partitionsResourceList) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {
                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof AutoscalePolicy) {
                            asPolicy = (AutoscalePolicy) dataObj;
                            if(log.isDebugEnabled()) {
                                log.debug(String.format("Autoscaler policy read from registry: [id] %s [name] %s [description] %s",
                                        asPolicy.getId(), asPolicy.getDisplayName(), asPolicy.getDescription()));
                            }
                            asPolicyList.add(asPolicy);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical autoscaler policies will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return asPolicyList;
    }

    public List<DeploymentPolicy> retrieveDeploymentPolicies() {
        List<DeploymentPolicy> depPolicyList = new ArrayList<DeploymentPolicy>();
        RegistryManager registryManager = RegistryManager.getInstance();
        String[] depPolicyResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE);

        if (depPolicyResourceList != null) {
            DeploymentPolicy depPolicy;
            for (String resourcePath : depPolicyResourceList) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {
                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof DeploymentPolicy) {
                            depPolicy = (DeploymentPolicy) dataObj;
                            if(log.isDebugEnabled()) {
                                log.debug(depPolicy.toString());
                            }
                            depPolicyList.add(depPolicy);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical deployment policies will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return depPolicyList;
    }

	public void removeAutoscalerPolicy(AutoscalePolicy autoscalePolicy) {
		 String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE + "/" + autoscalePolicy.getId();
         this.delete(resourcePath);
	     if(log.isDebugEnabled()) {
	          log.debug(String.format("Autoscaler policy deleted from registry: [id] %s [name] %s [description] %s",
	                    autoscalePolicy.getId(), autoscalePolicy.getDisplayName(), autoscalePolicy.getDescription()));
	     }
		
	}
	
	public void removeDeploymentPolicy(DeploymentPolicy depPolicy){
		String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE;
		this.delete(resourcePath);
		if(log.isDebugEnabled()) {
	          log.debug(String.format("Deployment policy deleted from registry: [id] %s" ,
	        		  depPolicy.getId()));
	     }
	}
	
	public void removeNetworkPartition(String networkPartition){
		String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.NETWORK_PARTITION_RESOURCE;
		this.delete(resourcePath);
		if(log.isDebugEnabled()) {
	          log.debug(String.format("Network partition deleted from registry: [id] %s" ,
	        		  networkPartition));
	     }
	}
	

	private void delete(String resourcePath) {
		 try {
	            registryService.beginTransaction();
	            registryService.delete(resourcePath);	           
	            registryService.commitTransaction();
	        } catch (RegistryException e) {
	            try {
	                registryService.rollbackTransaction();
	            } catch (RegistryException e1) {
	                if(log.isErrorEnabled()) {
	                    log.error("Could not rollback transaction", e);
	                }
	            }
	            log.error("Could not delete resource at "+ resourcePath);
	            throw new AutoScalerException("Could not delete data in registry at " + resourcePath, e);
	        }
		
	}
}
