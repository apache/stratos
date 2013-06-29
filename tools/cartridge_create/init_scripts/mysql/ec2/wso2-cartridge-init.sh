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
SLEEP_DURATION=3

if [ ! -d ${instance_path}/payload ]; then

    echo "creating payload dir ... " >> $LOG
    mkdir ${instance_path}/payload
    echo "payload dir created ... " >> $LOG
    wget http://169.254.169.254/latest/user-data -O ${instance_path}/payload/payload.zip
    echo "payload copied  ... "  >> $LOG
    unzip -d ${instance_path}/payload ${instance_path}/payload/payload.zip
    echo "unzipped..." >> $LOG

    for i in `/usr/bin/ruby /opt/get-launch-params.rb`
    do
    echo "exporting to bashrc $i ... " >> $LOG
        echo "export" ${i} >> /home/ubuntu/.bashrc
    done
    source /home/ubuntu/.bashrc

fi

if [ -f public-ipv4 ]; then
    echo "public-ipv4 already exists. Removing the public-ipv4 file" >> $LOG
    rm public-ipv4
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
          #crontab -r
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
   #crontab -r
fi


for i in `/usr/bin/ruby /opt/get-launch-params.rb`
do
    export ${i}
done

echo "Setting MySQL root password" >> $LOG
if [[ (-n ${MYSQL_PASSWORD} ) ]]; then
	mysqladmin -u root password "${MYSQL_PASSWORD}"
	mysql -uroot -p${MYSQL_PASSWORD} -e "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%'   IDENTIFIED BY '${MYSQL_PASSWORD}' WITH GRANT OPTION;flush privileges;"
	echo "MySQL root password set" >> $LOG
fi

cp -f ${instance_path}/payload/ssl-cert-snakeoil.pem /etc/ssl/certs/ssl-cert-snakeoil.pem
cp -f ${instance_path}/payload/ssl-cert-snakeoil.key /etc/ssl/private/ssl-cert-snakeoil.key
/etc/init.d/apache2 restart

echo "Logging sys variables .. PUBLIC_IP:${PUBLIC_IP}, HOST_NAME:${HOST_NAME}, KEY:${KEY}, PORTS=${PORTS} , BAM:${BAM_IP}, GITREPO:${GIT_REPO}" >> $LOG

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

echo "
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

sleep 3
echo "Sending register request to Cartridge agent service" >> $LOG
curl -X POST -H "Content-Type: text/xml" -H "SOAPAction: urn:register" -d @/etc/agent/conf/request.xml -k --silent --output /dev/null "${CARTRIDGE_AGENT_EPR}"


# ========================== // End of script ===========================================================

