#!/bin/sh 

export iaas="kubernetes"						#[openstack, ec2, mock, kubernetes]
export host_ip="localhost"

export artifacts_path="../../artifacts"
export cartridges_path="../../../../cartridges/${iaas}"
export cartridges_groups_path="../../../../cartridges-groups"

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
popd

sleep 3

# Adding kubernetes cluster
pushd ${cartridges_groups_path}
echo "Adding kubernetes cluster..."
curl -X POST -H "Content-Type: application/json" -d @'artifacts/kubernetes-cluster.json' -k -u admin:admin https://${host_ip}:9443/api/kubernetesClusters
popd

sleep 2

# Adding groups
	# Adding group6c4
echo "Adding group6c4 group..."
curl -X POST -H "Content-Type: application/json" -d @'group6c4.json' -k -v -u admin:admin https://${host_ip}:9443/api/cartridgeGroups

sleep 3

# Creating application
pushd ${artifacts_path}
echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d @'app_single_group.json' -k -v -u admin:admin https://${host_ip}:9443/api/applications
popd

sleep 3

# Deploy application
echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -d@'artifacts/dep_single_group.json' -k -v -u admin:admin https://${host_ip}:9443/api/applications/app_group_v1/deploy


