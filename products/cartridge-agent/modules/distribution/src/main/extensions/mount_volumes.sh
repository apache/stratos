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
# This extension script will be executed to start the servers.
# --------------------------------------------------------------
#

log=/var/log/apache-stratos/cartridge-agent-extensions.log
echo -e "Starting mounting volumes" | tee -a $log

# $1 refers the payload params file which is passed from Cartridge Agent code.
source $1

function mount_volume(){

	device=$1;
	mount_point=$2;

	# check if the volume has a file system
	output=`sudo file -s $device`;
	echo $output | tee -a $log

	# this is the pattern of the output of file -s if the volume does not have a file system
	# refer to http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-using-volumes.html
	pattern="$device: data"

	if [[ $output ==  $pattern ]]
	then
		echo -e "Volume is not formatted. So formating the device $device \n" | tee -a $log
		sudo mkfs -t ext4 $device
	fi



	echo "Mounting  the device $device to the mount point $mount_point \n" | tee -a $log
	if [ ! -d "$DIRECTORY" ]; then
		echo "creating the directory $mount_point since it does not exist." | tee -a $log
		mkdir $mount_point
	fi

	sudo mount $device $mount_point
}

IFS='|' read -ra ADDR <<< "$PERSISTANCE_MAPPING"
for i in "${!ADDR[@]}"; do
	# expected PERSISTANCE_MAPPING format is device1|mountPoint1|device2|mountpoint2...
	# so that even indexes are devices and odd indexes are mount points..

	if (( $i  % 2 == 0 ))
	then
	   mount_volume ${ADDR[$i]} ${ADDR[$i + 1]}
	fi
done

echo $IFS
