package org.wso2.carbon.stratos.cloud.controller.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.stratos.cloud.controller.exception.CloudControllerException;
import org.wso2.carbon.stratos.cloud.controller.impl.CloudControllerServiceImpl;
import org.wso2.carbon.stratos.cloud.controller.interfaces.CloudControllerService;
import org.wso2.carbon.stratos.cloud.controller.topic.ConfigurationPublisher;
import org.wso2.carbon.stratos.cloud.controller.util.DeclarativeServiceReferenceHolder;

/**
 * Registering Cloud Controller Service.
 * 
 * @scr.component name="org.wso2.carbon.stratos.cloud.controller" immediate="true"
 * @scr.reference name="ntask.component" interface="org.wso2.carbon.ntask.core.service.TaskService"
 *                cardinality="1..1" policy="dynamic" bind="setTaskService"
 *                unbind="unsetTaskService"
 * @scr.reference name="registry.service"
 *                interface=
 *                "org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 */
public class CloudControllerDSComponent {

    private static final Log log = LogFactory.getLog(CloudControllerDSComponent.class);

    protected void activate(ComponentContext context) {
        try {
        	if (DeclarativeServiceReferenceHolder.getInstance().getConfigPub() == null) {
        		DeclarativeServiceReferenceHolder.getInstance()
        		.setConfigPub(new ConfigurationPublisher());
        	}
            
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(CloudControllerService.class.getName(),
                                          new CloudControllerServiceImpl(), null);
            

            log.debug("******* Cloud Controller Service bundle is activated ******* ");
        } catch (Throwable e) {
            log.error("******* Cloud Controller Service bundle is failed to activate ****", e);
        }
    }
    
    protected void setTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Task Service");
        }
        DeclarativeServiceReferenceHolder.getInstance().setTaskService(taskService);
    }

    protected void unsetTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Unsetting the Task Service");
        }
        DeclarativeServiceReferenceHolder.getInstance().setTaskService(null);
    }
    
	protected void setRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("Setting the Registry Service");
		}
		try {
	        DeclarativeServiceReferenceHolder.getInstance()
	                                             .setRegistry(registryService.getGovernanceSystemRegistry());
        } catch (RegistryException e) {
        	String msg = "Failed when retrieving Governance System Registry.";
        	log.error(msg, e);
        	throw new CloudControllerException(msg, e);
        }
	}

	protected void unsetRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
            log.debug("Unsetting the Registry Service");
        }
        DeclarativeServiceReferenceHolder.getInstance().setRegistry(null);
	}
}