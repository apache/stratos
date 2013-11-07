#!/bin/bash
# --------------------------------------------------------------
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# --------------------------------------------------------------

echo "Starting event publisher..."
lib_path=./../lib/
class_path=${lib_path}commons-io-2.0.jar:${lib_path}andes-client-0.13.wso2v8.jar:${lib_path}apache-stratos-event-publisher-4.0.0-SNAPSHOT.jar:${lib_path}commons-codec-1.8.jar:${lib_path}commons-logging-1.1.1.jar:${lib_path}geronimo-jms_1.1_spec-1.1.0.wso2v1.jar:${lib_path}geronimo-jms_1.1_spec-1.1.jar:${lib_path}gson-2.2.4.jar:${lib_path}log4j-1.2.13.jar:${lib_path}org.apache.log4j-1.2.13.v200706111418.jar:${lib_path}org.apache.stratos.messaging-4.0.0-SNAPSHOT.jar:${lib_path}org.wso2.carbon.logging-4.1.0.jar:${lib_path}slf4j-api-1.7.5.jar:${lib_path}slf4j-log4j12-1.7.5.jar

java -cp $class_path org.apache.stratos.cartridge.agent.event.publisher.Main $*
echo "Event publisher completed"

