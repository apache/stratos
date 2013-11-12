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
event_subscriber_path=/opt/apache-stratos-cartridge-agent/event-subscriber # Event subscriber home
health_publisher_path=/opt/apache-stratos-cartridge-agent/health-publisher # Health publisher home

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
echo "Event published" | tee -a $LOG
popd


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
set -e
GIT_SH_LOG=/var/log/apache-stratos/git-sh.log
if [ -d \"${APP_PATH}/.git\" ]; then
   cd ${APP_PATH}
   echo \"Invoking repo info service...\" | tee -a \$GIT_SH_LOG
   username=\$1
   password=\$2
   repo=\$3
   echo \"username=\$username repo=\${repo}\" | tee -a \$GIT_SH_LOG
   echo \"Preparing .netrc...\" | tee -a \$GIT_SH_LOG
   url=\`echo \$repo |sed 's/http.*\/\///g' |sed 's/\:.*//g' |sed 's/\/.*//g'\`
   echo \"machine \${url} login \${username} password \${password}\" > ~/.netrc
   sudo echo \"machine \${url} login \${username} password \${password}\" > /root/.netrc
   chmod 600 ~/.netrc
   sudo chmod 600 /root/.netrc
   echo \"Setting git http.sslVerify false\" | tee -a \$GIT_SH_LOG 
   git config --global --bool --add http.sslVerify false
   echo \"Running git pull...\" | tee -a \$GIT_SH_LOG
   sudo git pull  
   if [ -f ~/.netrc ]; then
      echo \"Removing ~/.netrc...\" | tee -a \$GIT_SH_LOG
      rm ~/.netrc
   fi
   if [ -f /root/.netrc ]; then
      echo \"Removing /root/.netrc...\" | tee -a \$GIT_SH_LOG
      sudo rm /root/.netrc
   fi

   echo \"Changing owner of application path/www to www-data:www-data...\" | tee -a \$GIT_SH_LOG
   sudo chown -R www-data:www-data ${APP_PATH}/www
   if [ -f \"${APP_PATH}/sql/alter.sql\" ] && [ -n \"${MYSQL_HOST}\" ] && [ -n \"${MYSQL_USER}\" ] && [ -n \"${MYSQL_PASSWORD}\" ]; then
 	    mysql -h ${MYSQL_HOST} -u ${MYSQL_USER} -p${MYSQL_PASSWORD} < ${APP_PATH}/sql/alter.sql
   fi
else
   echo \"Removing index.html from application path...\" | tee -a \$GIT_SH_LOG
   sudo rm -f ${APP_PATH}/index.html
   echo \"Invoking repo info service...\" | tee -a \$GIT_SH_LOG
   curl -X POST -H \"Content-Type: text/xml\" -H \"SOAPAction: urn:getRepositoryCredentials\" -d @${instance_path}/repoinforequest.xml --silent  \"${REPO_INFO_EPR}\" --insecure > /tmp/git.xml
   echo \"Processing repo info service response...\" | tee -a \$GIT_SH_LOG
   sed '1,5d' /tmp/git.xml > /tmp/git1.xml
   sed '2d' /tmp/git1.xml > /tmp/git.xml
   username=\`xml_grep 'ax29:userName' /tmp/git.xml --text_only\`
   password=\`xml_grep 'ax29:password' /tmp/git.xml --text_only\`
   repo=\`xml_grep 'ax29:url' /tmp/git.xml --text_only\`
   echo \"username=\$username repo=\${repo}\" | tee -a \$GIT_SH_LOG
   rm /tmp/git1.xml
   rm /tmp/git.xml
   echo \"Preparing .netrc...\" | tee -a \$GIT_SH_LOG
   url=\`echo \$repo |sed 's/http.*\/\///g' |sed 's/\:.*//g' |sed 's/\/.*//g'\`
   echo \"machine \${url} login \${username} password \${password}\" > ~/.netrc
   sudo echo \"machine \${url} login \${username} password \${password}\" > /root/.netrc
   chmod 600 ~/.netrc
   sudo chmod 600 /root/.netrc
   echo \"Setting git http.sslVerify false\" | tee -a \$GIT_SH_LOG
   git config --global --bool --add http.sslVerify false
   echo \"Creating temporary git folder...\" | tee -a \$GIT_SH_LOG
   sudo mkdir ${instance_path}/temp_git
   echo \"Running git clone...\" | tee -a \$GIT_SH_LOG
   git clone \${repo} ${instance_path}/temp_git
   echo \"Moving content to /var/www\" | tee -a \$GIT_SH_LOG
   sudo mv ${instance_path}/temp_git/* /var/www
   sudo mv ${instance_path}/temp_git/.git /var/www
   sudo rm -rf ${instance_path}/temp_git
   if [ -f ~/.netrc ]; then
      echo \"Removing ~/.netrc...\" | tee -a \$GIT_SH_LOG
      rm ~/.netrc
   fi
   if [ -f /root/.netrc ]; then
      echo \"Removing /root/.netrc...\" | tee -a \$GIT_SH_LOG
      sudo rm /root/.netrc
   fi
 
   if [ -n \"${MYSQL_HOST}\" ] && [ -n \"${MYSQL_USER}\" ] && [ -n \"${MYSQL_PASSWORD}\" ]; then
      if [ -f \"${APP_PATH}/sql/init.sql\" ]; then
          mysql -h ${MYSQL_HOST} -u ${MYSQL_USER} -p${MYSQL_PASSWORD} < ${APP_PATH}/sql/init.sql
      fi
      echo \"Generating .htaccess...\" | tee -a \$GIT_SH_LOG
      echo \"SetEnv STRATOS_MYSQL_USER ${MYSQL_USER}
      SetEnv STRATOS_MYSQL_HOST ${MYSQL_HOST}
      SetEnv STRATOS_MYSQL_PASSWORD ${MYSQL_PASSWORD}
      \" > /tmp/.htaccess
      echo \"Copying .htaccess to application path...\" | tee -a \$GIT_SH_LOG
      sudo mv /tmp/.htaccess ${APP_PATH}/
   fi
   echo \"Changing owner of application path/www to www-data:www-data...\" | tee -a \$GIT_SH_LOG 
   sudo chown -R www-data:www-data ${APP_PATH}/www
   
fi
echo \"git.sh done\" | tee -a \$LOG" > ${instance_path}/git.sh
echo "git.sh generated" | tee -a $LOG
chmod 755 ${instance_path}/git.sh


# -----------------------------------------------------
# Git clone
# -----------------------------------------------------
pushd ${instance_path}
echo "Running git.sh..." | tee -a $LOG
sudo sh ${instance_path}/git.sh
echo "Git clone done" | tee -a $LOG


# -----------------------------------------------------
# Publish member-activated-event
# -----------------------------------------------------
while true
do
var=`nc -z localhost 80; echo $?`;
if [ $var -eq 0 ]
   then
       echo "Port 80 is available" | tee -a $LOG
       break
   else
       echo "Port 80 is not available" | tee -a $LOG
   fi
   sleep 1
done

while true
do
var=`nc -z localhost 443; echo $?`;
if [ $var -eq 0 ]
   then
       echo "Port 443 is available" | tee -a $LOG
       break
   else
       echo "Port 443 is not available" | tee -a $LOG
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
echo "Event published" | tee -a $LOG
popd

pushd $health_publisher_path/bin
echo "Executing: health-publisher.sh"
sh health-publisher.sh $MEMBER_ID $CEP_IP $CEP_PORT
echo "Health stat published" | tee -a $LOG
popd
