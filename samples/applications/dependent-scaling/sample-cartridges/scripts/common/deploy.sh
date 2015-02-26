#!/bin/bash

iaas=$1
host_ip="localhost"
host_port=9443

prgdir=`dirname "$0"`
script_path=`cd "$prgdir"; pwd`

artifacts_path=`cd "${script_path}/../../artifacts"; pwd`
iaas_artifacts_path=`cd "${script_path}/../../artifacts/${iaas}"; pwd`
cartridges_path=`cd "${script_path}/../../../../../cartridges/${iaas}"; pwd`
cartridges_groups_path=`cd "${script_path}/../../../../../cartridges-groups"; pwd`

set -e

if [[ -z "${iaas}" ]]; then
    echo "Usage: deploy.sh [iaas]"
    exit
fi

echo ${artifacts_path}/autoscaling-policy.json
echo "Adding autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/autoscaling-policy.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding network partitions..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/network-partition.json" -k -v -u admin:admin https://${host_ip}:9443/api/networkPartitions
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/network-partition-2.json" -k -v -u admin:admin https://${host_ip}:9443/api/networkPartitions

echo "Adding deployment policy..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/deployment-policy.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/deploymentPolicies

echo "Adding tomcat cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/tomcat.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

echo "Adding php cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/php.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

sleep 1

echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/application.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications

sleep 1

echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/application-policy.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/dependency-scaling-cartridges-app/deploy