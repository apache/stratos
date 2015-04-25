#!/bin/bash

iaas=$1
host_ip="localhost"
host_port=9443

script_path=`cd "$prgdir"; pwd`

network_partitions_path=`cd "${script_path}/../../../../network-partitions/${iaas}"; pwd`

curl -X PUT -H "Content-Type: application/json" -d "@${network_partitions_path}/network-partition-2.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/networkPartitions
