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
# Generate git.sh
# -----------------------------------------------------
echo "Creating repoinfo request  " | tee -a $LOG
echo "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://org.apache.axis2/xsd\">
   <soapenv:Header/>
   <soapenv:Body>
      <xsd:getRepositoryCredentials>
         <xsd:tenantId>${TENANT_ID}</xsd:tenantId>
         <xsd:cartridgeType>${SERVICE_NAME}</xsd:cartridgeType>
         <xsd:alias>${CARTRIDGE_ALIAS}</xsd:alias>
      </xsd:getRepositoryCredentials>
   </soapenv:Body>
</soapenv:Envelope>" > ${instance_path}/repoinforequest.xml

echo "Repoinfo request created " | tee -a $LOG


echo "Generating git.sh..." | tee -a $LOG
# If repo is available do a git pull, else clone
echo "#!/bin/bash
if [ -d \"${APP_PATH}/.git\" ]; then
   cd ${APP_PATH}
    
   curl -X POST -H \"Content-Type: text/xml\" -H \"SOAPAction: urn:getRepositoryCredentials\" -d @${instance_path}/repoinforequest.xml --silent  \"${REPO_INFO_EPR}\" --insecure > /tmp/git.xml
   sed '1,5d' /tmp/git.xml > /tmp/git1.xml
   sed '2d' /tmp/git1.xml > /tmp/git.xml
   username=\`xml_grep 'ax29:userName' /tmp/git.xml --text_only\`
   password=\`xml_grep 'ax29:password' /tmp/git.xml --text_only\`
   repo=\`xml_grep 'ax29:url' /tmp/git.xml --text_only\`
   rm /tmp/git1.xml
   rm /tmp/git.xml
   url=\`echo \$repo |sed 's/http.*\/\///g' |sed 's/\:.*//g' |sed 's/\/.*//g'\`
   echo \"machine \${url} login \${username} password \${password}\" > ~/.netrc
   sudo echo \"machine \${url} login \${username} password \${password}\" > /root/.netrc
   chmod 600 ~/.netrc
   sudo chmod 600 /root/.netrc
   git config --global --bool --add http.sslVerify false
   sudo git pull
   rm ~/.netrc
   sudo rm /root/.netrc

   sudo chown -R www-data:www-data ${APP_PATH}/www
   if [ -f \"${APP_PATH}/sql/alter.sql\" ]; then
 	mysql -h ${MYSQL_HOST} -u ${MYSQL_USER} -p${MYSQL_PASSWORD} < ${APP_PATH}/sql/alter.sql
   fi
else
   sudo rm -f ${APP_PATH}/index.html
   curl -X POST -H \"Content-Type: text/xml\" -H \"SOAPAction: urn:getRepositoryCredentials\" -d @/opt/repoinforequest.xml --silent  \"${REPO_INFO_EPR}\" --insecure > /tmp/git.xml
   sed '1,5d' /tmp/git.xml > /tmp/git1.xml
   sed '2d' /tmp/git1.xml > /tmp/git.xml
   username=\`xml_grep 'ax29:userName' /tmp/git.xml --text_only\`
   password=\`xml_grep 'ax29:password' /tmp/git.xml --text_only\`
   repo=\`xml_grep 'ax29:url' /tmp/git.xml --text_only\`
   rm /tmp/git1.xml
   rm /tmp/git.xml
   url=\`echo \$repo |sed 's/http.*\/\///g' |sed 's/\:.*//g' |sed 's/\/.*//g'\`
   echo \"machine \${url} login \${username} password \${password}\" > ~/.netrc
   sudo echo \"machine \${url} login \${username} password \${password}\" > /root/.netrc
   chmod 600 ~/.netrc
   sudo chmod 600 /root/.netrc
   git config --global --bool --add http.sslVerify false
   sudo git clone \${repo} ${APP_PATH}
   rm ~/.netrc
   sudo rm /root/.netrc
 

   if [ -f \"${APP_PATH}/sql/init.sql\" ]; then
       mysql -h ${MYSQL_HOST} -u ${MYSQL_USER} -p${MYSQL_PASSWORD} < ${APP_PATH}/sql/init.sql
   fi
   echo \"SetEnv STRATOS_MYSQL_USER ${MYSQL_USER}
   SetEnv STRATOS_MYSQL_HOST ${MYSQL_HOST}
   SetEnv STRATOS_MYSQL_PASSWORD ${MYSQL_PASSWORD}
   \" > /tmp/.htaccess
   sudo mv /tmp/.htaccess ${APP_PATH}/
   sudo chown -R www-data:www-data ${APP_PATH}/www
    
fi" > ${instance_path}/git.sh
echo "File created.." | tee -a $LOG
chmod 755 ${instance_path}/git.sh


# -----------------------------------------------------
# Publish member-activated-event
# -----------------------------------------------------
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

while true
do
var=`nc -z localhost 443; echo $?`;
if [ $var -eq 0 ]
   then
       echo "port 443 is available" | tee -a $LOG
       break
   else
       echo "port 443 is not available" | tee -a $LOG
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

# -----------------------------------------------------
# Git clone
# -----------------------------------------------------
echo "running git clone..." | tee -a $LOG
sh ${instance_path}/git.sh
echo "git clone done" | tee -a $LOG

