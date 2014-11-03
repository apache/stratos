package org.apache.stratos.autoscaler.util;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.session.UserRegistry;

public class ServiceReferenceHolder {
	
	private static ServiceReferenceHolder instance;
	private Registry registry;
    private TaskService taskService;

	private ServiceReferenceHolder() {
	}
	 
	public static ServiceReferenceHolder getInstance() {
	    if (instance == null) {
	        instance = new ServiceReferenceHolder();
	    }
	        return instance;
	}
	 
	public void setRegistry(UserRegistry governanceSystemRegistry) {
		registry = governanceSystemRegistry;
	}

    public Registry getRegistry() {
		return registry;
	}

    public TaskService getTaskService() {
        return taskService;
    }

    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }
}
