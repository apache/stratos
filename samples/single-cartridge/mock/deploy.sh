#!/bin/sh 


curl -X POST -H "Content-Type: application/json" -d @'artifacts/autoscale-policy.json' -k -v -u admin:admin https://localhost:9443/api/autoscalingPolicies

curl -X POST -H "Content-Type: application/json" -d @'artifacts/tomcat.json' -k -v -u admin:admin https://localhost:9443/api/cartridges

sleep 5

curl -X POST -H "Content-Type: application/json" -d @'artifacts/app_single_group.json' -k -v -u admin:admin https://localhost:9443/api/applications


sleep 5 

curl -X POST -H "Content-Type: application/json" -d@'artifacts/dep_single_group.json' -k -v -u admin:admin https://localhost:9443/api/applications/app_cartridge_v1/deploy
