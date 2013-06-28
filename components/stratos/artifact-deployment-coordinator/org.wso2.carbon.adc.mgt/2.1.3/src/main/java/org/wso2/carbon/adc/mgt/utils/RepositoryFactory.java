/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.utils;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.mgt.dao.Repository;
import org.wso2.carbon.adc.mgt.service.ApplicationManagementService;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * @author wso2
 * 
 */
public class RepositoryFactory {

	private static final Log log = LogFactory.getLog(ApplicationManagementService.class);

	/*public synchronized Repository createRepository(String cartridgeName, String tenantDomain,
	                                          String userName) throws Exception {

		Repository repository = new Repository();
		String repoName = tenantDomain + "/" + cartridgeName; // removed .git part
		String repoUserName = userName + "@" + tenantDomain;

		Process proc;
		try {

			String command =
			                 CarbonUtils.getCarbonHome() + File.separator + "bin" + File.separator +
			                         "manage-git-repo.sh " + "create " + repoUserName + " " +
			                         tenantDomain + " " + cartridgeName + " " +
			                         System.getProperty(CartridgeConstants.REPO_NOTIFICATION_URL) + " " +
			                         System.getProperty(CartridgeConstants.GIT_HOST_NAME) +
			                         " /";
			proc = Runtime.getRuntime().exec(command);
			log.info("executing manage-git-repo script..... command :" + command);
			proc.waitFor();
			log.info(" Repo is created ..... for user: " + userName + ", tenantName: " +
			         tenantDomain + " ");
			repository.setRepoName("git@" + System.getProperty(CartridgeConstants.GIT_HOST_NAME) + ":" +repoName);
		} catch (Exception e) {
			log.error(" Exception is occurred when executing manage-git-repo script. Reason :" +
			          e.getMessage());
			handleException(e.getMessage(), e);
		}

		return repository;

	}*/

	/*public synchronized void createGitFolderStructure(String tenantDomain, String cartridgeName,
	                                            String[] dirArray) throws Exception {

		log.info("In create Git folder structure...!");

		StringBuffer dirBuffer = new StringBuffer();
		for (String dir : dirArray) {
			dirBuffer.append(dir).append(" ");
		}

		Process proc;
		try {
			String command =
			                 CarbonUtils.getCarbonHome() + File.separator + "bin" + File.separator +
			                         "git-folder-structure.sh " + tenantDomain + " " +
			                         cartridgeName + " " + dirBuffer.toString() + " /";
			proc = Runtime.getRuntime().exec(command);
			log.info("executing manage-git-repo script..... command : " + command);
			proc.waitFor();

		} catch (Exception e) {
			log.error(" Exception is occurred when executing manage-git-repo script. Reason :" +
			          e.getMessage());
			handleException(e.getMessage(), e);
		}

		log.info(" Folder structure  is created ..... ");

	}*/
	
	public synchronized void destroyRepository(String cartridgeName, String tenantDomain,
	                                          String userName) throws Exception {

		String repoUserName = userName + "@" + tenantDomain;

		Process proc;
		try {

			String command =
			                 CarbonUtils.getCarbonHome() + File.separator + "bin" + File.separator +
			                         "manage-git-repo.sh " + "destroy " + repoUserName + " " +
			                         tenantDomain + " " + cartridgeName + 
			                         " /";
			proc = Runtime.getRuntime().exec(command);
			log.info("executing manage-git-repo script (destroy)..... command :" + command);
			proc.waitFor();
			log.info(" Repo is destroyed ..... for user: " + userName + ", tenantName: " +
			         tenantDomain + " ");
		} catch (Exception e) {
			log.error(" Exception is occurred when destroying git repo. Reason :" +
			          e.getMessage());
			handleException(e.getMessage(), e);
		}

	}

	private void handleException(String msg, Exception e) throws Exception {
		log.error(msg, e);
		throw new Exception(msg, e);
	}

}
