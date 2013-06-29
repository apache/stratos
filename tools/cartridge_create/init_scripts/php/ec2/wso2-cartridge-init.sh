#!/bin/bash

# ----------------------------------------------------------------------------
#  Copyright 2005-20012 WSO2, Inc. http://www.wso2.org
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# ----------------------------------------------------------------------------
export LOG=/var/log/wso2-cartridge.log
instance_path=/opt
PUBLIC_IP=""
KEY=`uuidgen`
CRON_DURATION=1
RETRY_COUNT=30
SLEEP_DURATION=3
ACCESSLOG="/var/log/apache2/access.log"
ERRORLOG="/var/log/apache2/error.log"


if [ ! -d ${instance_path}/payload ]; then

    echo "creating payload dir ... " >> $LOG
    mkdir ${instance_path}/payload
    echo "payload dir created ... " >> $LOG
    wget http://169.254.169.254/latest/user-data -O ${instance_path}/payload/payload.zip    
    echo "payload copied  ... "  >> $LOG
    unzip -d ${instance_path}/payload ${instance_path}/payload/payload.zip
    echo "unzippeddd..." >> $LOG

    for i in `/usr/bin/ruby /opt/get-launch-params.rb`
    do
    echo "exporting to bashrc $i ... " >> $LOG
        echo "export" ${i} >> /home/ubuntu/.bashrc
    done
    source /home/ubuntu/.bashrc

fi


echo ---------------------------- >> $LOG
echo "getting public ip from metadata service" >> $LOG

wget http://169.254.169.254/latest/meta-data/public-ipv4
files="`cat public-ipv4`"
if [[ -z ${files} ]]; then
    echo "getting public ip" >> $LOG
    for i in {1..30}
    do
      rm -f ./public-ipv4
      wget http://169.254.169.254/latest/meta-data/public-ipv4
      files="`cat public-ipv4`"
      if [ -z $files ]; then
          echo "Public ip is not yet assigned. Wait and continue for $i the time ..." >> $LOG
          sleep $SLEEP_DURATION
      else
          echo "Public ip assigned" >> $LOG
          break
      fi
    done

    if [ -z $files ]; then
      echo "Public ip is not yet assigned. So exit" >> $LOG
      exit 0
    fi
    for x in $files
    do
        PUBLIC_IP="$x"
    done


else 
   PUBLIC_IP="$files"
fi


for i in `/usr/bin/ruby /opt/get-launch-params.rb`
do
    export ${i}
done

echo "Logging sys variables .. PUBLIC_IP:${PUBLIC_IP}, HOST_NAME:${HOST_NAME}, KEY:${KEY}, PORTS=${PORTS}, BAM:${BAM_IP}, GITREPO:${GIT_REPO}" >> $LOG

mkdir -p  /etc/agent/conf

echo "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:agen=\"http://service.agent.cartridge.carbon.wso2.org\">
  <soapenv:Header/>
  <soapenv:Body>
     <agen:register>
        <registrant> 
           <alarmingLowerRate>${ALARMING_LOWER_RATE}</alarmingLowerRate>
           <alarmingUpperRate>${ALARMING_UPPER_RATE}</alarmingUpperRate>
           <hostName>${HOST_NAME}</hostName>
           <key>${KEY}</key>
          <maxInstanceCount>${MAX}</maxInstanceCount>
      <maxRequestsPerSecond>${MAX_REQUESTS_PER_SEC}</maxRequestsPerSecond>
          <minInstanceCount>${MIN}</minInstanceCount> " > /etc/agent/conf/request.xml

IFS='|' read -ra PT <<< "${PORTS}"
for i in "${PT[@]}"; do
IFS=':' read -ra PP <<< "$i"
echo "          <portMappings>
                        <primaryPort>${PP[1]}</primaryPort>
                        <proxyPort>${PP[2]}</proxyPort>
                        <type>${PP[0]}</type>
                </portMappings>">> /etc/agent/conf/request.xml
done

echo "          <remoteHost>${PUBLIC_IP}</remoteHost>
           <service>${SERVICE}</service>
       <remoteHost>${PUBLIC_IP}</remoteHost>
           <roundsToAverage>${ROUNDS_TO_AVERAGE}</roundsToAverage>
           <scaleDownFactor>${SCALE_DOWN_FACTOR}</scaleDownFactor>
           <tenantRange>${TENANT_RANGE}</tenantRange>
        </registrant>
     </agen:register>
  </soapenv:Body>
</soapenv:Envelope>
" >> /etc/agent/conf/request.xml

