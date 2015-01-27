#!/bin/sh

export host_ip="localhost"

set -e

# Undeploying application
echo "Undeploying application..."
curl -X POST -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:9443/api/applications/app_group_v1/undeploy

sleep 30

# Deleting application
echo "Deleting application..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:9443/api/applications/app_group_v1

# Removing cartridges
echo "Removing cartridges..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:9443/api/cartridges/tomcat
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:9443/api/cartridges/php

# Removing autoscale policies
echo "Removing autoscale policies..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:9443/api/autoscalingPolicies/autoscale_policy_1
