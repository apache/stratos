///*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *  http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//
//package org.apache.stratos.cloud.controller.pojo;
//
//import org.apache.stratos.cloud.controller.pojo.payload.PayloadDataHolder;
//import org.apache.stratos.messaging.domain.topology.Cluster;
//import org.apache.stratos.metadata.client.pojo.DataContext;
//
//import java.util.Map;
//import java.util.Set;
//
//public class ClusterDataHolder {
//
//    // Cluster Ids map, key = service name
//    private Map<String, Set<String>> clusterIdMap;
//
//    // Cluster object collection
//    private Set<Cluster> clusters;
//
////    // payload related information
////    private Set<PayloadDataHolder> payloadDataHolders;
//
//   public ClusterDataHolder (Map<String, Set<String>> clusterIdMap, Set<Cluster> clusters) {
//       this.clusterIdMap = clusterIdMap;
//       this.clusters = clusters;
//   }
//
//    public Map<String, Set<String>> getClusterIdMap() {
//        return clusterIdMap;
//    }
//
//    public Set<Cluster> getApplicationClusterContexts() {
//        return clusters;
//    }
//
//
////    public Set<PayloadDataHolder> getPayloadDataHolders() {
////        return payloadDataHolders;
////    }
////
////    public void setPayloadDataHolders(Set<PayloadDataHolder> payloadDataHolders) {
////        this.payloadDataHolders = payloadDataHolders;
////    }
//}
