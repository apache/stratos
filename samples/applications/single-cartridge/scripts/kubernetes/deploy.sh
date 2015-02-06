#!/bin/bash

host_ip="localhost"
host_port=9443
iaas="kubernetes"
script_path=""
if [ "$(uname)" == "Darwin" ]; then
    script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    script_path="$( cd -P "$( dirname "$SOURCE" )" && pwd )/`dirname $0`"
else
   echo "Unknown operating system"
   exit
fi
common_folder="${script_path}/../common"
iaas_artifacts_path="${script_path}/../../artifacts/${iaas}"

echo "Adding kubernetes cluster..."
curl -X POST -H "Content-Type: application/json" -d "@${iaas_artifacts_path}/kubernetes-cluster.json" -k -u admin:admin https://${host_ip}:${host_port}/api/kubernetesClusters

pushd ${common_folder}
bash deploy.sh ${iaas}
popd