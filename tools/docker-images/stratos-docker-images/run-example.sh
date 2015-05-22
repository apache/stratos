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

# This script runs activemq, mysql and docker containers
# change the environment variables to reflect your environment

# Set the DOMAIN to the dns domain name you want to use for your Stratos environment
# The DNS domain name is only used internally by the stratos docker images and should not be exposed publically
export DOMAIN=example.com
 
# Set the IP_ADDR to the IP address you use to reach the docker host
export IP_ADDR=192.168.56.5

# Set the version of Stratos docker images
export STRATOS_VERSION=4.1.0

########
# Bind
########
 
# We need to grant access to hosts or networks to allow DNS records to be added from those hosts
# See http://www.zytrax.com/books/dns/ch7/address_match_list.html for more info
export UPDATE_ADDR_LIST=any
 
export BIND_ID=$(docker run -d -p 53:53/udp -e "DOMAIN=$DOMAIN" -e "IP_ADDR=$IP_ADDR" -e "UPDATE_ADDR_LIST=$UPDATE_ADDR_LIST" apachestratos/bind:$STRATOS_VERSION); sleep 2s;
export BIND_IP_ADDR=$(docker inspect --format '{{ .NetworkSettings.Gateway }}' $BIND_ID)

###########
# ActiveMQ
###########

export MB_ID=$(docker run -p 61616 -d apachestratos/activemq:$STRATOS_VERSION); sleep 2s;
export MB_IP_ADDR=$(docker inspect --format '{{ .NetworkSettings.Gateway }}' $MB_ID)
export MB_PORT=$(docker port $MB_ID 61616 | awk -F':' '{ print $2 }')

###############
# PuppetMaster
###############

# Create a file containing instructions for the nsupdate tool
cat > addpuppetdomain.txt <<EOF
server 127.0.0.1
zone $DOMAIN
prereq nxdomain puppet.$DOMAIN.
update add puppet.$DOMAIN. 10  A $IP_ADDR
send
EOF
 
# Run the nsupdate tool to add puppetmaster to the domain
nsupdate addpuppetdomain.txt
rm -f addpuppetdomain.txt

export MASTERHOSTNAME=puppet.$DOMAIN
export TRUSTSTORE_PASSWORD=wso2carbon
 
export PUPPET_ID=$(docker run -d -h ${MASTERHOSTNAME} --dns=${BIND_IP_ADDR} -e "DOMAIN=${DOMAIN}" -e "MASTERHOSTNAME=${MASTERHOSTNAME}" -e "MB_HOSTNAME=${MB_IP_ADDR}" -e "MB_PORT=${MB_PORT}" -e "TRUSTSTORE_PASSWORD=${TRUSTSTORE_PASSWORD}" -p 8140 apachestratos/puppetmaster:$STRATOS_VERSION); sleep 2s;
export PUPPET_IP_ADDR=$(docker inspect --format '{{ .NetworkSettings.Gateway }}' $PUPPET_ID)
export PUPPET_PORT=$(docker port $PUPPET_ID 8140 | awk -F':' '{ print $2 }')

########
# MySQL
########

export USERSTORE_ID=$(docker run -d -p 3306 -e MYSQL_ROOT_PASSWORD=password apachestratos/mysql:$STRATOS_VERSION); sleep 2s;
export USERSTORE_IP_ADDR=$(docker inspect --format '{{ .NetworkSettings.Gateway }}' $USERSTORE_ID)
export USERSTORE_PORT=$(docker port $USERSTORE_ID 3306 | awk -F':' '{ print $2 }')

##########
# Stratos
##########

unset docker_env # Ensure docker environment variable is clean to start with
 
# Database Settings
docker_env+=(-e "USERSTORE_DB_HOSTNAME=${USERSTORE_IP_ADDR}")
docker_env+=(-e "USERSTORE_DB_PORT=${USERSTORE_PORT}")
docker_env+=(-e "USERSTORE_DB_SCHEMA=USERSTORE_DB_SCHEMA")
docker_env+=(-e "USERSTORE_DB_USER=root")
docker_env+=(-e "USERSTORE_DB_PASS=password")
 
# Puppet Setings
docker_env+=(-e "PUPPET_IP=${IP_ADDR}")
docker_env+=(-e "PUPPET_HOSTNAME=${MASTERHOSTNAME}")
docker_env+=(-e "PUPPET_ENVIRONMENT=none")
 
# MB Settings
docker_env+=(-e "MB_HOSTNAME=${MB_IP_ADDR}")
docker_env+=(-e "MB_PORT=${MB_PORT}")

docker_env+=(-e "EC2_ENABLED=true")
docker_env+=(-e "EC2_IDENTITY=none")
docker_env+=(-e "EC2_CREDENTIAL=none")
docker_env+=(-e "EC2_CREDENTIAL=none")
docker_env+=(-e "EC2_OWNER_ID=none")
docker_env+=(-e "EC2_AVAILABILITY_ZONE=none")
docker_env+=(-e "EC2_SECURITY_GROUPS=none")
docker_env+=(-e "EC2_KEYPAIR=none")
 
docker_env+=(-e "OPENSTACK_ENABLED=false")
docker_env+=(-e "OPENSTACK_IDENTITY=none")
docker_env+=(-e "OPENSTACK_CREDENTIAL=none")
docker_env+=(-e "OPENSTACK_ENDPOINT=none")
 
docker_env+=(-e "VCLOUD_ENABLED=false")
docker_env+=(-e "VCLOUD_IDENTITY=none")
docker_env+=(-e "VCLOUD_CREDENTIAL=none")
docker_env+=(-e "VCLOUD_ENDPOINT=none")

# Stratos Settings [STRATOS_PROFILE=default|cc|as|sm]
docker_env+=(-e "STRATOS_PROFILE=default")

# Start Stratos container as daemon
STRATOS_ID=$(docker run -d "${docker_env[@]}" -p 9443:9443 --dns=${BIND_IP_ADDR} apachestratos/stratos:$STRATOS_VERSION)

echo ==============================================
echo Starting Stratos docker images. 
echo
echo To view the stratos log file, run the command:
echo 
echo docker logs -f $STRATOS_ID
echo ==============================================

# To run stratos interactively - e.g. for debugging
# docker run -i -t "${docker_env[@]}" -p 9443:9443 --dns=${BIND_IP_ADDR} apachestratos/stratos:$STRATOS_VERSION /bin/bash
