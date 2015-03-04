#!/bin/bash

host_ip="localhost"
host_port=9443

set -e

echo "Undeploying application..."
curl -X POST -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/group-scaling-v1/undeploy

sleep 10

echo "Deleting application..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/group-scaling-v1

echo "Removing groups..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridgeGroups/group2

echo "Removing cartridges..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges/c2
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges/c3

echo "Removing autoscaling policies..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies/autoscaling-policy-c2
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies/autoscaling-policy-c3

echo "Removing application policies..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applicationPolicies/application-policy-1

