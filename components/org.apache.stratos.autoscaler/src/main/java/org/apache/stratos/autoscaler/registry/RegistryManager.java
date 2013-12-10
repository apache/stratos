package org.apache.stratos.autoscaler.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.autoscaler.util.Serializer;

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
    public void persist(Object dataObj, String resourcePath) throws RegistryException {
    	
        try {
        	/*
        	if (registryService.resourceExists(resourcePath)) {
                throw new AutoScalerException("Resource already exist in the registry: " + resourcePath);
            }*/
            registryService.beginTransaction();

            Resource nodeResource = registryService.newResource();
            nodeResource.setContent(Serializer.serializeToByteArray(dataObj));            
            
            registryService.put(resourcePath, nodeResource);
            registryService.commitTransaction();
            
            if(log.isDebugEnabled()){
            	
            }
        
        } catch (Exception e) {
            String msg = "Failed to persist the data in registry.";
            registryService.rollbackTransaction();
            log.error(msg, e);
            throw new AutoScalerException(msg, e);

        }
    }
    
    public Object retrieve(String resourcePath) {

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
}
