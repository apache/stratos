package org.apache.stratos.cloud.controller.internal;

import java.util.List;

import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.impl.CloudControllerServiceImpl;
import org.apache.stratos.cloud.controller.interfaces.CloudControllerService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.ServiceReferenceHolder;
import org.apache.stratos.lb.common.mb.publish.TopicPublisher;

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
    private static final FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();

    protected void activate(ComponentContext context) {
        try {
        	// get all the topics - comma separated list
        	String topicsString = dataHolder.getTopologyConfig().getProperty(CloudControllerConstants.TOPICS_PROPERTY);
        	
        	if(topicsString == null || topicsString.isEmpty()) {
        		topicsString = CloudControllerConstants.TOPOLOGY_TOPIC_NAME;
        	} 
        	
        	String[] topics = topicsString.split(",");
        	
        	// initialize the topic publishers
        	for (String topic : topics) {
				
        		dataHolder.addTopicPublisher(new TopicPublisher(topic));
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
        ServiceReferenceHolder.getInstance().setTaskService(taskService);
    }

    protected void unsetTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Unsetting the Task Service");
        }
        ServiceReferenceHolder.getInstance().setTaskService(null);
    }
    
	protected void setRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("Setting the Registry Service");
		}
		try {
	        ServiceReferenceHolder.getInstance()
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
        ServiceReferenceHolder.getInstance().setRegistry(null);
	}
	
	protected void deactivate(ComponentContext ctx) {

		List<TopicPublisher> publishers = dataHolder.getAllTopicPublishers();
		for (TopicPublisher topicPublisher : publishers) {
			topicPublisher.close();
		}
	}
}