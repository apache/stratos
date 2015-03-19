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
# This extension script will be executed to mount volumes
# to the instance.
# --------------------------------------------------------------
#


log=/var/log/apache-stratos/cartridge-agent-extensions.log
echo -e "Starting mounting volumes" 2>&1 | tee -a $log

# $1  is passed from Cartridge Agent code.
echo -e "launh param file location $1" | tee -a $log
#source /opt/apache-stratos-cartridge-agent/launch.params
PERSISTENCE_MAPPING=$1
echo -e "Persistence mappings : $PERSISTENCE_MAPPING" 2>&1 | tee -a $log

mount_volume(){

        device=$1;
        mount_point=$2;
        echo "device $device"
        echo "point  $mount_point"
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
        device_mounted=$(mount | grep "$device")

        if [ ! -d "$mount_point" ]
        then
              echo "creating the  mount point directory $mount_point since it does not exist." | tee -a $log
              sudo mkdir $mount_point
        fi

        #mounting the device if it is not already mounted
        if [ ! "$device_mounted" = "" ]
        then
              echo -e "Device $device is already mounted." | tee -a $log
        else
              sudo mount $device $mount_point
        fi

}

IFS='|' read -ra ADDR <<< "${PERSISTENCE_MAPPING}"
echo "${ADDR[@]}" | tee -a $log

for i in "${!ADDR[@]}"; do
        # expected PERSISTANCE_MAPPING format is device1|mountPoint1|device2|mountpoint2...
        # so that even indexes are devices and odd indexes are mount points..
        if (( $i  % 2 == 0 ))
        then
           mount_volume ${ADDR[$i]} ${ADDR[$i + 1]}
        fi
done

