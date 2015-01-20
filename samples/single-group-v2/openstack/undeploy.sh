#!/bin/sh 

curl -X POST -H "Content-Type: application/json" -k -v -u admin:admin https://localhost:9443/api/applications/app_group_v2/undeploy

