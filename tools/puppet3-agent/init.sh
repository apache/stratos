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

MKDIR=`which mkdir`
UNZIP=`which unzip`
ECHO=`which echo`
FIND=`which find`
GREP=`which grep`
RM=`which rm`
XARGS=`which xargs`
SED=`which sed`
CUT=`which cut`
AWK=`which awk`
IFCONFIG=`which ifconfig`
HOSTNAME=`which hostname`
SLEEP=`which sleep`
TR=`which tr`
HEAD=`which head`
WGET=`which wget`
PUPPETD=`which puppet`
AGENT="agent"
PUPPETAGENT="${PUPPETD} ${AGENT}"
OS=$(lsb_release -si)
PUPPET_DNS_AVAILABLE=false

COMMAND="${PUPPETAGENT} -vt"
IP=`${IFCONFIG} eth0 | ${GREP} -e "inet addr" | ${AWK} '{print $2}' | ${CUT} -d ':' -f 2`
LOG=/tmp/puppet-init.log

HOSTSFILE=/etc/hosts
HOSTNAMEFILE=/etc/hostname
PUPPETCONF=/etc/puppet/puppet.conf

read_master() {
	${COMMAND}
}


is_public_ip_assigned() {

while true
do
   wget http://169.254.169.254/latest/meta-data/public-ipv4
   if [ ! -f public-ipv4 ]
    	then
      	echo "Public ipv4 file not found. Sleep and retry" >> $LOG
      	sleep 2;
      	continue;
    	else
      	echo "public-ipv4 file is available. Read value" >> $LOG
      	# Here means file is available. Read the file
      	read -r ip<public-ipv4;
      	echo "value is **[$ip]** " >> $LOG

      	if [ -z "$ip" ]
        	then
          	echo "File is empty. Retry...." >> $LOG
          	sleep 2
          	rm public-ipv4
          	continue
         	else
           	echo "public ip is assigned. value is [$ip]. Remove file" >> $LOG
           	rm public-ipv4
           	break
         	fi
    	fi
done
}


DATE=`date +%d%m%y%S`
RANDOMNUMBER="`${TR} -c -d 0-9 < /dev/urandom | ${HEAD} -c 4`${DATE}"

if [ ! -d /tmp/payload ]; then

	## Check whether the public ip is assigned
	is_public_ip_assigned

	echo "Public ip have assigned. Continue.." >> $LOG

	## Clean old poop
	${ECHO} "Removing all existing certificates .."
	#${FIND} /var/lib/puppet -type f -print0 | ${XARGS} -0r ${RM}

	${MKDIR} -p /tmp/payload
	${WGET} http://169.254.169.254/latest/user-data -O /tmp/payload/launch-params

	cd /tmp/payload
	SERVICE_NAME=`sed 's/,/\n/g' launch-params | grep SERVICE_NAME | cut -d "=" -f 2`
	DEPLOYMENT=`sed 's/,/\n/g' launch-params | grep DEPLOYMENT | cut -d "=" -f 2`
	INSTANCE_HOSTNAME=`sed 's/,/\n/g' launch-params | grep HOSTNAME | cut -d "=" -f 2`
	PUPPET_IP=`sed 's/,/\n/g' launch-params | grep PUPPET_IP | cut -d "=" -f 2`
	PUPPET_HOSTNAME=`sed 's/,/\n/g' launch-params | grep PUPPET_HOSTNAME | cut -d "=" -f 2`
	PUPPET_ENV=`sed 's/,/\n/g' launch-params | grep PUPPET_ENV | cut -d "=" -f 2`
	PUPPET_DNS_AVAILABLE=`sed 's/,/\n/g' launch-params | grep PUPPET_DNS_AVAILABLE | cut -d "=" -f 2`
	
	#If this property is not set, then set it as false
	if [  -z $PUPPET_DNS_AVAILABLE ];then
		PUPPET_DNS_AVAILABLE=false
	fi
	NODEID="${RANDOMNUMBER}.${DEPLOYMENT}.${SERVICE_NAME}"
	#essential to have PUPPET_HOSTNAME at the end in order to auto-sign the certs
	DOMAIN="${PUPPET_HOSTNAME}"
	${ECHO} -e "\nNode Id ${NODEID}\n"
	${ECHO} -e "\nDomain ${DOMAIN}\n"
	sed -i "s/server=.*/server=${PUPPET_HOSTNAME}/g"  ${PUPPETCONF}
	/etc/init.d/puppet restart    
	ARGS=("-n${NODEID}" "-d${DOMAIN}" "-s${PUPPET_IP}")
	HOST="${NODEID}.${DOMAIN}"
	${HOSTNAME} ${HOST}
	${ECHO} "${HOST}" > ${HOSTNAMEFILE}
	if [ true != $PUPPET_DNS_AVAILABLE ] ; then
		${ECHO} "${PUPPET_IP}  ${PUPPET_HOSTNAME}" >> ${HOSTSFILE} 
	fi
	${ECHO} "127.0.0.1 ${HOST}" >> ${HOSTSFILE}

	if [ "$OS" = "CentOS" ]; then
		#CentOS hostname change
		${ECHO} "CentOS : Changing host name in /etc/sysconfig/network"
		CENTOSHOSTNAME="/etc/sysconfig/network"
		${ECHO} "NETWORKING=yes" > ${CENTOSHOSTNAME}
		${ECHO} "HOSTNAME=${HOST}" >> ${CENTOSHOSTNAME}
		${ECHO} "Network restarting..."
		/etc/init.d/network restart
		${SLEEP} 4	
	else
		#Ubuntu hostname change
		/etc/init.d/hostname start
	fi

	PUPPET=`which puppet`
        PUPPETAGENT="${PUPPET} agent"
        RUNPUPPET="${PUPPETAGENT} -vt"

        ${SLEEP} 5

        ${PUPPETAGENT} --enable

        ${RUNPUPPET}

        ${PUPPETAGENT} --disable
    	${ECHO} -e "Initialization completed successfully."

fi

# END
