/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.adc.mgt.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.adc.mgt.repository.Repository;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.event.artifact.synchronization.ArtifactUpdatedEvent;

public class ArtifactUpdatePublisher {
	
	private static final Log log = LogFactory.getLog(ArtifactUpdatePublisher.class);
	
	private Repository repository;
	private String clusterId;
	private String tenantId;
	
	public ArtifactUpdatePublisher(Repository repository, String clusterId, String tenantId) {
		this.repository = repository;
		this.clusterId = clusterId;
		this.tenantId =  tenantId;
		
	}	
	
	public void publish() {
		EventPublisher depsyncEventPublisher = DataHolder.getEventPublisher();
		log.info("publishing ** ");
		depsyncEventPublisher.publish(createArtifactUpdateEvent());
	}

	private ArtifactUpdatedEvent createArtifactUpdateEvent() {
		ArtifactUpdatedEvent artifactUpdateEvent = new ArtifactUpdatedEvent();
		artifactUpdateEvent.setClusterId(clusterId);
		artifactUpdateEvent.setRepoUserName(repository.getUserName());
		artifactUpdateEvent.setRepoPassword(repository.getPassword());
		artifactUpdateEvent.setRepoURL(repository.getUrl());
		artifactUpdateEvent.setTenantId(tenantId);
		
		log.info("Creating artifact updated event ");
		log.info("cluster Id : " + artifactUpdateEvent.getClusterId());
		log.info("repo url : " + artifactUpdateEvent.getRepoURL());
		log.info("repo username : " + artifactUpdateEvent.getRepoUserName());
		log.info("repo pwd : " + artifactUpdateEvent.getRepoPassword());
		log.info("tenant Id : " + artifactUpdateEvent.getTenantId());
		return artifactUpdateEvent;
	}
}
