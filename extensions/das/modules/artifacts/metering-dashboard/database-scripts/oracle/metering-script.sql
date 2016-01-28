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

CREATE TABLE MEMBER_STATUS(Time NUMBER(20), ApplicationId VARCHAR2(150), ClusterAlias VARCHAR2(150), MemberId VARCHAR2
(150), MemberStatus VARCHAR2(50));
CREATE TABLE MEMBER_COUNT(Time NUMBER(20), ApplicationId VARCHAR2(150), ClusterAlias VARCHAR2(150),
CreatedInstanceCount NUMBER(10), InitializedInstanceCount NUMBER(10), ActiveInstanceCount NUMBER(10),
TerminatedInstanceCount NUMBER(10));
CREATE TABLE MEMBER_INFORMATION(MemberId VARCHAR2(150), InstanceType VARCHAR2(150), ImageId VARCHAR2(150), HostName
VARCHAR2(150), PrivateIPAddresses VARCHAR2(150), PublicIPAddresses VARCHAR2(150), Hypervisor VARCHAR2(150), CPU
VARCHAR2(10), RAM VARCHAR2(10), OSName VARCHAR2(150), OSVersion VARCHAR2(150));


