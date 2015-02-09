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

echo ${artifacts_path}/autoscale-policy.json
echo "Adding autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/autoscaling-policy.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies

echo "Adding tomcat cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/tomcat.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

echo "Adding tomcat1 cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/tomcat1.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

echo "Adding tomcat2 cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_path}/tomcat2.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges

echo "Adding group6c group..."
curl -X POST -H "Content-Type: application/json" -d "@${cartridges_groups_path}/group6c.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridgeGroups

sleep 1

echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d "@${artifacts_path}/application.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications

sleep 1

echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -d "@${iaas_artifacts_path}/deployment-policy.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/single_group_v2/deploy
