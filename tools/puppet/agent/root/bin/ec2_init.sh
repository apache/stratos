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
# This script will read SERVICE from the payload
# and do the following.
# - Set the hostname of the instance.
# - Configure the Puppet agent and restart the service.
# - Fetch deployment instructions and configs for this host
#   from the Puppet master.
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
PUPPETD=`which puppetd`

COMMAND="${PUPPETD} -vt"
IP=`${IFCONFIG} eth0 | ${GREP} -e "inet addr" | ${AWK} '{print $2}' | ${CUT} -d ':' -f 2`
LOG=/tmp/puppet-init.log

HOSTSFILE=/etc/hosts
HOSTNAMEFILE=/etc/hostname

read_master() {
#	local COUNT=0
#	[ ${COUNT} == 10 ] && exit 1 || true
	${COMMAND}
#	[ $? == 0 ] && true || (read_master ; COUNT=`expr ${COUNT} + 1`)
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
                "public-ipv4 file is available. Read value" >> $LOG

                # Here means file is available. Read the file
                read -r ip<public-ipv4;
                echo "Empty value is **$ip** " >> $LOG

                if [ -z "$ip" ]
                   then
                         echo "File is empty. Retry...." >> $LOG
                         sleep 2
                         rm public-ipv4
                         continue
                   else
                         echo "public ip is assigned.. value is [$ip]. Remove file" >> $LOG
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

	"Public ip have assigned. Continue.." >> $LOG

	## Clean old poop
	${ECHO} "Removing all existing certificates .."
	#${FIND} /var/lib/puppet -type f -print0 | ${XARGS} -0r ${RM}

	${MKDIR} -p /tmp/payload
	${WGET} http://169.254.169.254/latest/user-data -O /tmp/payload/launch-params

	cd /tmp/payload
	SERVICE_NAME=`sed 's/,/\n/g' launch-params | grep SERVICE_NAME | cut -d "=" -f 2`
	DEPLOYMENT=`sed 's/,/\n/g' launch-params | grep DEPLOYMENT | cut -d "=" -f 2`
	INSTANCE_HOSTNAME=`sed 's/,/\n/g' launch-params | grep HOSTNAME | cut -d "=" -f 2`
	#CLUSTER=`sed 's/,/\n/g' launch-params | grep CLUSTER | cut -d "=" -f 2`
	PUPPET_IP=`sed 's/,/\n/g' launch-params | grep PUPPET_IP | cut -d "=" -f 2`
	HOST="${RANDOMNUMBER}.${DEPLOYMENT}.${SERVICE_NAME}"
	${ECHO} -e "\nNew hostname ${HOST}\n"

	## Set the hostname
	${HOSTNAME} ${HOST}
	${ECHO} "${HOST}" > ${HOSTNAMEFILE}
#	${ECHO} "${IP}  ${HOST}" >> ${HOSTSFILE}
	#${ECHO} "${IP} local.${INSTANCE_HOSTNAME}" >> ${HOSTSFILE} 
	#${ECHO} "${IP} bind.${INSTANCE_HOSTNAME}" >> ${HOSTSFILE}
	#${ECHO} "${PUPPET_IP} ${INSTANCE_HOSTNAME}" >> ${HOSTSFILE}
	${ECHO} "${PUPPET_IP}  s2demo.s2.wso2.com" >> ${HOSTSFILE} 
	/etc/init.d/hostname start

#	## Enable and restart Puppet
#	${SED} -i /etc/default/puppet -e 's/START=no/START=yes/'
	/etc/init.d/puppet stop

	${SLEEP} 5
	
	${PUPPETD} --enable
    
	read_master

#	${SLEEP} 15m

	${PUPPETD} --disable

    #${ECHO} "* * * * * root /root/bin/cron-hosts.sh" >> /etc/crontab
fi

# END
