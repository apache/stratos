#!/bin/bash

iaas=$1
host_ip="localhost"
host_port=9443

set -e

echo "Getting application runtime..."
curl -X GET -H "Content-Type: application/json" -k -v -u admin:admin https://${host_ip}:${host_port}/api/applications/single-cartridge-app/runtime
