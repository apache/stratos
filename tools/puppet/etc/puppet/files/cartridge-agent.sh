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

# This script will be called from /etc/rc.local when the cartridge
# instance is spawned. It will initiate all the tasks that needs to 
# be run to bring the cartridge instance to operational state.

source /etc/environment

set -e # Terminate on any error
export LOG=/var/log/apache-stratos/cartridge-agent.log
instance_path=/opt/apache-stratos-cartridge-agent # Cartridge agent home
event_publisher_path=/opt/apache-stratos-cartridge-agent/event-publisher # Event publisher home
event_subscriber_path=/opt/apache-stratos-cartridge-agent/event-subscriber # Event subscriber home
health_publisher_path=/opt/apache-stratos-cartridge-agent/health-publisher # Health publisher home
temp_payload_path=/tmp/payload/launch-params
puppet_payload_path=/tmp/puppet-payload


#-----
# Unzip packs
#-----
pushd ${instance_path}
unzip apache-stratos-event-publisher-4.0.0-SNAPSHOT-bin.zip
unzip apache-stratos-event-subscriber-4.0.0-SNAPSHOT-bin.zip
unzip apache-stratos-health-publisher-4.0.0-SNAPSHOT-bin.zip
mv apache-stratos-event-publisher-4.0.0-SNAPSHOT event-publisher
mv apache-stratos-event-subscriber-4.0.0-SNAPSHOT event-subscriber
mv apache-stratos-health-publisher-4.0.0-SNAPSHOT health-publisher
popd

# ---------------------------------------------
# Download payload.zip
# ---------------------------------------------
if [ ! -d ${instance_path}/payload ]; then
    echo "creating payload directory... " | tee -a $LOG
    mkdir ${instance_path}/payload
    echo "payload directory created" | tee -a $LOG
    #wget http://169.254.169.254/latest/user-data -O ${instance_path}/payload/launch-params -- payload already downloaded
    #echo "payload copied"  | tee -a $LOG

    # Concat puppet payload and instance payload into ${instance_path}/payload/launch-params
    #Read puppet configs
    puppet_config=`cat /tmp/puppet-payload`
    echo "puppet_config"
    sed "s|$|${puppet_config}|" ${temp_payload_path} > ${instance_path}/payload/launch-params

    for i in `/usr/bin/ruby ${instance_path}/get-launch-params.rb`
    do
        # Add double quotes on both sides of the value
        value=`echo "${i}" | sed -e s@=@=\"@g`
        value=${value}"\""
        if [[ ${value} == PORTS* ]]; then
            # Replace port separator | with ,
            value=`echo ${value} | sed -e s@'|'@,@g`
        fi
        echo "writing to launch.params ${value}" | tee -a $LOG
        echo "export" ${value} >> ${instance_path}/launch.params
    done    
fi

source ${instance_path}/launch.params



#---------------------------
# Starting Topic Subscriber
#---------------------------
# change mb ip port in conf/jndi.properties
pushd $event_subscriber_path/conf
cp -rf jndi.properties jndi.properties.tmp
cat jndi.properties.tmp | sed -e "s@MB-PORT@$MB_PORT@g" > jndi.properties
cp -rf jndi.properties jndi.properties.tmp
cat jndi.properties.tmp | sed -e "s@MB-IP-ADDRESS@$MB_IP@g" > jndi.properties
rm -f jndi.properties.tmp
popd

pushd $event_subscriber_path/bin
echo "Executing: event-subscriber.sh "
sh event-subscriber.sh  &
echo "Event subscribed" | tee -a $LOG
popd


pushd $health_publisher_path/bin
echo "Executing: health-publisher.sh"
sh health-publisher.sh $MEMBER_ID $CEP_IP $CEP_PORT $PORTS $CLUSTER_ID
echo "Health stat published" | tee -a $LOG
popd
