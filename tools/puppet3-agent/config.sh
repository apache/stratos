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
# 	http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# --------------------------------------------------------------
shopt -s nocasematch
ECHO=`which echo`
RM=`which rm`
READ=`which read`
TR=`which tr`
HEAD=`which head`
WGET=`which wget`
MKDIR=`which mkdir`
GREP=`which grep`
SED=`which sed`
CP=`which cp`
MV=`which mv`

HOSTSFILE=/etc/hosts
LOCKFILE=/mnt/apache-stratos-cartridge-agent-4.0.0/wso2carbon.lck
DATE=`date +%d%m%y%S`
RANDOMNUMBER="`${TR} -c -d 0-9 < /dev/urandom | ${HEAD} -c 4`${DATE}"

function valid_ip()
{
    local  ip=$1
    local  stat=1

    if [[ $ip =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
        OIFS=$IFS
        IFS='.'
        ip=($ip)
        IFS=$OIFS
        [[ ${ip[0]} -le 255 && ${ip[1]} -le 255 \
            && ${ip[2]} -le 255 && ${ip[3]} -le 255 ]]
        stat=$?
    fi
    return $stat
}

read -p "This script will install and configure puppet agent, do you want to continue [y/n]" answer
if [[ $answer = y ]] ; then

	${CP} -f ${HOSTSFILE} /etc/hosts.tmp
	${MKDIR} -p /tmp/payload
	${WGET} http://169.254.169.254/latest/user-data -O /tmp/payload/launch-params

	read -p "Please provide stratos service-name:" SERVICE_NAME
	if [[ -z $SERVICE_NAME ]]; then
	echo "service is empty!. Base image will be created."
        SERVICE_NAME=default
	fi

	read -p "Please provide puppet master IP:" PUPPET_IP
	if ! valid_ip $PUPPET_IP ; then
	echo "invalid IP address format!"
	exit -1
	fi

	read -p "Please provide puppet master hostname [puppet.stratos.org]:" DOMAIN
	DOMAIN=${DOMAIN:-puppet.stratos.org}
	#essential to have PUPPET_HOSTNAME at the end in order to auto-sign the certs

	#read -p "Please provide stratos deployment:" DEPLOYMENT
	#DEPLOYMENT=${DEPLOYMENT:-default}
	DEPLOYMENT="default"

	NODEID="${RANDOMNUMBER}.${DEPLOYMENT}.${SERVICE_NAME}"
	
	${ECHO} -e "\nNode Id ${NODEID}\n"
	${ECHO} -e "\nDomain ${DOMAIN}\n"
    
	ARGS=("-n${NODEID}" "-d${DOMAIN}" "-s${PUPPET_IP}")
	${ECHO} "\nRunning puppet installation with arguments: ${ARGS[@]}"
	/root/bin/puppetinstall/puppetinstall "${ARGS[@]}"
	#check for java agent lock file
	if [ -e ${LOCKFILE} ]; then
    ${RM} ${LOCKFILE}
	fi

	${GREP} -q '/root/bin/init.sh > /tmp/puppet_log' /etc/rc.local || ${SED} -i 's/exit 0$/\/root\/bin\/init.sh \> \/tmp\/puppet_log\nexit 0/' /etc/rc.local
	${RM} -rf /tmp/*
	${RM} -rf /var/lib/puppet/ssl/*
	${MV} -f /etc/hosts.tmp ${HOSTSFILE}

fi

# END
