#!/bin/bash

iaas=$1
host_ip="localhost"
host_port=9443

script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
artifacts_path="${script_path}/../../artifacts"
iaas_artifacts_path="${script_path}/../../artifacts/${iaas}"
cartridges_path="${script_path}/../../../../cartridges/${iaas}"
cartridges_groups_path="${script_path}/../../../../cartridges-groups"

set -e

if [[ -z "${iaas}" ]]; then
    echo "Usage: deploy.sh [iaas]"
    exit
fi

echo "Adding autoscale policy c1..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/autoscaling-policy-c1.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding autoscale policy c2..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/autoscaling-policy-c2.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding autoscale policy c3..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/autoscaling-policy-c3.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding autoscale policy c4..."
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
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/composite_application.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications

sleep 1

echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -d "@${iaas_artifacts_path}/app_deployment_policy.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/complex-app/deploy
