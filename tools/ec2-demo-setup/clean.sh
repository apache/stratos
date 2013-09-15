#!/bin/bash
# ----------------------------------------------------------------------------
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
# ----------------------------------------------------------------------------
#  Clean up script for Apache Stratos
# ----------------------------------------------------------------------------

# Make sure the user is not running as root.
if [ "$UID" == "0" ]; then
echo ; echo "  You must not run $0 as root." ; echo 
exit 69
fi

echo "Terminating all Java processes.. "
killall java
sleep 1;

echo "Cleaning up databases... "
mysqladmin -f -uroot -pg drop stratos_foundation
mysqladmin -f -uroot -pg drop userstore
mysql -uroot -pg </opt/resources/stratos_mysql.sql
mysql -uroot -pg </opt/resources/userstore.sql
sleep 1;

echo "Cleaning log files... "
rm -rf /opt/apache-stratos-elb-3.0.0-incubating/repository/logs/*
rm -rf /opt/apache-stratos-cc-3.0.0-incubating/repository/logs/*
rm -rf /opt/apache-stratos-agent-3.0.0-incubating/repository/logs/*
rm -rf /opt/apache-stratos-sc-3.0.0-incubating/repository/logs/*

echo "Cleaning Cartridge Agent's registrants... "
rm -rf /opt/apache-stratos-agent-3.0.0-incubating/registrants/*

echo "Removing zookeeper directory... "
rm -rf /opt/apache-stratos-cc-3.0.0-incubating/repository/data/*

echo "Cleaning topology file... "
rm -rf /opt/apache-stratos-cc-3.0.0-incubating/repository/conf/service-topology.conf*

echo "Cleaning service definition files... "
rm -rf /opt/apache-stratos-cc-3.0.0-incubating/repository/deployment/server/services/*

echo "Cleaning cartridge definition files..."
rm -rf /opt/apache-stratos-cc-3.0.0-incubating/repository/deployment/server/cartridges/*

echo "Cleaning Cloud Controller's registry... "
rm -rf /opt/apache-stratos-cc-3.0.0-incubating/repository/database/*
cp /opt/resources/WSO2CARBON_DB.h2.db /opt/apache-stratos-cc-3.0.0-incubating/repository/database/


