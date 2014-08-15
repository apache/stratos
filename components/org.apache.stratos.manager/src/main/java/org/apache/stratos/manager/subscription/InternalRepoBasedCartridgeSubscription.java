/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.manager.subscription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.InvalidRepositoryException;
import org.apache.stratos.manager.exception.RepositoryCredentialsRequiredException;
import org.apache.stratos.manager.exception.RepositoryRequiredException;
import org.apache.stratos.manager.exception.RepositoryTransportException;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.service.RepositoryInfoBean;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.apache.stratos.manager.utils.RepositoryCreator;

public class InternalRepoBasedCartridgeSubscription extends CartridgeSubscription {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1869600699603408517L;
	private static final Log log = LogFactory.getLog(InternalRepoBasedCartridgeSubscription.class);
	
	public InternalRepoBasedCartridgeSubscription(CartridgeInfo cartridgeInfo,
			SubscriptionTenancyBehaviour subscriptionTenancyBehaviour) {
		super(cartridgeInfo, subscriptionTenancyBehaviour);
	}

	@Override
	public Repository manageRepository(String repoURL, String repoUserName,
			String repoUserPassword, boolean privateRepo) throws ADCException,
			RepositoryRequiredException,
			RepositoryCredentialsRequiredException,
			RepositoryTransportException, InvalidRepositoryException {

        if(log.isDebugEnabled()) {
            log.debug("Managing internal repo for repo URL: " + repoURL);
        }
		
		Repository repository = null;
		String defaultRepoUserName = System.getProperty(CartridgeConstants.INTERNAL_GIT_USERNAME);
		String defaultRepoPassword = System.getProperty(CartridgeConstants.INTERNAL_GIT_PASSWORD);
		String[] dirArray = {"test"};

        if (repoURL != null && !repoURL.equalsIgnoreCase("null") && !repoURL.isEmpty()) {
            repository = new Repository();
            repository.setUrl(repoURL);
            repository.setUserName(defaultRepoUserName);
            repository.setPassword(defaultRepoPassword);
            return repository;
        }
		
		// Repo URL will be generated inside createInternalRepository method
		RepositoryInfoBean repoInfoBean = new RepositoryInfoBean(repoURL, getAlias(), getSubscriber().getTenantDomain(),
				defaultRepoUserName, defaultRepoPassword, dirArray);
		RepositoryCreator repoCreator = new RepositoryCreator(repoInfoBean);
		try {
			repository = repoCreator.createInternalRepository();
		} catch (Exception e) {
			String msg = "Error occurred in creating internal repository. ";
			log.error( msg + e.getMessage());
			throw new ADCException(msg);
		}
		return repository;
				
	}
	
   

}
