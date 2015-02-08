#!/bin/bash

iaas=$1
host_ip="localhost"
host_port=9443

prgdir=`dirname "$0"`
script_path=`cd "$prgdir"; pwd`

artifacts_path=`cd "${script_path}/../../artifacts"; pwd`
iaas_artifacts_path=`cd "${script_path}/../../artifacts/${iaas}"; pwd`
cartridges_path=`cd "${script_path}/../../../../cartridges/${iaas}"; pwd`
cartridges_groups_path=`cd "${script_path}/../../../../cartridges-groups"; pwd`

set -e

if [[ -z "${iaas}" ]]; then
    echo "Usage: deploy.sh [iaas]"
    exit
fi

echo "Adding autoscaling policy c1..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/autoscaling-policy-c1.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding autoscaling policy c2..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/autoscaling-policy-c2.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding autoscaling policy c3..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/autoscaling-policy-c3.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding autoscaling policy c4..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/autoscaling-policy-c4.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding c1 cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/c1.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

echo "Adding c2 cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/c2.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

echo "Adding c3 cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/c3.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

echo "Adding c4 cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/c4.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

echo "Adding group1 group..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_groups_path}/group1.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridgeGroups

sleep 1

echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/application.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications

sleep 1

echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -d "@${iaas_artifacts_path}/deployment-policy.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/group-scaling/deploy
