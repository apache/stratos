#!/bin/bash

export host_ip="localhost"
export artifacts_path="../../artifacts"

pushd ${artifacts_path}
echo "Adding domain mappings..."
curl -X POST -H "Content-Type: application/json" -d @'domain-mappings.json' -k -u admin:admin https://${host_ip}:9443/api/applications/single-cartridge-app/domainMappings
popd