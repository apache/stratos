#!/bin/sh 
# ----------------------------------------------------------------------------
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#
# ----------------------------------------------------------------------------

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
