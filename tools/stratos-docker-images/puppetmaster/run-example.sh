#!/bin/bash
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

DOMAIN=example.com
MASTERHOSTNAME=puppet.example.com

MB_HOSTNAME=
MB_PORT=
TRUSTSTORE_PASSWORD=wso2carbon


# To run interactively:
# docker run -i -t -e "DOMAIN=${DOMAIN}" -e "MASTERHOSTNAME=${MASTERHOSTNAME}" -p 8140 apachestratos/puppet /bin/bash

PUPPET_ID=$(docker run -d --dns=${IP_ADDR} -e "DOMAIN=${DOMAIN}" -e "MASTERHOSTNAME=${MASTERHOSTNAME}" -p 8140 apachestratos/puppet)
PUPPET_IP_ADDR=$(docker inspect --format '{{ .NetworkSettings.Gateway }}' $PUPPET_ID)
PUPPET_PORT=$(docker port $PUPPET_ID 8140 | awk -F':' '{ print $2 }')

# TODO create a docker cartridge 
# add dns record for cartridge, e.g. cartridge1.$DOMAIN
# start puppet agent in cartridge to test puppetmaster:
# puppet agent --server ${MASTERHOSTNAME} --masterport ${PUPPET_PORT} --test --verbose --debug

