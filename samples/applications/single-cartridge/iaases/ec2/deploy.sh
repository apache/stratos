#!/bin/sh 

export iaas="ec2"						#[openstack, ec2, mock, kubernetes]
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
	# Adding tomcat cartridge
echo "Adding tomcat cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'tomcat.json' -k -u admin:admin https://${host_ip}:9443/api/cartridges
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
curl -X POST -H "Content-Type: application/json" -d@'artifacts/dep_single_group.json' -k -u admin:admin https://${host_ip}:9443/api/applications/single-cartridge-app/deploy

