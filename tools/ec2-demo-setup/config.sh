#!/bin/bash
# ----------------------------------------------------------------------------
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
# ----------------------------------------------------------------------------
#  Server configuration script for Apache Stratos
# ----------------------------------------------------------------------------

export LOG=/var/log/stratos.log
export TEMP_CONFIG_DIR=/opt/tempconfigs
CARTRIDGE_DEFINITIONS=/opt/apache-stratos-cc-3.0.0-incubating/repository/deployment/server/cartridges
SERVICE_DEFINITIONS=/opt/apache-stratos-cc-3.0.0-incubating/repository/deployment/server/services
PAYLOADS=/opt/apache-stratos-cc-3.0.0-incubating/repository/resources/payload
SC_CONF_MT=/opt/apache-stratos-sc-3.0.0-incubating/repository/conf/multitenancy
MYSQL_JAR=/opt/mysql-connector-java-5.1.25.jar
MYSQL_COPY_PATH=/opt/apache-stratos-sc-3.0.0-incubating/repository/components/lib

# Make sure the user is running as root.
if [ "$UID" -ne "0" ]; then
echo ; echo "  You must be root to run $0.  (Try running 'sudo bash' first.)" ; echo 
exit 69
fi

if [ -e "$LOG" ]; then
rm $LOG
fi

# following is helpful if you've mistakenly added data in user-data and want to recover.
read -p "Please confirm that you want to be prompted, irrespective of whether the data is available in the user-data? [y/n]" answer
if [[ $answer = n ]] ; then

echo "Trying to find values via user-data.." >> $LOG
wget http://169.254.169.254/latest/user-data -O /opt/user-data.txt >> $LOG
userData=`cat /opt/user-data.txt`
echo "Extracted user-data: $userData" >> $LOG

#assign values obtained through user-data
for i in {1..8}
do
entry=`echo $userData  | cut -d',' -f$i | sed 's/,//g'`
key=`echo $entry  | cut -d'=' -f1 | sed 's/=//g'`
value=`echo $entry  | cut -d'=' -f2 | sed 's/=//g'`
if [[ "$key" == *EC2_KEY_PATH* ]] ; then EC2_KEY_PATH=$value; 
elif [[ "$key" == *ACCESS_KEY* ]] ; then ACCESS_KEY=$value; 
elif [[ "$key" == *SECRET_KEY* ]] ; then SECRET_KEY=$value; 
elif [[ "$key" == *OWNER_ID* ]] ; then OWNER_ID=$value; 
elif [[ "$key" == *AVAILABILITY_ZONE* ]] ; then AVAILABILITY_ZONE=$value; 
elif [[ "$key" == *SECURITY_GROUP* ]] ; then SECURITY_GROUP=$value; 
elif [[ "$key" == *KEY_PAIR_NAME* ]] ; then KEY_PAIR_NAME=$value; 
elif [[ "$key" == *DOMAIN* ]] ; then DOMAIN=$value; 
fi
done
fi

# Getting EC2 hostname
wget http://169.254.169.254/latest/meta-data/public-hostname -O /opt/public-hostname
stratos_hostname=`cat /opt/public-hostname`

