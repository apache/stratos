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
# EC2 Demo configuration script for Apache Stratos
# ----------------------------------------------------------------------------

# Die on any error:
set -e

source "./conf/setup.conf"
SLEEP=100
export LOG=$log_path/stratos-ec2-user-data.log

# Make sure the user is running as root.
if [ "$UID" -ne "0" ]; then
	echo ; echo "  You must be root to run $0.  (Try running 'sudo bash' first.)" ; echo 
	exit 69
fi

if [[ ! -d $log_path ]]; then
    mkdir -p $log_path
fi

#  Configuring Amazon EC2 user data.
# ----------------------------------------------------------------------------

# Following is helpful if you've mistakenly added data in user-data and want to recover.
read -p "Please confirm whether you want to be prompted, irrespective of the data available in user-data? [y/n] " answer
if [[ $answer = n && `curl -o /dev/null --silent --head --write-out '%{http_code}\n' http://169.254.169.254/latest/user-data` = "200" ]] ; then
    echo "Trying to find values via user-data" >> $LOG
    wget http://169.254.169.254/latest/user-data -O /opt/user-data.txt >> $LOG
    userData=`cat /opt/user-data.txt`
    echo "Extracted user-data: $userData" >> $LOG

    # Assign values obtained through user-data
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

# Get hostname
wget http://169.254.169.254/latest/meta-data/public-hostname -O /opt/public-hostname
stratos_hostname=`cat /opt/public-hostname`

# Prompt for the values that are not retrieved via user-data
if [[ -z $EC2_KEY_PATH ]]; then
    echo -n "Please copy your EC2 public key and enter full path to it (eg: /home/ubuntu/EC2SKEY.pem):"
    read EC2_KEY_PATH
    if [ ! -e "$EC2_KEY_PATH" ]; then
        echo "EC2 key file ($EC2_KEY_PATH) is not found. Installer cannot proceed."
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
    echo -n "Name of the key pair (eg: EC2SKEY) :"
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
    echo "EC2 key file ($EC2_KEY_PATH) is not found. Installer cannot proceed."
    exit 69
fi

echo "Updating conf/setup.conf with user data"
cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export stratos_domain=\"*.*\"@export stratos_domain=\"$DOMAIN\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export keypair_path=\"*.*\"@export keypair_path=\"$EC2_KEY_PATH\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export ec2_keypair_name=\"*.*\"@export ec2_keypair_name=\"$KEY_PAIR_NAME\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export ec2_identity=\"*.*\"@export ec2_identity=\"$ACCESS_KEY\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export ec2_credential=\"*.*\"@export ec2_credential=\"$SECRET_KEY\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export ec2_owner_id=\"*.*\"@export ec2_owner_id=\"$OWNER_ID\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export ec2_availability_zone=\"*.*\"@export ec2_availability_zone=\"$AVAILABILITY_ZONE\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export ec2_security_groups=\"*.*\"@export ec2_security_groups=\"$SECURITY_GROUP\"@g" > conf/setup.conf

# Updating conf/setup.conf with relevent data
# ----------------------------------------------------------------------------

ip=`facter ipaddress`
echo "Setting private ip addresses $ip" >> $LOG

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export hostip=\"*.*\"@export hostip=\"$ip\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export host_user=\"*.*\"@export host_user=\"ubuntu\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export userstore_db_pass=\"*.*\"@export userstore_db_pass=\"root\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export stratos_foundation_db_pass=\"*.*\"@export stratos_foundation_db_pass=\"root\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export elb_ip=\"*.*\"@export elb_ip=\"$ip\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export agent_ip=\"*.*\"@export agent_ip=\"$ip\"@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export ec2_provider_enabled=false@export ec2_provider_enabled=true@g" > conf/setup.conf

cp -f conf/setup.conf conf/setup.conf.orig
cat conf/setup.conf.orig | sed -e "s@export openstack_provider_enabled=true@export openstack_provider_enabled=false@g" > conf/setup.conf


# Mapping domain/host names for each product
# ----------------------------------------------------------------------------

cp -f /etc/hosts hosts.tmp

echo "$ip stratos.apache.org        # stratos domain"	>> hosts.tmp
echo "$ip mb.stratos.apache.org     # message broker hostname"	>> hosts.tmp
echo "$ip cc.stratos.apache.org     # cloud controller hostname"	>> hosts.tmp
echo "$ip sc.stratos.apache.org     # stratos controller hostname"	>> hosts.tmp
echo "$ip elb.stratos.apache.org    # elastic load balancer hostname"	>> hosts.tmp
echo "$ip agent.stratos.apache.org  # agent hostname"	>> hosts.tmp

mv -f ./hosts.tmp /etc/hosts


# Setup and start the MB server
# ----------------------------------------------------------------------------

export LOG=$log_path/stratos-mb-setup.log
export mb_path=$stratos_path/"wso2mb"
export mb_pack=$stratos_pack_path/"wso2mb"

if [[ ! -d $mb_path ]]; then
    unzip $mb_pack -d $stratos_path
fi

pushd $mb_path

# cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
# cat repository/conf/carbon.xml.orig | sed -e "s@AGENT_HOSTNAME:AGENT_PORT@$agent_ip:$agent_https_port@g" > repository/conf/cartridge-config.properties

popd

echo "Starting MB server ..." >> $LOG
nohup ${mb_path}/bin/wso2server.sh -DportOffset=5 &
echo "MB server started" >> $LOG
sleep $SLEEP

echo "WSO2 MB setup has successfully completed"

#  Server configurations
# ----------------------------------------------------------------------------
su -c "source $setup_path/conf/setup.conf;$setup_path/setup.sh -p'all'"

