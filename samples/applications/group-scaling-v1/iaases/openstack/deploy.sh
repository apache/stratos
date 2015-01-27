#!/bin/sh

export iaas="openstack"						#[openstack, ec2, mock, kubernetes]
export host_ip="localhost"

export artifacts_path="../../artifacts"
export cartridges_path="../../../../cartridges/${iaas}"
export cartridges_groups_path="../../../../cartridges-groups"

set -e

# Adding autoscale policy
    # Adding c1 autoscale policy
echo "Adding c1 autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d @'autoscale-policy-c1.json' -k -v -u admin:admin https://${host_ip}:9443/api/autoscalingPolicies

    # Adding c2 autoscale policy
echo "Adding c2 autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d @'autoscale-policy-c2.json' -k -v -u admin:admin https://${host_ip}:9443/api/autoscalingPolicies

    # Adding c3 autoscale policy
echo "Adding c3 autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d @'autoscale-policy-c3.json' -k -v -u admin:admin https://${host_ip}:9443/api/autoscalingPolicies

    # Adding c4 autoscale policy
echo "Adding c4 autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d @'autoscale-policy-c4.json' -k -v -u admin:admin https://${host_ip}:9443/api/autoscalingPolicies


# Adding cartridges
pushd ${cartridges_path}
	# Adding c3 cartridge
echo "Adding c3 cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'c3.json' -k -v -u admin:admin https://${host_ip}:9443/api/cartridges

	# Adding c4 cartridge
echo "Adding c4 cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'c4.json' -k -v -u admin:admin https://${host_ip}:9443/api/cartridges

	# Adding c1 cartridge
echo "Adding c1 cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'c1.json' -k -v -u admin:admin https://${host_ip}:9443/api/cartridges

	# Adding c2 cartridge
echo "Adding c2 cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'c2.json' -k -v -u admin:admin https://${host_ip}:9443/api/cartridges
popd

# Adding groups
pushd ${cartridges_groups_path}
	# Adding group1b
echo "Adding group1b group..."
curl -X POST -H "Content-Type: application/json" -d @'group1b.json' -k -v -u admin:admin https://${host_ip}:9443/api/cartridgeGroups
popd

sleep 3

# Creating application
echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d @'composite_application.json' -k -v -u admin:admin https://${host_ip}:9443/api/applications

sleep 3
# Deploy application
echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -d@'artifacts/app_deployment_policy.json' -k -v -u admin:admin https://${host_ip}:9443/api/applications/appscaling/deploy

