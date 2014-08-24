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

DOMAIN=stratos.com
MASTERHOSTNAME=puppet.stratos.com
MB_HOSTNAME=
MB_PORT=
TRUSTSTORE_PASSWORD=wso2carbon


# To run interactively:
# sudo docker run -i -t -e "DOMAIN=${DOMAIN}" -e "MASTERHOSTNAME=${MASTERHOSTNAME}" -p 8140 apachestratos/puppet /bin/bash

PUPPET_ID=$(sudo docker run -d -e "DOMAIN=${DOMAIN}" -e "MASTERHOSTNAME=${MASTERHOSTNAME}" -p 8140 apachestratos/puppet)
PUPPET_IP_ADDR=$(sudo docker inspect --format '{{ .NetworkSettings.Gateway }}' $PUPPET_ID)
PUPPET_PORT=$(sudo docker port $PUPPET_ID 8140 | awk -F':' '{ print $2 }')

echo Puppet container ID: ${PUPPET_ID}
echo Puppet master reachable on IP address: ${PUPPET_IP_ADDR}
echo Puppet master reachable on port: ${PUPPET_PORT}


echo Connection test:
echo puppet agent --server puppet.stratos.com --masterport ${PUPPET_PORT} --test --verbose --debug

echo View container logs:
echo sudo docker logs -f ${PUPPET_ID}
