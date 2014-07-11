package org.apache.stratos.manager.composite.application;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.composite.application.beans.CompositeAppDefinition;
import org.apache.stratos.manager.composite.application.parser.CompositeApplicationParser;
import org.apache.stratos.manager.composite.application.parser.DefaultCompositeApplicationParser;
import org.apache.stratos.manager.composite.application.utils.ApplicationUtils;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.CompositeApplicationDefinitionException;
import org.apache.stratos.manager.exception.PersistenceManagerException;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.messaging.domain.topology.ConfigCompositeApplication;

//Grouping
public class CompositeApplicationManager {
	
	private static Log log = LogFactory.getLog(CompositeApplicationManager.class);
	
	public void deployCompositeApplication (CompositeAppDefinition compositeAppDefinition) throws CompositeApplicationDefinitionException, PersistenceManagerException {
//		if (log.isDebugEnabled()) {
//			log.debug("deploying composite application " + configCompositeApplication.getAlias());
//		}
//		registerCompositeApplication(configCompositeApplication);
//		if (log.isDebugEnabled()) {
//			log.debug("publishing composite application " + configCompositeApplication.getAlias());
//		}
//		ApplicationUtils.publishApplicationCreatedEvent(configCompositeApplication);
//		if (log.isDebugEnabled()) {
//			log.debug("composite application successfully deployed" + configCompositeApplication.getAlias());
//		}

        CompositeApplicationParser compositeAppParser = new DefaultCompositeApplicationParser();
        compositeAppParser.parse(compositeAppDefinition);
        
        DataInsertionAndRetrievalManager mgr = new DataInsertionAndRetrievalManager();
        mgr.persistCompositeApplication(compositeAppDefinition);

        // TODO: traverse the data structure and create the subscriptions

        log.info("Composite Application [ Id: " + compositeAppDefinition.getApplicationId() + " , alias: "
                + compositeAppDefinition.getAlias() + " ] deployed successfully");
	}
	
	public void unDeployCompositeApplication(String configApplicationAlias) throws ADCException {
		if (log.isDebugEnabled()) {
			log.debug("undeploying composite application " + configApplicationAlias);
		}
		// unregister application
		unRegisterCompositeApplication(configApplicationAlias);
		if (log.isDebugEnabled()) {
			log.debug("publishing composite application removed event" + configApplicationAlias);
		}
		ApplicationUtils.publishApplicationUnDeployEvent(configApplicationAlias);
		if (log.isDebugEnabled()) {
			log.debug("composite application successfully removed " + configApplicationAlias);
		}
	}
	
	private void registerCompositeApplication(ConfigCompositeApplication configCompositeApplication) throws ADCException {
		
		try {
			if (log.isDebugEnabled()) {
				log.debug("registering composite application " + configCompositeApplication.getAlias());
			}
			DataInsertionAndRetrievalManager mgr = new DataInsertionAndRetrievalManager();
			mgr.persistCompositeApplication ( configCompositeApplication);
			
			if (log.isDebugEnabled()) {
				log.debug("testing to retrieve persisted composite application ");
				Collection<ConfigCompositeApplication> apps = mgr.getCompositeApplications();
				log.debug("retrieved persisted composite application " + apps.size());
				for (ConfigCompositeApplication app : apps) {
					log.debug("retrieved persisted composite application " + app.getAlias());
				}
			}

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error saving composite application " + configCompositeApplication.getAlias();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Successfully registered composite application " + configCompositeApplication.getAlias());
		
	}
	
	private void unRegisterCompositeApplication(String configApplicationAlias) throws ADCException {
		
		try {
			if (log.isDebugEnabled()) {
				log.debug("unregistering composite application " + configApplicationAlias);
			}
			DataInsertionAndRetrievalManager mgr = new DataInsertionAndRetrievalManager();
			mgr.removeCompositeApplication(configApplicationAlias); 
			
			if (log.isDebugEnabled()) {
				log.debug("removed persisted composite application successfully");
			}

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error undeploying composite application " + configApplicationAlias;
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Successfully undeployed composite application " + configApplicationAlias);
		
	}
	
	public void restoreCompositeApplications () throws ADCException {
		try {
			if (log.isDebugEnabled()) {
				log.debug("restoring composite applications " );
			}
			DataInsertionAndRetrievalManager mgr = new DataInsertionAndRetrievalManager();
			Collection<ConfigCompositeApplication> apps = mgr.getCompositeApplications();
			
			if (apps == null) {
				if (log.isDebugEnabled()) {			
					log.debug("no composite application configured");
				}
				return;
			}
			if (log.isDebugEnabled()) {			
				log.debug("retrieved persisted composite application " + apps.size());
				for (ConfigCompositeApplication app : apps) {
					log.debug("retrieved persisted composite application " + app.getAlias());
				}
			}
			// sending application created event to restore in Toplogy
			for (ConfigCompositeApplication app : apps) {
				log.debug("restoring composite application " + app.getAlias());
				ApplicationUtils.publishApplicationCreatedEvent(app);
			}

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error restoring composite application ";
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }
	}

}
