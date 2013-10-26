#!/bin/bash
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

set -e
log_path=/var/log/apache-stratos
export LOG=$log_path/cartridge-agent-sh.log
instance_path=/opt

if [ ! -d $log_path ]; then
    mkdir -p $log_path
fi

echo "Starting cartridge-agent.sh..." | tee -a $LOG

if [ ! -d ${instance_path}/payload ]; then
    echo "creating payload dir... " | tee -a $LOG
    mkdir ${instance_path}/payload
    echo "payload directory created " | tee -a $LOG
fi

# Fetch user data from payload
wget http://169.254.169.254/latest/user-data -O ${instance_path}/payload/user-data.txt
echo "user-data.txt received"  | tee -a $LOG

for i in `/usr/bin/ruby get-launch-params.rb`
do
    echo "writing user-data parameter $i to user-data.params" | tee -a $LOG
    value=`echo "${i}" | sed -e s@=@=\"@g`
    value=$value"\""
    echo "export" $value >> user-data.params
done
source user-data.params
echo "user-data.params exported" | tee -a $LOG

echo "Generating user-data.json..." | tee -a $LOG
# Replace port separator '|' with ','
PORTS=`echo $PORTS | sed -e s@'|'@,@g`

cp -f user-data.json.template user-data.json.tmp
cat user-data.json.tmp | sed -e "s@SERVICE_NAME@$SERVICE_NAME@g" > user-data.json
cp -f user-data.json user-data.json.tmp
cat user-data.json.tmp | sed -e "s@CLUSTER_ID@$CLUSTER_ID@g" > user-data.json
cp -f user-data.json user-data.json.tmp
cat user-data.json.tmp | sed -e "s@MEMBER_ID@$MEMBER_ID@g" > user-data.json
cp -f user-data.json user-data.json.tmp
cat user-data.json.tmp | sed -e "s@HOST_IP_ADDRESS@$IP_ADDRESS@g" > user-data.json
cp -f user-data.json user-data.json.tmp
cat user-data.json.tmp | sed -e "s@PORT_LIST@$PORTS@g" > user-data.json
cp -f user-data.json user-data.json.tmp
cat user-data.json.tmp | sed -e "s@MB_IP_ADDRESS@$MB_IP@g" > user-data.json
cp -f user-data.json user-data.json.tmp
cat user-data.json.tmp | sed -e "s@MB_PORT@$MB_PORT@g" > user-data.json

echo "user-data.json generated" | tee -a $LOG

echo "Starting Cartridge Agent..." | tee -a $LOG
lib_path=./../lib/
class_path=${lib_path}commons-io-2.0.jar:${lib_path}andes-client-0.13.wso2v8.jar:${lib_path}apache-stratos-cartridge-agent-3.0.0-SNAPSHOT.jar:${lib_path}commons-codec-1.8.jar:${lib_path}commons-logging-1.1.1.jar:${lib_path}geronimo-jms_1.1_spec-1.1.0.wso2v1.jar:${lib_path}geronimo-jms_1.1_spec-1.1.jar:${lib_path}gson-2.2.4.jar:${lib_path}log4j-1.2.13.jar:${lib_path}org.apache.log4j-1.2.13.v200706111418.jar:${lib_path}org.apache.stratos.messaging-3.0.0-SNAPSHOT.jar:${lib_path}org.wso2.carbon.logging-4.1.0.jar:${lib_path}slf4j-api-1.7.5.jar:${lib_path}slf4j-log4j12-1.7.5.jar

json_path=`pwd`/user-data.json
java -cp $class_path org.apache.stratos.cartridge.agent.Main $json_path
echo "Cartridge Agent started" | tee -a $LOG

#TODO 1. Configure required server components
#TODO 2. Download application artifacts
#TODO 3. Deploy application artifacts