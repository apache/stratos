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
package org.apache.stratos.messaging.event.topology;

import java.util.HashMap;
import java.util.Map;

public class PartitionUpdatedEvent {
    private String id;
    private String scope;
    private Map<String, String> properties = new HashMap<String, String>();
    private String oldPartitionId;

    PartitionUpdatedEvent(String id, String scope, String oldId) {
        this.id = id;
        this.scope = scope;
        this.oldPartitionId = oldId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void setProperty(String key, String value) {

        if (key != null && value != null) {
            getProperties().put(key, value);
        }
    }

    public String getProperty(String key) {
        return getProperties().get(key);
    }

    public String getOldPartitionId() {
        return oldPartitionId;
    }

    public void setOldPartitionId(String oldPartitionId) {
        this.oldPartitionId = oldPartitionId;
    }
}
