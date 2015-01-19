#!/bin/sh 

echo "Adding autoscaling policy..."
curl -X POST -H "Content-Type: application/json" -d @'artifacts/autoscale-policy.json' -k -v -u admin:admin https://localhost:9443/api/autoscalingPolicies

echo "Adding tomcat cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'artifacts/tomcat.json' -k -v -u admin:admin https://localhost:9443/api/cartridges

echo "Adding php cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'artifacts/php.json' -k -v -u admin:admin https://localhost:9443/api/cartridges

sleep 3
echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d @'artifacts/app_dependency_scaling.json' -k -v -u admin:admin https://localhost:9443/api/applications

sleep 3
echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -d@'artifacts/dep_dependency_scaling.json' -k -v -u admin:admin https://localhost:9443/api/applications/app_group_v1/deploy

