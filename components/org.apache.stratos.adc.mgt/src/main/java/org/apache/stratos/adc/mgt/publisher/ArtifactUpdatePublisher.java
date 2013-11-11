/**
 * 
 */
package org.apache.stratos.adc.mgt.publisher;

import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.adc.mgt.repository.Repository;
import org.apache.stratos.adc.mgt.utils.RepoPasswordMgtUtil;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.event.artifact.synchronization.ArtifactUpdatedEvent;

/**
 * @author wso2
 *
 */
public class ArtifactUpdatePublisher {

	private Repository repository;
	private String clusterId;
	
	public ArtifactUpdatePublisher(Repository repository, String clusterId) {
		this.repository = repository;
		this.clusterId = clusterId;
	}	
	
	public void publish() {
		EventPublisher depsyncEventPublisher = DataHolder.getEventPublisher();
		depsyncEventPublisher.publish(createArtifactUpdateEvent());
	}

	private ArtifactUpdatedEvent createArtifactUpdateEvent() {
		ArtifactUpdatedEvent artifactUpdateEvent = new ArtifactUpdatedEvent();
		artifactUpdateEvent.setClusterId(clusterId);
		artifactUpdateEvent.setRepoUserName(repository.getUserName());
		artifactUpdateEvent.setRepoPassword(RepoPasswordMgtUtil.decryptPassword(repository.getPassword())); // Decrypt
		artifactUpdateEvent.setRepoURL(repository.getUrl());
		return artifactUpdateEvent;
	}
}
