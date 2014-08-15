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
# This extension script will be executed when complete topology 
# event is received.
# --------------------------------------------------------------
#

log=/var/log/apache-stratos/cartridge-agent-extensions.log
echo `date`": Complete Topology Event: " | tee -a $log
echo "LB IP: ${STRATOS_LB_IP}" | tee -a $log
echo "LB PUBLIC IP: $STRATOS_LB_PUBLIC_IP}" | tee -a $log
echo "STRATOS_PARAM_FILE_PATH: ${STRATOS_PARAM_FILE_PATH}"
echo "Member List: ${STRATOS_MEMBER_LIST_JSON}" | tee -a $log
echo "Complete Topology: ${STRATOS_TOPOLOGY_JSON}" | tee -a $log
echo "Members in LB: ${STRATOS_MEMBERS_IN_LB_JSON}" | tee -a $log
