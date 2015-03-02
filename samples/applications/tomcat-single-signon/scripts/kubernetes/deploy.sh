#!/bin/bash

host_ip="localhost"
host_port=9443
iaas="kubernetes"

prgdir=`dirname "$0"`
script_path=`cd "$prgdir"; pwd`
common_folder=`cd "${script_path}/../common"; pwd`
kubernetes_clusters_path=`cd "${script_path}/../../../../kubernetes-clusters"; pwd`


echo "Adding kubernetes cluster..."
curl -X POST -H "Content-Type: application/json" -d "@${kubernetes_clusters_path}/kubernetes-cluster-1.json" -k -u admin:admin https://${host_ip}:${host_port}/api/kubernetesClusters

bash ${common_folder}/deploy.sh ${iaas}