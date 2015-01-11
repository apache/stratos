#!/bin/sh 

# Create autoscale policy
curl -X POST -H "Content-Type: application/json" -d @'autoscale-policy.json' -k -v -u admin:admin https://localhost:9443/api/autoscalingPolicies

# Create tomcat cartridge
curl -X POST -H "Content-Type: application/json" -d @'tomcat.json' -k -v -u admin:admin https://localhost:9443/api/cartridges

# Deploy tomcat1 cartride
curl -X POST -H "Content-Type: application/json" -d @'tomcat1.json' -k -v -u admin:admin https://localhost:9443/api/cartridges

# Deploy tomcat2 cartride
curl -X POST -H "Content-Type: application/json" -d @'tomcat2.json' -k -v -u admin:admin https://localhost:9443/api/cartridges

# Deploy group
curl -X POST -H "Content-Type: application/json" -d @'group6c.json' -k -v -u admin:admin https://localhost:9443/api/groups

curl -X POST -H "Content-Type: application/json" -d @'group8c.json' -k -v -u admin:admin https://localhost:9443/api/groups

# GET group
#curl -X GET -H "Content-Type: application/json" -k -v -u admin:admin https://localhost:9443/api/groups/group6


sleep 5
# Create application
curl -X POST -H "Content-Type: application/json" -d @'application_definition.json' -k -v -u admin:admin https://localhost:9443/api/applications

sleep 5
# GET application
# curl -X GET -H "Content-Type: application/json" -k -v -u admin:admin https://localhost:9443/api/applications/myapp1265

# Deploy application
#curl -X POST -H "Content-Type: application/json" -d@'deployment-policy.json' -k -v -u admin:admin https://localhost:9443/api/applicationDeployments

# Undeploy application
#curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://localhost:9443/api/applicationDeployments/myapp1265

# Delete application
#curl -X DELETE -H "Content-Type: application/json" -k -v -u admin:admin https://localhost:9443/api/applications/app_boo

sleep 3
#curl -X POST -H "Content-Type: application/json" -d@'grouping/dep_single_group.json' -k -v -u admin:admin https://localhost:9443/api/application/app_boo/deploy
curl -X POST -H "Content-Type: application/json" -d@'deployment-policy.json' -k -v -u admin:admin https://localhost:9443/api/applications/app_boo/deploy

