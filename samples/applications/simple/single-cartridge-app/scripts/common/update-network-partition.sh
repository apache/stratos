#!/bin/bash

iaas=$1
host_ip="localhost"
host_port=9443

script_path=`cd "$prgdir"; pwd`
network_partitions_path=`cd "${script_path}/../../../../../network-partitions/${iaas}"; pwd`
echo "Updating network partition..."
curl -X PUT -H "Content-Type: application/json" -d "@${network_partitions_path}/network-partition-1.json" -k -v -u admin:admin https://${host_ip}:9443/api/networkPartitions
