#!/bin/bash

export host_ip="localhost"
export artifacts_path="../../artifacts"

pushd ${artifacts_path}
echo "Adding network partition..."
curl -X POST -H "Content-Type: application/json" -d @'network-partition.json' -kv -u admin:admin https://${host_ip}:9443/api/networkPartitions
popd