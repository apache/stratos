package org.apache.stratos.metadataservice.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.MemberStartedEvent;
import org.apache.stratos.messaging.event.topology.MemberSuspendedEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.listener.topology.MemberStartedEventListener;
import org.apache.stratos.messaging.listener.topology.MemberSuspendedEventListener;
import org.apache.stratos.messaging.listener.topology.MemberTerminatedEventListener;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.metadataservice.services.MetaDataAdmin;

public class TopologyListener implements ServletContextListener {

	private static final Log log = LogFactory.getLog(TopologyListener.class);

	private TopologyAgent topologyThread = null;
	private Thread thread = null;

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		log.info("Topology literner started....");
		if (topologyThread == null) {
			// load default agent
			topologyThread = new TopologyAgent();
			if (log.isDebugEnabled()) {
				log.debug("Loading default Cartridge Agent.");
			}
		}
		// start agent
		thread = new Thread(topologyThread);
		thread.start();

	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		thread.stop();
	}

	protected void registerTopologyEventListeners() {
		if (log.isDebugEnabled()) {
			log.debug("Starting topology event message receiver thread");
		}
		TopologyEventReceiver topologyEventReceiver = new TopologyEventReceiver();

		topologyEventReceiver.addEventListener(new MemberTerminatedEventListener() {
			@Override
			protected void onEvent(Event event) {
				try {
					TopologyManager.acquireReadLock();
					if (log.isDebugEnabled()) {
						log.debug("Member terminated event received");
					}
					MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;
					if(log.isDebugEnabled()){
    					log.info("Terminated event :::::::::::::::::::: " +
    					                   memberTerminatedEvent.getServiceName());
					}
					new MetaDataAdmin().removeCartridgeMetaDataDetails("appA", "php");

					// extensionHandler.onMemberTerminatedEvent(memberTerminatedEvent);
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("Error processing member terminated event", e);
					}
				} finally {
					TopologyManager.releaseReadLock();
				}
			}
		});

		topologyEventReceiver.addEventListener(new MemberSuspendedEventListener() {
			@Override
			protected void onEvent(Event event) {
				try {
					TopologyManager.acquireReadLock();
					if (log.isDebugEnabled()) {
						log.debug("Member suspended event received");
					}
					MemberSuspendedEvent memberSuspendedEvent = (MemberSuspendedEvent) event;
					// extensionHandler.onMemberSuspendedEvent(memberSuspendedEvent);
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("Error processing member suspended event", e);
					}
				} finally {
					TopologyManager.releaseReadLock();
				}
			}
		});

		topologyEventReceiver.addEventListener(new MemberStartedEventListener() {
			@Override
			protected void onEvent(Event event) {
				try {
					TopologyManager.acquireReadLock();
					if (log.isDebugEnabled()) {
						log.debug("Member started event received");
					}
					MemberStartedEvent memberStartedEvent = (MemberStartedEvent) event;
					// extensionHandler.onMemberStartedEvent(memberStartedEvent);
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("Error processing member started event", e);
					}
				} finally {
					TopologyManager.releaseReadLock();
				}
			}
		});

		Thread thread = new Thread(topologyEventReceiver);
		thread.start();
		if (log.isDebugEnabled()) {
			log.info("Cartridge Agent topology receiver thread started");
		}
	}

}
