#!/bin/sh 

# Undeploy application
curl -X POST -H "Content-Type: application/json" -k -v -u admin:admin https://localhost:9443/api/applications/myapp1265/undeploy
