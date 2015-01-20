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

# Persists the payload parameters by storing the environment variables in the launch-params file

echo "APPLICATION_ID=${APPLICATION_ID},SERVICE_NAME=${SERVICE_NAME},HOST_NAME=${HOST_NAME},MULTITENANT=false,TENANT_ID=${TENANT_ID},TENANT_RANGE=*,CARTRIDGE_ALIAS=${CARTRIDGE_ALIAS},CLUSTER_ID=${CLUSTER_ID},CLUSTER_INSTANCE_ID=${CLUSTER_INSTANCE_ID},CARTRIDGE_KEY=${CARTRIDGE_KEY},DEPLOYMENT=${DEPLOYMENT},REPO_URL=${REPO_URL},PORTS=${PORTS},PUPPET_IP=${PUPPET_IP},PUPPET_HOSTNAME=${PUPPET_HOSTNAME},PUPPET_ENV=${PUPPET_ENV},MEMBER_ID=${MEMBER_ID},LB_CLUSTER_ID=${LB_CLUSTER_ID},NETWORK_PARTITION_ID=${NETWORK_PARTITION_ID},PARTITION_ID=${PARTITION_ID},MIN_COUNT=${MIN_COUNT},INTERNAL=${INTERNAL},CLUSTERING_PRIMARY_KEY=${CLUSTERING_PRIMARY_KEY}" >> /mnt/apache-stratos-python-cartridge-agent-4.1.0-SNAPSHOT/payload/launch-params
