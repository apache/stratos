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

set -e # Terminate on any error
export LOG=/var/log/apache-stratos/cartridge-agent.log
instance_path=/opt/apache-stratos-cartridge-agent # Cartridge agent home
event_publisher_path=/opt/apache-stratos-cartridge-agent/event-publisher # Event publisher home

# ---------------------------------------------
# Download payload.zip
# ---------------------------------------------
if [ ! -d ${instance_path}/payload ]; then
    echo "creating payload directory... " | tee -a $LOG
    mkdir ${instance_path}/payload
    echo "payload directory created" | tee -a $LOG
    wget http://169.254.169.254/latest/user-data -O ${instance_path}/payload/payload.zip    
    echo "payload copied"  | tee -a $LOG
    unzip -d ${instance_path}/payload ${instance_path}/payload/payload.zip
    echo "unzipped" | tee -a $LOG

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

# ---------------------------------------------
# Publish member-started-event
# ---------------------------------------------
echo "Generating member-started-event.json..." | tee -a $LOG
cp -f $event_publisher_path/templates/member-started-event.json.template member-started-event.json.tmp
cat member-started-event.json.tmp | sed -e "s@SERVICE_NAME@$SERVICE_NAME@g" > member-started-event.json
cp -f member-started-event.json member-started-event.json.tmp
cat member-started-event.json.tmp | sed -e "s@CLUSTER_ID@$CLUSTER_ID@g" > member-started-event.json
cp -f member-started-event.json member-started-event.json.tmp
cat member-started-event.json.tmp | sed -e "s@MEMBER_ID@$MEMBER_ID@g" > member-started-event.json
rm -f member-started-event.json.tmp
echo "member-started-event.json generated" | tee -a $LOG

echo "Publishing member started event..." | tee -a $LOG
event_class_name=org.apache.stratos.messaging.event.instance.status.MemberStartedEvent
event_json_path=`pwd`/member-started-event.json

pushd $event_publisher_path/bin
echo "Executing: event-publisher.sh $MB_IP $MB_PORT $event_class_name $event_json_path"
sh event-publisher.sh $MB_IP $MB_PORT $event_class_name $event_json_path
popd
echo "Event published" | tee -a $LOG


# -----------------------------------------------------
# Set MySQL root password
# -----------------------------------------------------
echo "Setting MySQL root password" >> $LOG
if [[ (-n ${MYSQL_PASSWORD} ) ]]; then
    mysqladmin -uroot -pmysql password "${MYSQL_PASSWORD}"
    mysql -uroot -p${MYSQL_PASSWORD} -e "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '${MYSQL_PASSWORD}' WITH GRANT OPTION; flush privileges;"
    echo "MySQL root password set" >> $LOG
fi


# -----------------------------------------------------
# Publish member-activated-event
# -----------------------------------------------------
while true
do
var=`nc -z localhost 3306; echo $?`;
if [ $var -eq 0 ]
   then
       echo "port 3306 is available" | tee -a $LOG
       break
   else
       echo "port 3306 is not available" | tee -a $LOG
   fi
   sleep 1
done

while true
do
var=`nc -z localhost 80; echo $?`;
if [ $var -eq 0 ]
   then
       echo "port 80 is available" | tee -a $LOG
       break
   else
       echo "port 80 is not available" | tee -a $LOG
   fi
   sleep 1
done

echo "Generating member-activated-event.json..." | tee -a $LOG
cp -f $event_publisher_path/templates/member-activated-event.json.template member-activated-event.json.tmp
cat member-activated-event.json.tmp | sed -e "s@SERVICE_NAME@$SERVICE_NAME@g" > member-activated-event.json
cp -f member-activated-event.json member-activated-event.json.tmp
cat member-activated-event.json.tmp | sed -e "s@CLUSTER_ID@$CLUSTER_ID@g" > member-activated-event.json
cp -f member-activated-event.json member-activated-event.json.tmp
cat member-activated-event.json.tmp | sed -e "s@MEMBER_ID@$MEMBER_ID@g" > member-activated-event.json
rm -f member-activated-event.json.tmp
echo "member-activated-event.json generated" | tee -a $LOG

echo "Publishing member activated event..." | tee -a $LOG
event_class_name=org.apache.stratos.messaging.event.instance.status.MemberActivatedEvent
event_json_path=`pwd`/member-activated-event.json

pushd $event_publisher_path/bin
echo "Executing: event-publisher.sh $MB_IP $MB_PORT $event_class_name $event_json_path"
sh event-publisher.sh $MB_IP $MB_PORT $event_class_name $event_json_path
popd
echo "Event published" | tee -a $LOG
