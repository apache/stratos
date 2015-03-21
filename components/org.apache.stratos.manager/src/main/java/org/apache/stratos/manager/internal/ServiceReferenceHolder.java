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

package org.apache.stratos.manager.internal;

import com.hazelcast.core.HazelcastInstance;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.stratos.common.services.ComponentStartUpSynchronizer;
import org.apache.stratos.common.services.DistributedObjectProvider;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Service reference holder.
 */
public class ServiceReferenceHolder {
    private static volatile ServiceReferenceHolder instance = null;

    private static ConfigurationContext clientConfigContext;
    private static ConfigurationContext serverConfigContext;
    private static RealmService realmService;
    private static RegistryService registryService;
    private TaskService taskService;
    private HazelcastInstance hazelcastInstance;
    private AxisConfiguration axisConfiguration;
    private DistributedObjectProvider distributedObjectProvider;
    private ComponentStartUpSynchronizer componentStartUpSynchronizer;

    private ServiceReferenceHolder() {       }

    public static ServiceReferenceHolder getInstance() {
        if (instance == null) {
            synchronized (ServiceReferenceHolder .class){
                if (instance == null) {
                    instance = new ServiceReferenceHolder();
                }
            }
        }
        return instance;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static void setRealmService(RealmService realmService) {
        ServiceReferenceHolder.realmService = realmService;
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static ConfigurationContext getClientConfigContext() {
        CarbonUtils.checkSecurity();
        return clientConfigContext;
    }

    public static void setClientConfigContext(ConfigurationContext clientConfigContext) {
        ServiceReferenceHolder.clientConfigContext = clientConfigContext;
    }

    public static ConfigurationContext getServerConfigContext() {
        CarbonUtils.checkSecurity();
        return serverConfigContext;
    }

    public static void setServerConfigContext(ConfigurationContext serverConfigContext) {
        ServiceReferenceHolder.serverConfigContext = serverConfigContext;
    }

    public static void setRegistryService(RegistryService registryService) {
        ServiceReferenceHolder.registryService = registryService;
    }

    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }

    public void setAxisConfiguration(AxisConfiguration axisConfiguration) {
        this.axisConfiguration = axisConfiguration;
    }

    public AxisConfiguration getAxisConfiguration() {
        return axisConfiguration;
    }
    
    public void setDistributedObjectProvider(DistributedObjectProvider distributedObjectProvider) {
        this.distributedObjectProvider = distributedObjectProvider;
    }

    public DistributedObjectProvider getDistributedObjectProvider() {
        return distributedObjectProvider;
    }

    public void setComponentStartUpSynchronizer(ComponentStartUpSynchronizer componentStartUpSynchronizer) {
        this.componentStartUpSynchronizer = componentStartUpSynchronizer;
    }

    public ComponentStartUpSynchronizer getComponentStartUpSynchronizer() {
        return componentStartUpSynchronizer;
    }
}
