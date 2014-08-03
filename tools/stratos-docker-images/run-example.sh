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
# change the docker-env environment variables to reflect your environment

#
# Start activemq docker container 
# (skip this step if you already have activemq installed)
#

MB_ID=$(sudo docker run -p 61616 -d apachestratos/activemq); sleep 2s;
MB_IP_ADDR=$(sudo docker inspect --format '{{ .NetworkSettings.Gateway }}' $MB_ID)
MB_PORT=$(sudo docker port $MB_ID 61616 | awk -F':' '{ print $2 }')

#
# Start Puppet Container
#

DOMAIN=stratos.com
MASTERHOSTNAME=puppet.stratos.com
MB_HOSTNAME=${MB_IP_ADDR}
MB_PORT=${MB_PORT}
TRUSTSTORE_PASSWORD=wso2carbon

PUPPET_ID=$(sudo docker run -d -e "DOMAIN=${DOMAIN}" -e "MASTERHOSTNAME=${MASTERHOSTNAME}" -e "MB_HOSTNAME=${MB_HOSTNAME}" -e "MB_PORT=${MB_PORT}" -e "TRUSTSTORE_PASSWORD=${TRUSTSTORE_PASSWORD}" -p 8140 apachestratos/puppet)
PUPPET_IP_ADDR=$(sudo docker inspect --format '{{ .NetworkSettings.Gateway }}' $PUPPET_ID)
PUPPET_PORT=$(sudo docker port $PUPPET_ID 8140 | awk -F':' '{ print $2 }')

#
# Start mysql docker container 
# (skip this step if you already have mysql already installed that has a Stratos schema)
# 
# NOTE: This image does NOT persist data - all data is lost when this image stops.
#

USERSTORE_ID=$(sudo docker run -d -p 3306 -e MYSQL_ROOT_PASSWORD=password apachestratos/mysql); sleep 2s;
USERSTORE_IP_ADDR=$(sudo docker inspect --format '{{ .NetworkSettings.Gateway }}' $USERSTORE_ID)
USERSTORE_PORT=$(sudo docker port $USERSTORE_ID 3306 | awk -F':' '{ print $2 }')

#
# Start Stratos
#

# Ensure docker environment variable is clean
unset docker_env

# Database Settings
docker_env+=(-e "USERSTORE_DB_HOSTNAME=${USERSTORE_IP_ADDR}")
docker_env+=(-e "USERSTORE_DB_PORT=${USERSTORE_PORT}")
docker_env+=(-e "USERSTORE_DB_SCHEMA=USERSTORE_DB_SCHEMA")
docker_env+=(-e "USERSTORE_DB_USER=root")
docker_env+=(-e "USERSTORE_DB_PASS=password")

# Puppet Setings
docker_env+=(-e "PUPPET_IP=192.168.56.5")
docker_env+=(-e "PUPPET_HOSTNAME=stratos.com")
docker_env+=(-e "PUPPET_ENVIRONMENT=none")

# MB Settings
docker_env+=(-e "MB_HOSTNAME=${MB_IP_ADDR}")
docker_env+=(-e "MB_PORT=${MB_PORT}")

# IAAS Settings
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

# Stratos Settings [profile=default|cc|as|sm]
docker_env+=(-e "STRATOS_PROFILE=default")

# Start Stratos container as daemon
container_id=$(sudo docker run -d "${docker_env[@]}" -p 9443:9443 apachestratos/stratos)
sudo docker logs -f $container_id

# Start interactively (requires running /usr/local/bin/run manually)
# sudo docker run -i -t "${docker_env[@]}" apachestratos/stratos /bin/bash
