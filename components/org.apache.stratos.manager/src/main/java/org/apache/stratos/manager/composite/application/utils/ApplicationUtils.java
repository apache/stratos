package org.apache.stratos.manager.composite.application.utils;

import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.exception.InvalidCartridgeAliasException;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.event.topology.CompositeApplicationCreatedEvent;
import org.apache.stratos.messaging.event.topology.CompositeApplicationRemovedEvent;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.messaging.domain.topology.ConfigCompositeApplication;


// Grouping
public class ApplicationUtils {
	
	private static Log log = LogFactory.getLog(ApplicationUtils.class);
	
	   static class ApplicationCreatedEventPublisher implements Runnable {
	    	
	    	private ConfigCompositeApplication configCompositeApplication;

	        public ApplicationCreatedEventPublisher(ConfigCompositeApplication configCompositeApplication) {
	    		this.configCompositeApplication = configCompositeApplication;

			}
	        
			@Override
			public void run() {
				try {
					if(log.isInfoEnabled()) {
						log.info(String.format("Publishing application createdevent: [application-alias] %s ", configCompositeApplication.getAlias()));
					}
					CompositeApplicationCreatedEvent event = new CompositeApplicationCreatedEvent(configCompositeApplication);
					EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.TOPOLOGY_TOPIC);
					eventPublisher.publish(event);
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error(String.format("Could not publish tenant subscribed event: [application-alias] %s ", configCompositeApplication.getAlias()), e);
					}
				}
				
			}
	    	
	    }
	   
	    public static void publishApplicationCreatedEvent(ConfigCompositeApplication configCompositeApplication) {
	    	
	    	
	    	Executor exec = new Executor() {
				@Override
				public void execute(Runnable command) {
					command.run();
				}
			};
			
			exec.execute(new ApplicationCreatedEventPublisher(configCompositeApplication));
	    }

	    
		   static class ApplicationRemovedEventPublisher implements Runnable {
		    	
		    	private String configCompositeApplicationAlias;

		        public ApplicationRemovedEventPublisher(String configCompositeApplicationAlias) {
		    		this.configCompositeApplicationAlias = configCompositeApplicationAlias;

				}
		        
				@Override
				public void run() {
					try {
						if(log.isInfoEnabled()) {
							log.info(String.format("Publishing application createdevent: [application-alias] %s ", configCompositeApplicationAlias));
						}
						CompositeApplicationRemovedEvent event = new CompositeApplicationRemovedEvent(configCompositeApplicationAlias);
						EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.TOPOLOGY_TOPIC);
						eventPublisher.publish(event);
					} catch (Exception e) {
						if (log.isErrorEnabled()) {
							log.error(String.format("Could not publish composite removed event event: [application-alias] %s ", configCompositeApplicationAlias), e);
						}
					}
					
				}
		    	
		    }
	    
	    public static void publishApplicationUnDeployEvent(String configApplicationAlias) {
	    	Executor exec = new Executor() {
				@Override
				public void execute(Runnable command) {
					command.run();
				}
			};
			
			exec.execute(new ApplicationRemovedEventPublisher(configApplicationAlias));
	    }

    public static boolean isAliasValid (String alias) {

        String patternString = "([a-z0-9]+([-][a-z0-9])*)+";
        Pattern pattern = Pattern.compile(patternString);
        
        return pattern.matcher(alias).matches();
    }


}
