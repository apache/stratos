#!/bin/bash

host_ip="localhost"
host_port=9443
iaas="kubernetes"

prgdir=`dirname "$0"`
script_path=`cd "$prgdir"; pwd`

common_folder=`cd "${script_path}/../common"; pwd`
iaas_artifacts_path=`cd "${script_path}/../../artifacts/${iaas}"; pwd`

echo "Adding kubernetes cluster..."
curl -X POST -H "Content-Type: application/json" -d "@${iaas_artifacts_path}/kubernetes-cluster.json" -k -u admin:admin https://${host_ip}:${host_port}/api/kubernetesClusters

bash ${common_folder}/deploy.sh ${iaas}