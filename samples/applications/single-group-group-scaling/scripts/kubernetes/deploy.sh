#!/bin/bash

host_ip="localhost"
host_port=9443
iaas="kubernetes"
script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
common_folder="${script_path}/../common"
iaas_artifacts_path="${script_path}/../../artifacts/${iaas}"

echo "Adding kubernetes cluster..."
curl -X POST -H "Content-Type: application/json" -d "@${iaas_artifacts_path}/kubernetes-cluster.json" -k -u admin:admin https://${host_ip}:${host_port}/api/kubernetesClusters

pushd ${common_folder}
bash deploy.sh ${iaas}
popd