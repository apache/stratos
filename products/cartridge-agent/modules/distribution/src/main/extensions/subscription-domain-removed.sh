#!/bin/bash
# --------------------------------------------------------------
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# --------------------------------------------------------------
# This extension script will be executed when subscription domain
# removed event is received.
# --------------------------------------------------------------
#

log=/var/log/apache-stratos/cartridge-agent-extensions.log
OUTPUT=`date`": Subscription Domain Removed Event"
OUTPUT="$OUTPUT STRATOS_SUBSCRIPTION_SERVICE_NAME: ${STRATOS_SUBSCRIPTION_SERVICE_NAME},"
OUTPUT="$OUTPUT STRATOS_SUBSCRIPTION_DOMAIN_NAME: ${STRATOS_SUBSCRIPTION_DOMAIN_NAME},"
OUTPUT="$OUTPUT STRATOS_SUBSCRIPTION_TENANT_ID: ${STRATOS_SUBSCRIPTION_TENANT_ID},"
OUTPUT="$OUTPUT APP_PATH: ${APP_PATH},"
OUTPUT="$OUTPUT STRATOS_SUBSCRIPTION_TENANT_DOMAIN: $STRATOS_SUBSCRIPTION_TENANT_DOMAIN}"
echo $OUTPUT | tee -a $log

curl -k -v -X POST -H "Content-Type:application/soap+xml;charset=UTF-8;action=urn:deleteHost" -d "<?xml version=\"1.0\" encoding=\"UTF-8\"?><s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://www.w3.org/2005/08/addressing\"><s:Body><p:deleteHost xmlns:p=\"http://mapper.url.carbon.wso2.org\"><xs:hostName xmlns:xs=\"http://mapper.url.carbon.wso2.org\">$STRATOS_SUBSCRIPTION_DOMAIN_NAME</xs:hostName></p:deleteHost></s:Body></s:Envelope>" https://localhost:9443/services/UrlMapperAdminService -u admin:admin
