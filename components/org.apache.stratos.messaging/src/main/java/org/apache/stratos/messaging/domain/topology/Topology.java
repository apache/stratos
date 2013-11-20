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

/**
 * Defines a topology of serviceMap in Stratos.
 */
public class Topology implements Serializable {
    private static final long serialVersionUID = -2453583548027402122L;
    // Key: Service.serviceName
    private Map<String, Service> serviceMap;
    private Map<String, Partition> partitionMap;

    public Topology() {
        this.serviceMap = new HashMap<String, Service>();
        partitionMap = new HashMap<String, Partition>();
    }

    public Collection<Service> getServices() {
        return serviceMap.values();
    }

    public Collection<Partition> getPartitions() {
        return partitionMap.values();
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

    public Map<String, Partition> getPartitionMap() {
        return partitionMap;
    }

    public Partition getPartition(String id) {
        return  this.partitionMap.get(id);
    }

    public void setPartitionMap(Map<String, Partition> partitionMap) {
        this.partitionMap = partitionMap;
    }

    public void addPartition(Partition partition) {
        this.partitionMap.put(partition.getId(), partition);
    }

    public void addPartitions(Collection<Partition> partitions) {
        for (Partition partition : partitions) {
            addPartition(partition);
        }
    }

    public void removePartition(Partition partition) {
        this.partitionMap.remove(partition.getId());
    }

    public void removePartition(String partitionId) {
        this.partitionMap.remove(partitionId);
    }
}
