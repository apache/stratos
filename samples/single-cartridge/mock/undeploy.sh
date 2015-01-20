#!/bin/sh 

echo "undeploying application..."
curl -X POST -H "Content-Type: application/json" -k -u admin:admin https://localhost:9443/api/applications/single-cartridge-app/undeploy

