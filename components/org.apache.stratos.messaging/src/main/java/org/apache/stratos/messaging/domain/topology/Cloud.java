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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Defines an IaaS cloud.
 */
public class Cloud {
    private String cloudId;
    private String cloudName;
    private Properties properties;
    private Map<String, Region> regionMap;

    public Cloud() {
        this.regionMap = new HashMap<String, Region>();
    }

    public String getCloudId() {
        return cloudId;
    }

    public void setCloudId(String cloudId) {
        this.cloudId = cloudId;
    }

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Collection<Region> getRegions() {
        return regionMap.values();
    }

    public void addRegion(Region region) {
        regionMap.put(region.getRegionId(), region);
    }

    public void removeRegion(Region region) {
        regionMap.remove(region.getRegionId());
    }

    public void removeRegion(String regionId) {
        regionMap.remove(regionId);
    }

    public Region getRegion(String regionId) {
        return regionMap.get(regionId);
    }

}
