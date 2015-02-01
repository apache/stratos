#!/bin/bash

export iaas="mock"						#[openstack, ec2, mock, kubernetes]
export host_ip="localhost"

export artifacts_path="../../artifacts"
export cartridges_path="../../../../cartridges/${iaas}"

set -e

# Adding autoscale policy
pushd ${artifacts_path}
echo "Adding autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d @'autoscale-policy.json' -k -u admin:admin https://${host_ip}:9443/api/autoscalingPolicies
popd

sleep 1

# Adding cartridges
pushd ${cartridges_path}
	# Adding php cartridge
echo "Adding php cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'php.json' -k -u admin:admin https://${host_ip}:9443/api/cartridges
popd

sleep 5

# Creating application
pushd ${artifacts_path}
echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d @'application.json' -k -u admin:admin https://${host_ip}:9443/api/applications
popd

sleep 5 

# Deploy application
echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -d@'artifacts/deployment-policy.json' -k -u admin:admin https://${host_ip}:9443/api/applications/single-cartridge-app/deploy

