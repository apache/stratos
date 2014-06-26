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

package org.apache.stratos.messaging.domain.topology;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.util.CompositeApplicationBuilder;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Defines a topology of serviceMap in Stratos.
 */
public class Topology implements Serializable {
    private static final long serialVersionUID = -2453583548027402122L;
    // Key: Service.serviceName
    private Map<String, Service> serviceMap;
    //Grouping
    private Map<String, CompositeApplication> compositeApplicationMap;
    private Map<String, ConfigCompositeApplication> configCompositeApplicationMap;
    private boolean initialized;
    private static Log log = LogFactory.getLog(Topology.class);

    public Topology() {
        this.serviceMap = new HashMap<String, Service>();
        this.compositeApplicationMap = new HashMap<String, CompositeApplication>();
        this.configCompositeApplicationMap = new HashMap<String, ConfigCompositeApplication>();
    }

    public Collection<Service> getServices() {
        return serviceMap.values();
    }

    public void addService(Service service) {
        this.serviceMap.put(service.getServiceName(), service);
    }

    public void addServices(Collection<Service> services) {
        for (Service service : services) {
            addService(service);
        }
    }

    public void removeService(Service service) {
        this.serviceMap.remove(service.getServiceName());
    }

    public void removeService(String serviceName) {
        this.serviceMap.remove(serviceName);
    }

    public Service getService(String serviceName) {
        return this.serviceMap.get(serviceName);
    }

    public boolean serviceExists(String serviceName) {
        return this.serviceMap.containsKey(serviceName);
    }

    public void clear() {
        this.serviceMap.clear();
    }
    
    // Grouping
    public Collection<CompositeApplication> getCompositeApplication() {
        return this.compositeApplicationMap.values();
    }

    public void addCompositeApplication(String alias, CompositeApplication app) {
        this.compositeApplicationMap.put(alias, app);
    }

    public void removeCompositeApplication(String alias) {
        this.compositeApplicationMap.remove(alias);
    }
    
    public Collection<ConfigCompositeApplication> getConfigCompositeApplication() {
        
        if (this.configCompositeApplicationMap == null) {
    		log.info("adding new config comp in topology while retrieving it, ConfigCompositeApplication is  null");
    		this.configCompositeApplicationMap = new HashMap<String, ConfigCompositeApplication>();
    	} 
        return this.configCompositeApplicationMap.values();
    }
    
    public void addConfigCompositeApplication(String alias, ConfigCompositeApplication configApp) {
    	log.info("adding config comp in topology" + alias + " / " + configApp);
    	if (this.configCompositeApplicationMap != null) {
    		log.info("adding config comp in topology, ConfigCompositeApplication is not null");
    		this.configCompositeApplicationMap.put(alias, configApp);
    		log.info("successful config comp in topology, ConfigCompositeApplication is not null");
    	} else {
    		log.info("adding config comp in topology, ConfigCompositeApplication is null, adding one");
    		this.configCompositeApplicationMap = new HashMap<String, ConfigCompositeApplication>();
    		this.configCompositeApplicationMap.put(alias, configApp);
    	}
    }

    public void removeConfigCompositeApplication(String alias) {
        this.configCompositeApplicationMap.remove(alias);
    }
    
    public void removeAllCompositeApplication() {
    	java.util.Set<String> keys = this.compositeApplicationMap.keySet();
    	for (String key : keys) {
    		compositeApplicationMap.remove(key);
    	}
    }
    
    public void removeAllConfigCompositeApplication() {
    	java.util.Set<String> keys = this.configCompositeApplicationMap.keySet();
    	for (String key : keys) {
    		configCompositeApplicationMap.remove(key);
    	}
    }

    public CompositeApplication getCompositeApplication(String appAlias) {
        return this.compositeApplicationMap.get(appAlias);
    }

    public boolean compositeApplicationExists(String appAlias) {
        return this.compositeApplicationMap.containsKey(appAlias);
    }
    
    public ConfigCompositeApplication getConfigCompositeApplication(String appAlias) {
        return this.configCompositeApplicationMap.get(appAlias);
    }

    public boolean configCompositeApplicationExists(String appAlias) {
        return this.configCompositeApplicationMap.containsKey(appAlias);
    }


    public Map<String, ConfigCompositeApplication> getConfigCompositeApplicationMap() {
		return configCompositeApplicationMap;
	}
    
    public void setConfigCompositeApplicationMap(Map<String, ConfigCompositeApplication> configCompositeApplicationMap) {
		this.configCompositeApplicationMap = configCompositeApplicationMap;
	}

	public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public String toString() {
        return "Topology [serviceMap=" + serviceMap + ", initialized=" + initialized + "]";
    }
}