echo "Creating repoinfo request  " >> $LOG
echo "TENANT_ID and SERVICE ${TENANT_ID} and ${SERVICE} " >> $LOG
set -- "${HOST_NAME}" 
IFS="."; declare -a Array=($*)
ALIAS="${Array[0]}"
echo "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://org.apache.axis2/xsd\">
   <soapenv:Header/>
   <soapenv:Body>
      <xsd:getRepositoryCredentials>
         <xsd:tenantId>${TENANT_ID}</xsd:tenantId>
         <xsd:cartridgeType>${SERVICE}</xsd:cartridgeType>
         <xsd:alias>${ALIAS}</xsd:alias>
      </xsd:getRepositoryCredentials>
   </soapenv:Body>
</soapenv:Envelope>" > /opt/repoinforequest.xml

echo "Repoinfo request created " >> $LOG

echo "Private Key....Copying to .ssh  " >> $LOG

cp ${instance_path}/payload/id_rsa /root/.ssh/id_rsa
chmod 0600 /root/.ssh/id_rsa
echo "StrictHostKeyChecking no" >> /root/.ssh/config


cp -f ${instance_path}/payload/ssl-cert-snakeoil.pem /etc/ssl/certs/ssl-cert-snakeoil.pem
cp -f ${instance_path}/payload/ssl-cert-snakeoil.key /etc/ssl/private/ssl-cert-snakeoil.key

/etc/init.d/apache2 restart

echo "Sending register request to Cartridge agent service" >> $LOG

curl -X POST -H "Content-Type: text/xml" -H "SOAPAction: urn:register" -d @/etc/agent/conf/request.xml -k --silent --output /dev/null "${CARTRIDGE_AGENT_EPR}"

#echo "Getting repo username password from repoInfoService" >> $LOG
#curl -X POST -H "Content-Type: text/xml" -d @/etc/agent/conf/request.xml --silent --output /dev/null "${CARTRIDGE_AGENT_EPR}"

echo "Git repo sync" >> $LOG

# If repo is available do a git pull, else clone
echo "#!/bin/bash
if [ -d \"${APP_PATH}/.git\" ]; then
    cd ${APP_PATH}
    
    curl -X POST -H \"Content-Type: text/xml\" -H \"SOAPAction: urn:getRepositoryCredentials\" -d @/opt/repoinforequest.xml --silent  \"${REPO_INFO_EPR}\" --insecure > /tmp/git.xml
   sed '1,5d' /tmp/git.xml > /tmp/git1.xml
   sed '2d' /tmp/git1.xml > /tmp/git.xml
   username=\`xml_grep 'ax211:userName' /tmp/git.xml --text_only\`
   password=\`xml_grep 'ax211:password' /tmp/git.xml --text_only\`
   repo=\`xml_grep 'ax211:url' /tmp/git.xml --text_only\`
   rm /tmp/git1.xml
   rm /tmp/git.xml
   url=\`echo \$repo |sed 's/http.*\/\///g' |sed 's/\:.*//g' |sed 's/\/.*//g'\`
   echo \"machine \${url} login \${username} password \${password}\" > ~/.netrc
   sudo echo \"machine \${url} login \${username} password \${password}\" > /root/.netrc
   chmod 600 ~/.netrc
   sudo chmod 600 /root/.netrc
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
   username=\`xml_grep 'ax211:userName' /tmp/git.xml --text_only\`
   password=\`xml_grep 'ax211:password' /tmp/git.xml --text_only\`
   repo=\`xml_grep 'ax211:url' /tmp/git.xml --text_only\`
   rm /tmp/git1.xml
   rm /tmp/git.xml
   url=\`echo \$repo |sed 's/http.*\/\///g' |sed 's/\:.*//g' |sed 's/\/.*//g'\`
   echo \"machine \${url} login \${username} password \${password}\" > ~/.netrc
   sudo echo \"machine \${url} login \${username} password \${password}\" > /root/.netrc
   chmod 600 ~/.netrc
   sudo chmod 600 /root/.netrc
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
    


fi" > /opt/git.sh
echo "File created.." >> $LOG
chmod 755 /opt/git.sh
echo "running git clone........" >> $LOG
su - ubuntu /opt/git.sh


echo "setting up logging conf" >> $LOG


echo "host:     ${BAM_IP}
thriftPort:     ${BAM_PORT}

#cartridge configs
cartridgeAlias:  ${SERVICE}
tenantName:      ${HOST_NAME}
tenantId::      ${TENANT_ID}
localIP:         ${PUBLIC_IP}" > /opt/cartridge_data_publisher_1.0.2/conf/data_publisher.conf


echo "started loggin ........."
cd /opt/cartridge_data_publisher_1.0.2/dist/Debug/GNU-Linux-x86/
nohup ./cartridge_data_publisher_1.0.2 ${ACCESSLOG} ${ERRORLOG} 2>&1> /dev/null &





# ========================== // End of script ===========================================================