#prompt for the values that are not retrieved via user-data
if [[ -z $EC2_KEY_PATH ]]; then
echo -n "Please copy your EC2 public key and enter full path to it (eg: /home/ubuntu/EC2S2KEY.pem):"
read EC2_KEY_PATH
if [ ! -e "$EC2_KEY_PATH" ]; then
echo "EC2 key file ($EC2_KEY_PATH) is not found. We cannot proceed."
exit 69
fi
fi
if [[ -z $ACCESS_KEY ]]; then
echo -n "Access Key of EC2 account (eg: Q0IAJDWGFM842UHQP27L) :"
read ACCESS_KEY
fi
if [[ -z $SECRET_KEY ]]; then
echo -n "Secret key of EC2 account (eg: DSKidmKS620mMWMBK5DED983HJSELA) :"
read SECRET_KEY
fi
if [[ -z $OWNER_ID ]]; then
echo -n "Owner id of EC2 account (eg: 927235126122165) :"
read OWNER_ID
fi
if [[ -z $AVAILABILITY_ZONE ]]; then
echo -n "Availability zone (default value: us-east-1c) :"
read AVAILABILITY_ZONE
fi
if [[ -z $SECURITY_GROUP ]]; then
echo -n "Name of the EC2 security group (eg: stratosdemo) :"
read SECURITY_GROUP
fi
if [[ -z $KEY_PAIR_NAME ]]; then
echo -n "Name of the key pair (eg: EC2S2KEY) :"
read KEY_PAIR_NAME
fi
if [[ -z $DOMAIN ]]; then
echo -n "Domain name for Stratos (default value: stratos.apache.org) :"
read DOMAIN
fi

if  [ -z "$DOMAIN" ]; then
DOMAIN="stratos.apache.org"
fi

if  [ -z "$AVAILABILITY_ZONE" ]; then
AVAILABILITY_ZONE="us-east-1c"
echo "Default Availability Zone $AVAILABILITY_ZONE" >> $LOG
fi

if [ ! -e "$EC2_KEY_PATH" ]; then
echo "EC2 key file ($EC2_KEY_PATH) is not found. We cannot proceed."
exit 69
fi

ip=`facter ipaddress`
echo "Setting private ip addresses $ip" >> $LOG

echo "Setting up cloud controller values" >> $LOG

cp $TEMP_CONFIG_DIR/cloud-controller.xml $TEMP_CONFIG_DIR/cloud-controller.xml.tmp
sed -i "s/ACCESSKEY/$ACCESS_KEY/g" $TEMP_CONFIG_DIR/cloud-controller.xml.tmp
sed -i "s#SECRETKEY#$SECRET_KEY#g" $TEMP_CONFIG_DIR/cloud-controller.xml.tmp
sed -i "s/OWNERID/$OWNER_ID/g" $TEMP_CONFIG_DIR/cloud-controller.xml.tmp
sed -i "s/AVAILABILITYZONE/$AVAILABILITY_ZONE/g" $TEMP_CONFIG_DIR/cloud-controller.xml.tmp
sed -i "s/SECURITYGROUP/$SECURITY_GROUP/g" $TEMP_CONFIG_DIR/cloud-controller.xml.tmp
sed -i "s/KEYPAIR/$KEY_PAIR_NAME/g" $TEMP_CONFIG_DIR/cloud-controller.xml.tmp
mv $TEMP_CONFIG_DIR/cloud-controller.xml.tmp /opt/apache-stratos-cc-3.0.0-incubating/repository/conf/cloud-controller.xml
chown ubuntu:ubuntu /opt/apache-stratos-cc-3.0.0-incubating/repository/conf/cloud-controller.xml

