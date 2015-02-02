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

echo ${artifacts_path}/autoscale-policy.json
echo "Adding autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/autoscaling-policy.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding php cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/php.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

sleep 1

echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/application.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications

sleep 1

echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -d "@${iaas_artifacts_path}/deployment-policy.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/single-cartridge-app/deploy
