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
	echo -e "Formating the device $1 \n"
	sudo mkfs -t ext4 $1

	echo "Mounting  the device $1 to the mount point $2 \n"
	if [ -d "$DIRECTORY" ]; then
		echo "creating the directory $2 since it does not exist."
		mkdir $2	
	fi

	sudo mount $1 $2
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
