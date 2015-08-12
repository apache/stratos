#!/bin/sh

echo ${autoscaling_policies_path}/autoscaling-policy-1.json
echo "Adding autoscale policy..."
curl -X POST -H "Content-Type: application/json" -d "@economy-policy.json" -k -v -u admin:admin https://127.0.0.1:9443/api/autoscalingPolicies

echo "Adding network partitions..."
curl -X POST -H "Content-Type: application/json" -d "@RegionOne.json" -k -v -u admin:admin https://127.0.0.1:9443/api/networkPartitions

echo "Adding deployment policies..."
curl -X POST -H "Content-Type: application/json" -d "@static-1.json" -k -v -u admin:admin https://127.0.0.1:9443/api/deploymentPolicies

echo "Adding tomcat cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@c1.json" -k -v -u admin:admin https://127.0.0.1:9443/api/cartridges

echo "Adding tomcat1 cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@c2.json" -k -v -u admin:admin https://127.0.0.1:9443/api/cartridges

echo "Adding tomcat2 cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@c3.json" -k -v -u admin:admin https://127.0.0.1:9443/api/cartridges

echo "Adding tomcat2 cartridge..."
curl -X POST -H "Content-Type: application/json" -d "@c4.json" -k -v -u admin:admin https://127.0.0.1:9443/api/cartridges
curl -X POST -H "Content-Type: application/json" -d "@c5.json" -k -v -u admin:admin https://127.0.0.1:9443/api/cartridges


echo "Adding group6c group..."
curl -X POST -H "Content-Type: application/json" -d "@cartridge-groups.json" -k -v -u admin:admin https://127.0.0.1:9443/api/cartridgeGroups

sleep 1

echo "Adding application policy..."
curl -X POST -H "Content-Type: application/json" -d "@application-policy-1.json" -k -v -u admin:admin https://127.0.0.1:9443/api/applicationPolicies

sleep 1

echo "Creating application..."
curl -X POST -H "Content-Type: application/json" -d "@application-s-g-c1-c2-c3-s.json" -k -v -u admin:admin https://127.0.0.1:9443/api/applications

sleep 1

echo "Deploying application..."
curl -X POST -H "Content-Type: application/json" -k -v -u admin:admin https://127.0.0.1:9443/api/applications/s-g-c1-c2-c3-s/deploy/application-policy-1
