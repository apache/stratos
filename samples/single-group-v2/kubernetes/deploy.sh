#!/bin/sh 


curl -X POST -H "Content-Type: application/json" -d @'artifacts/autoscale-policy.json' -k -v -u admin:admin https://localhost:9443/api/autoscalingPolicies

curl -X POST -H "Content-Type: application/json" -d @'artifacts/tomcat.json' -k -v -u admin:admin https://localhost:9443/api/cartridges
curl -X POST -H "Content-Type: application/json" -d @'artifacts/tomcat1.json' -k -v -u admin:admin https://localhost:9443/api/cartridges
curl -X POST -H "Content-Type: application/json" -d @'artifacts/tomcat2.json' -k -v -u admin:admin https://localhost:9443/api/cartridges
curl -X POST -H "Content-Type: application/json" -d @'artifacts/group6c.json' -k -v -u admin:admin https://localhost:9443/api/cartridgeGroups

sleep 3

curl -X POST -H "Content-Type: application/json" -d @'artifacts/app_single_group.json' -k -v -u admin:admin https://localhost:9443/api/applications

sleep 3
curl -X POST -H "Content-Type: application/json" -d@'artifacts/dep_single_group.json' -k -v -u admin:admin https://localhost:9443/api/applications/app_group_v2/deploy

