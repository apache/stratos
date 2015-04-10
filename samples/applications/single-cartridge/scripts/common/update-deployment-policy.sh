#!/bin/bash

iaas=$1
host_ip="localhost"
host_port=9443

script_path=`cd "$prgdir"; pwd`

deployment_policies_path=`cd "${script_path}/../../../../deployment-policies"; pwd`

curl -X PUT -H "Content-Type: application/json" -d "@${deployment_policies_path}/deployment-policy-1.json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/deploymentPolicies

