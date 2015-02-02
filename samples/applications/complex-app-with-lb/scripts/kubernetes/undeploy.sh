#!/bin/bash

host_ip="localhost"
host_port=9443
script_path="$( cd -P "$( dirname "$SOURCE" )" && pwd )/`dirname $0`"
common_folder="${script_path}/../common"

pushd ${common_folder}
bash undeploy.sh
popd

echo "Removing kubernetes cluster..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/kubernetesClusters/kubernetes-cluster-1
