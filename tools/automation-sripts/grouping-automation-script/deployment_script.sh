#!/bin/sh 

curl -X POST -H "Content-Type: application/json" -d@'samples/ec2/p1.json' -k -v -u admin:admin https://localhost:9443/stratos/admin/policy/deployment/partition

sleep 15

curl -X POST -H "Content-Type: application/json" -d @'samples/ec2/autoscale-policy.json' -k -v -u admin:admin https://localhost:9443/stratos/admin/policy/autoscale

sleep 5

curl -X POST -H "Content-Type: application/json" -d@'samples/ec2/deployment-policy.json' -k -v -u admin:admin https://localhost:9443/stratos/admin/policy/deployment

sleep 5

curl -X POST -H "Content-Type: application/json" -d @'samples/ec2/php-cart.json' -k -v -u admin:admin https://localhost:9443/stratos/admin/cartridge/definition

sleep 5

curl -X POST -H "Content-Type: application/json" -d @'samples/ec2/tomcat.json' -k -v -u admin:admin https://localhost:9443/stratos/admin/cartridge/definition

sleep 5

curl -X POST -H "Content-Type: application/json" -d @'samples/ec2/tomcat1.json' -k -v -u admin:admin https://localhost:9443/stratos/admin/cartridge/definition


sleep 5

curl -X POST -H "Content-Type: application/json" -d @'samples/ec2/group1.json' -k -v -u admin:admin https://localhost:9443/stratos/admin/group/definition

sleep 5

curl -X POST -H "Content-Type: application/json" -d @'samples/ec2/group2.json' -k -v -u admin:admin https://localhost:9443/stratos/admin/group/definition

sleep 5

curl -X POST -H "Content-Type: application/json" -d @'samples/ec2/m2_single_subsciption_app.json' -k -v -u admin:admin https://localhost:9443/stratos/admin/application/definition
