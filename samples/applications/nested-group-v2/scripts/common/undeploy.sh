#!/bin/bash

host_ip="localhost"
host_port=9443

set -e

echo "Undeploying application..."
curl -X POST -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/nested-group-app/undeploy

sleep 10

echo "Deleting application..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/nested-group-app

echo "Removing groups..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridgeGroups/group6

echo "Removing cartridges..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges/tomcat
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges/tomcat1
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/cartridges/tomcat2

echo "Removing autoscale policies..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/autoscalingPolicies/autoscaling-policy-5

echo "Removing application policies..."
curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applicationPolicies/application-policy-5

