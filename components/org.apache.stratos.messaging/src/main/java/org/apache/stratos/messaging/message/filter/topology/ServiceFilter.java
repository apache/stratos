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

package org.apache.stratos.messaging.message.filter.topology;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A filter to discard topology events which are not in a given service name list.
 */
public class ServiceFilter {
    private static final Log log = LogFactory.getLog(ServiceFilter.class);
    private static volatile ServiceFilter instance;

    private Map<String, Boolean> serviceNameMap;

    private ServiceFilter() {
        this.serviceNameMap = new HashMap<String, Boolean>();

        String filter = System.getProperty("stratos.messaging.topology.service.filter");
        if(StringUtils.isNotBlank(filter)) {
            String[] array = filter.split(",");
            for(String item : array) {
                serviceNameMap.put(item, true);
            }
            if(log.isDebugEnabled()) {
                log.debug(String.format("Service filter initialized: [included] %s", filter));
            }
        }
    }

    public static synchronized ServiceFilter getInstance() {
        if (instance == null) {
            synchronized (ServiceFilter.class){
                if (instance == null) {
                    instance = new ServiceFilter();
                    if(log.isDebugEnabled()) {
                        log.debug("Service filter object created");
                    }
                }
            }
        }
        return instance;
    }

    public boolean isActive() {
        return serviceNameMap.size() > 0;
    }

    public boolean included(String serviceName) {
        return serviceNameMap.containsKey(serviceName);
    }

    public boolean excluded(String serviceName) {
        return !serviceNameMap.containsKey(serviceName);
    }

    public Collection<String> getIncludedServiceNames() {
        return serviceNameMap.keySet();
    }
}
