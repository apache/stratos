#!/bin/sh 

echo "Adding autoscaling policy..."
curl -X POST -H "Content-Type: application/json" -d @'artifacts/autoscale-policy.json' -k -u admin:admin https://localhost:9443/api/autoscalingPolicies

sleep 1

echo "Adding php cartridge..."
curl -X POST -H "Content-Type: application/json" -d @'artifacts/php-cartridge.json' -k -u admin:admin https://localhost:9443/api/cartridges

sleep 1

echo "Adding kubernetes cluster..."
curl -X POST -H "Content-Type: application/json" -d @'artifacts/kubernetes-cluster.json' -k -u admin:admin https://localhost:9443/api/kubernetesClusters

sleep 5

echo "Adding application..."
curl -X POST -H "Content-Type: application/json" -d @'artifacts/application.json' -k -u admin:admin https://localhost:9443/api/applications

sleep 5

echo "Deploying application..." 
curl -X POST -H "Content-Type: application/json" -d@'artifacts/deployment-policy.json' -k -v -u admin:admin https://localhost:9443/api/applications/single-cartridge-app/deploy

