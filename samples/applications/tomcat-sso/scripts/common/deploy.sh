#!/bin/bash

iaas=$1
host_ip="localhost"
host_port=9443

prgdir=`dirname "$0"`
script_path=`cd "$prgdir"; pwd`

artifacts_path=`cd "${script_path}/../../artifacts"; pwd`
iaas_artifacts_path=`cd "${script_path}/../../artifacts/${iaas}"; pwd`
iaas_cartridges_path=`cd "${script_path}/../../../../cartridges/${iaas}"; pwd`
cartridges_groups_path=`cd "${script_path}/../../../../cartridges-groups"; pwd`
autoscaling_policies_path=`cd "${script_path}/../../../../autoscaling-policies"; pwd`
network_partitions_path=`cd "${script_path}/../../../../network-partitions/${iaas}"; pwd`
deployment_policies_path=`cd "${script_path}/../../../../deployment-policies"; pwd`

set -e

if [[ -z "${iaas}" ]]; then
    echo "Usage: deploy.sh [iaas]"
    exit
fi

echo ${artifacts_path}/autoscaling-policy.json
echo "Adding autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d "@${autoscaling_policies_path}/autoscaling-policy-1.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding network partitions..."
curl -X POST -H "Content-Type: application/json" -d "@${network_partitions_path}/network-partition-3.json" -k -v -u admin:admin https://${host_ip}:9443/api/networkPartitions

echo "Adding deployment policy..."
curl -X POST -H "Content-Type: application/json" -d "@${deployment_policies_path}/deployment-policy-3.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/deploymentPolicies

echo "Adding tomcat cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${iaas_cartridges_path}/tomcat-sso.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

echo "Adding wso2-is cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${iaas_cartridges_path}/wso2-is.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

sleep 1

echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/application.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications

sleep 1

echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/application-policy.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/tomcat/deploy
