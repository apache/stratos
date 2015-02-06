#!/bin/bash

host_ip="localhost"
host_port=9443

prgdir=`dirname "$0"`
script_path=`cd "$prgdir"; pwd`
common_folder="${script_path}/../common"

bash ${common_folder}/undeploy.sh

echo "Removing kubernetes cluster..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/kubernetesClusters/kubernetes-cluster-1