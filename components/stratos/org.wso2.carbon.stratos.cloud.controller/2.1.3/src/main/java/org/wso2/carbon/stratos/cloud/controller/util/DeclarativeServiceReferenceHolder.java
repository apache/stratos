/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.stratos.cloud.controller.util;

import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.cloud.controller.topic.ConfigurationPublisher;

/**
 * Singleton class to hold all the service references.
 */
public class DeclarativeServiceReferenceHolder {

    private static DeclarativeServiceReferenceHolder instance;
    private TaskService taskService;
    private ConfigurationPublisher configPub;
    private Registry registry;
    
    private DeclarativeServiceReferenceHolder() {
    }

    public static DeclarativeServiceReferenceHolder getInstance() {
        if (instance == null) {
            instance = new DeclarativeServiceReferenceHolder();
        }
        return instance;
    }
    
    public ConfigurationPublisher getConfigPub(){
    	return configPub;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }
    
    public void setConfigPub(ConfigurationPublisher configPub) {
        this.configPub = configPub;
    }

	public void setRegistry(UserRegistry governanceSystemRegistry) {
		registry = governanceSystemRegistry;
    }

	public Registry getRegistry() {
	    return registry;
    }
    
}