rm -rf $CARTRIDGE_DEFINITIONS/*

echo "Setting up domain in cartridge xml" >> $LOG
cp $TEMP_CONFIG_DIR/mysql.xml $TEMP_CONFIG_DIR/mysql.xml.tmp
sed -i "s/S2DOMAIN/$DOMAIN/g" $TEMP_CONFIG_DIR/mysql.xml.tmp
mv $TEMP_CONFIG_DIR/mysql.xml.tmp $CARTRIDGE_DEFINITIONS/mysql.xml
chown ubuntu:ubuntu $CARTRIDGE_DEFINITIONS/mysql.xml

cp $TEMP_CONFIG_DIR/php.xml $TEMP_CONFIG_DIR/php.xml.tmp
sed -i "s/S2DOMAIN/$DOMAIN/g" $TEMP_CONFIG_DIR/php.xml.tmp
mv $TEMP_CONFIG_DIR/php.xml.tmp $CARTRIDGE_DEFINITIONS/php.xml
chown ubuntu:ubuntu $CARTRIDGE_DEFINITIONS/php.xml

cp $TEMP_CONFIG_DIR/tomcat.xml $TEMP_CONFIG_DIR/tomcat.xml.tmp
sed -i "s/S2DOMAIN/$DOMAIN/g" $TEMP_CONFIG_DIR/tomcat.xml.tmp
mv $TEMP_CONFIG_DIR/tomcat.xml.tmp $CARTRIDGE_DEFINITIONS/tomcat.xml
chown ubuntu:ubuntu $CARTRIDGE_DEFINITIONS/tomcat.xml

cp $TEMP_CONFIG_DIR/hosts $TEMP_CONFIG_DIR/hosts.tmp
echo "$ip   stratos.apache.com" >> $TEMP_CONFIG_DIR/hosts.tmp
echo "$ip   cc.apache.org" >> $TEMP_CONFIG_DIR/hosts.tmp
echo "$ip   elb.apache.org" >> $TEMP_CONFIG_DIR/hosts.tmp
echo "$ip   sc.apache.org" >> $TEMP_CONFIG_DIR/hosts.tmp

mv $TEMP_CONFIG_DIR/hosts.tmp /etc/hosts

cp $TEMP_CONFIG_DIR/cartridge-config.properties $TEMP_CONFIG_DIR/cartridge-config.properties.tmp
cp $TEMP_CONFIG_DIR/agent.properties $TEMP_CONFIG_DIR/agent.properties.tmp


sed -i "s/stratos_ip/$ip/g" $TEMP_CONFIG_DIR/cartridge-config.properties.tmp
sed -i "s@EC2KEYPATH@$EC2_KEY_PATH@g" $TEMP_CONFIG_DIR/cartridge-config.properties.tmp
sed -i "s/stratos_hostname/$stratos_hostname/g" $TEMP_CONFIG_DIR/cartridge-config.properties.tmp
sed -i "s/stratos_ip/$ip/g" $TEMP_CONFIG_DIR/agent.properties.tmp

rm -rf $SERVICE_DEFINITIONS/*

cp $TEMP_CONFIG_DIR/stratosreponotifier.groovy $TEMP_CONFIG_DIR/stratosreponotifier.groovy.tmp
sed -i "s/stratos_hostname/$stratos_hostname/g" $TEMP_CONFIG_DIR/stratosreponotifier.groovy.tmp
mv $TEMP_CONFIG_DIR/stratosreponotifier.groovy.tmp /opt/GitBlit/data/groovy/stratosreponotifier.groovy

mv $TEMP_CONFIG_DIR/cartridge-config.properties.tmp /opt/apache-stratos-sc-3.0.0-incubating/repository/conf/cartridge-config.properties
chown ubuntu:ubuntu /opt/apache-stratos-sc-3.0.0-incubating/repository/conf/cartridge-config.properties
mv $TEMP_CONFIG_DIR/agent.properties.tmp /opt/apache-stratos-agent-3.0.0-incubating/repository/conf/agent.properties
chown ubuntu:ubuntu /opt/apache-stratos-agent-3.0.0-incubating/repository/conf/agent.properties

echo "Setting up domain in features dashboard" >> $LOG
cp $TEMP_CONFIG_DIR/features-dashboard.xml $TEMP_CONFIG_DIR/features-dashboard.xml.tmp
sed -i "s/S2DOMAIN/$DOMAIN/g" $TEMP_CONFIG_DIR/features-dashboard.xml.tmp
mv $TEMP_CONFIG_DIR/features-dashboard.xml.tmp $SC_CONF_MT/features-dashboard.xml
chown ubuntu:ubuntu $SC_CONF_MT/features-dashboard.xml

echo "Copying mysql connector jar" >> $LOG
cp -f $MYSQL_JAR $MYSQL_COPY_PATH

echo "You have successfully configured Apache Stratos!!"

su - ubuntu -c /opt/start_servers.sh

