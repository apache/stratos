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
#

iaas=$1
host_ip="localhost"
host_port=9443

prgdir=`dirname "$0"`
script_path=`cd "$prgdir"; pwd`

artifacts_path=`cd "${script_path}/../../artifacts"; pwd`
iaas_artifacts_path=`cd "${script_path}/../../artifacts/${iaas}"; pwd`
cartridges_path=`cd "${script_path}/../../../../../cartridges/${iaas}"; pwd`
cartridges_groups_path=`cd "${script_path}/../../../../../cartridges-groups"; pwd`
autoscaling_policies_path=`cd "${script_path}/../../../../../autoscaling-policies"; pwd`
network_partitions_path=`cd "${script_path}/../../../../../network-partitions/${iaas}"; pwd`
deployment_policies_path=`cd "${script_path}/../../../../../deployment-policies"; pwd`
application_policies_path=`cd "${script_path}/../../../../../application-policies"; pwd`

set -e

if [[ -z "${iaas}" ]]; then
    echo "Usage: deploy.sh [iaas]"
    exit
fi

echo ${autoscaling_policies_path}/autoscaling-policy-1.json
echo "Adding autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d "@${autoscaling_policies_path}/autoscaling-policy-1.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding network partitions..."
curl -X POST -H "Content-Type: application/json" -d "@${network_partitions_path}/network-partition-1.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/networkPartitions
curl -X POST -H "Content-Type: application/json" -d "@${network_partitions_path}/network-partition-2.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/networkPartitions

echo "Adding deployment policies..."
curl -X POST -H "Content-Type: application/json" -d "@${deployment_policies_path}/deployment-policy-1.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/deploymentPolicies
curl -X POST -H "Content-Type: application/json" -d "@${deployment_policies_path}/deployment-policy-2.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/deploymentPolicies

echo "Adding mysql cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/mysql.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/postgres.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/esb.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/tomcat.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

echo "Adding php cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/php.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

echo "Adding mysql-php-group group..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_groups_path}/app-group.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridgeGroups
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_groups_path}/db-group.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridgeGroups

sleep 1

echo "Adding application policy..."
curl -X POST -H "Content-Type: application/json" -d "@${application_policies_path}/application-policy-1.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applicationPolicies

sleep 1

echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/application.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications

sleep 1

echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/my-compositeapp/deploy/application-policy-1

