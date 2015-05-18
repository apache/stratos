/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.metadata.service.registry;


import org.apache.stratos.metadata.service.definition.Property;
import org.wso2.carbon.registry.api.RegistryException;

import java.util.List;

/*
 * Interface of the Data Store
 */
public interface DataStore {

    public void addPropertyToApplication(String applicationId, Property property) throws RegistryException;

    public List<Property> getApplicationProperties(String applicationName) throws RegistryException;

    public List<Property> getClusterProperties(String applicationName, String clusterId)
            throws RegistryException;

    public void addPropertyToCluster(String applicationId, String clusterId, Property property) throws RegistryException;

    public boolean deleteApplicationProperties(String applicationId) throws RegistryException;

    public boolean removePropertyFromApplication(String applicationId, String propertyName) throws RegistryException;

    public boolean removePropertyValueFromApplication(String applicationId, String propertyName, String valueToRemove) throws RegistryException;


}
