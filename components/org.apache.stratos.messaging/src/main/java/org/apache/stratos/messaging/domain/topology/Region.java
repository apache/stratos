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
import java.util.Map;
import java.util.Properties;

/**
 * Defines a geographical region of IaaS cloud.
 */
public class Region {
    private String regionId;
    private String regionName;
    private Properties properties;
    private Map<String, Zone> zoneMap;

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Collection<Zone> getZones() {
        return zoneMap.values();
    }

    public void addZone(Zone zone) {
        zoneMap.put(zone.getZoneId(), zone);
    }

    public void removeZone(Zone zone) {
        zoneMap.remove(zone.getZoneId());
    }

    public void removeZone(String zoneId) {
        zoneMap.remove(zoneId);
    }

    public Zone getZone(String zoneId) {
        return zoneMap.get(zoneId);
    }

}
