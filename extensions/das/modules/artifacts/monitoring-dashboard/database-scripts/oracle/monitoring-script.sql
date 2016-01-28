/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

CREATE TABLE AVG_MEMORY_CONSUMPTION_STATS(Time NUMBER(20), ClusterId VARCHAR2(150), ClusterInstanceId VARCHAR2(150),
NetworkPartitionId VARCHAR2(150), Value NUMERIC);
CREATE TABLE M_AVG_MEMORY_CONSUMPTION_STATS(Time NUMBER(20), MemberId VARCHAR2(150), ClusterId VARCHAR2(150),
ClusterInstanceId VARCHAR2(150), NetworkPartitionId VARCHAR2(150), Value NUMERIC);
CREATE TABLE AVG_LOAD_AVERAGE_STATS(Time NUMBER(20), ClusterId VARCHAR2(150), ClusterInstanceId VARCHAR2(150),
NetworkPartitionId VARCHAR2(150), Value NUMERIC);
CREATE TABLE M_AVG_LOAD_AVERAGE_STATS(Time NUMBER(20), MemberId VARCHAR2(150), ClusterId VARCHAR2(150),
ClusterInstanceId VARCHAR2(150), NetworkPartitionId VARCHAR2(150), Value NUMERIC);
CREATE TABLE AVG_IN_FLIGHT_REQUESTS(Time NUMBER(20), ClusterId VARCHAR2(150), ClusterInstanceId VARCHAR2(150),
NetworkPartitionId VARCHAR2(150), Count NUMERIC);
CREATE TABLE SCALING_DETAILS(Time NUMBER(20), ScalingDecisionId VARCHAR2(150), ClusterId VARCHAR2(150),
MinInstanceCount INT, MaxInstanceCount INT, RIFPredicted INT, RIFThreshold INT, RIFRequiredInstances INT, MCPredicted
 INT, MCThreshold INT, MCRequiredInstances INT, LAPredicted INT, LAThreshold INT,LARequiredInstances INT,
 RequiredInstanceCount INT, ActiveInstanceCount INT, AdditionalInstanceCount INT, ScalingReason VARCHAR2(150));

