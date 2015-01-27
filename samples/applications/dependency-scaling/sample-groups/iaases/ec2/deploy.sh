#!/bin/sh 

export iaas="ec2"						#[openstack, ec2, mock, kubernetes]
export host_ip="localhost"

export artifacts_path="../../artifacts"
export cartridges_path="../../../../../cartridges/${iaas}"
export cartridges_groups_path="../../../../../cartridges-groups"

set -e

# Adding autoscale policy
pushd ${artifacts_path}
echo "Adding autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d @'autoscale-policy.json' -k -v -u admin:admin https://${host_ip}:9443/api/autoscalingPolicies
popd

# Adding cartridges
pushd ${cartridges_path}
	# Adding tomcat cartridge
echo "Adding tomcat cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'tomcat.json' -k -v -u admin:admin https://${host_ip}:9443/api/cartridges

	# Adding esb cartridge
echo "Adding esb cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'esb.json' -k -v -u admin:admin https://${host_ip}:9443/api/cartridges

	# Adding php cartridge
echo "Adding php cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'php.json' -k -v -u admin:admin https://${host_ip}:9443/api/cartridges
popd

# Adding groups
pushd ${cartridges_groups_path}
	# Adding group6c2
echo "Adding group6c2 group..."
curl -X POST -H "Content-Type: application/json" -d @'group6c2.json' -k -v -u admin:admin https://${host_ip}:9443/api/cartridgeGroups
popd

sleep 3

# Creating application
pushd ${artifacts_path}
echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d @'app_single_group.json' -k -v -u admin:admin https://${host_ip}:9443/api/applications
popd

sleep 3

# Deploy application
echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -d@'artifacts/dep_single_group.json' -k -v -u admin:admin https://${host_ip}:9443/api/applications/app_group_v2/deploy

