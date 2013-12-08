/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.stratos.load.balancer.context.map;

import org.apache.stratos.load.balancer.ServiceContext;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service name, service context map for accessing service contexts using service name:
 * Map[ServiceName, ServiceContext]
 */
public class ServiceNameServiceContextMap {

    private ConcurrentHashMap<String, ServiceContext> concurrentHashMap;

    public ServiceNameServiceContextMap() {
        concurrentHashMap = new ConcurrentHashMap<String, ServiceContext>();
    }

    public Collection<ServiceContext> getServiceContexts() {
        return concurrentHashMap.values();
    }

    public ServiceContext getServiceContext(String serviceName) {
        return concurrentHashMap.get(serviceName);
    }

    public void addServiceContext(ServiceContext serviceContext) {
        concurrentHashMap.put(serviceContext.getServiceName(), serviceContext);
    }

    public void removeServiceContext(String serviceName) {
        concurrentHashMap.remove(serviceName);
    }

    public void clear() {
        concurrentHashMap.clear();
    }
}
