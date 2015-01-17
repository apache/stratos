#!/bin/sh 

# Create autoscale policy
curl -X POST -H "Content-Type: application/json" -d @'artifacts/autoscale-policy-c1.json' -k -v -u admin:admin https://localhost:9443/api/autoscalingPolicies
curl -X POST -H "Content-Type: application/json" -d @'artifacts/autoscale-policy-c2.json' -k -v -u admin:admin https://localhost:9443/api/autoscalingPolicies
curl -X POST -H "Content-Type: application/json" -d @'artifacts/autoscale-policy-c3.json' -k -v -u admin:admin https://localhost:9443/api/autoscalingPolicies
curl -X POST -H "Content-Type: application/json" -d @'artifacts/autoscale-policy-c4.json' -k -v -u admin:admin https://localhost:9443/api/autoscalingPolicies

# Deploy c3 cartridge
curl -X POST -H "Content-Type: application/json" -d @'artifacts/c3.json' -k -v -u admin:admin https://localhost:9443/api/cartridges

# Deploy c4 cartridge
curl -X POST -H "Content-Type: application/json" -d @'artifacts/c4.json' -k -v -u admin:admin https://localhost:9443/api/cartridges

# Deploy c1 cartride
curl -X POST -H "Content-Type: application/json" -d @'artifacts/c1.json' -k -v -u admin:admin https://localhost:9443/api/cartridges

# Deploy c2 cartride
curl -X POST -H "Content-Type: application/json" -d @'artifacts/c2.json' -k -v -u admin:admin https://localhost:9443/api/cartridges

# Deploy group
curl -X POST -H "Content-Type: application/json" -d @'artifacts/group1.json' -k -v -u admin:admin https://localhost:9443/api/cartridgeGroups

sleep 3
# Create application
curl -X POST -H "Content-Type: application/json" -d @'artifacts/composite_application.json' -k -v -u admin:admin https://localhost:9443/api/applications

sleep 3
# Deploy application
curl -X POST -H "Content-Type: application/json" -d@'artifacts/app_deployment_policy.json' -k -v -u admin:admin https://localhost:9443/api/applications/appscaling/deploy

