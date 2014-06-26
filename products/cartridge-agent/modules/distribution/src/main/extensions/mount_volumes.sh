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
echo -e "Starting mounting volumes" 2>&1 | tee -a $log

# $1  is passed from Cartridge Agent code.
echo -e "launch param file location $1" | tee -a $log

PERSISTENCE_MAPPING=$1
echo -e "Persistance mappings : $PERSISTENCE_MAPPING" 2>&1 | tee -a $log

mount_volume(){

        device=$1;
        mount_point=$2;
    echo -e "device $device" | tee -a $log
        echo -e "mount point  $mount_point"| tee -a $log


        if [  "$mount_point" = "null" ]
        then
              echo -e "[ERROR] Mount point can not be null" | tee -a $log
          return
        fi

        if [  "$device" = "null" ]
        then
              echo -e "[ERROR] Device can not be null" | tee -a $log
          return
        fi

        device_exist=`sudo fdisk -l $device`;
        if [ "$device_exist" = "" ]
        then
              echo -e "[ERROR] Device $device does not exist in this instance." | tee -a $log
          return
    fi

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
              sudo mkdir -p $mount_point
        fi

        #mounting the device if it is not already mounted
        if [ ! "$device_mounted" = "" ]
        then
              echo -e "[WARNING] Device $device is already mounted." | tee -a $log
        else
              sudo mount $device $mount_point  2>&1 | tee -a $log
        fi

}

IFS='|' read -ra ADDR <<< "${PERSISTENCE_MAPPING}"
echo "${ADDR[@]}" | tee -a $log

echo -e "\n Volumes before mounting...." | tee -a $log
output=`/bin/lsblk`
echo -e "\n$output\n" | tee -a $log
output=`/sbin/blkid`
echo -e "\n$output\n" | tee -a $log

totalcount=0
for i in "${!ADDR[@]}"; do
        # expected PERSISTANCE_MAPPING format is device1|volumeID1|mountPoint1|device2|volumeID2|mountpoint2...
        if (( $i  % 3 == 0 ))
        then
           devicelist[$totalcount]=${ADDR[$i]}
           mountpathlist[$totalcount]=${ADDR[$i + 2]}
           totalcount=$((totalcount+1))
           lastdevice=${ADDR[$i]}
        fi
done


device_exist=`sudo fdisk -l $lastdevice`;
if [ "$device_exist" = "" ]
then
# Last device doesn't exist, which means devices are not attached as per the order given in the payload
# So start mounting from the last available device and come down
    devlist=`sudo lsblk -n | cut -f 1 -d " "`
    devcount=`echo ${devlist} | awk '{print NF}'`
    totalcount=$((totalcount-1))
        counter=0
    for ((i=${devcount}-1; totalcount>=0; i--)); do
        devnum=`expr ${devcount} - ${counter}`
        currdevice=`echo ${devlist} | cut -d " " -f ${devnum}`
        fileout=`sudo file -s /dev/${currdevice}`
        if [[ $fileout == *ROM* ]] || [[ $fileout == *boot* ]] || [[ $fileout == *cloud* ]] || [[ $fileout == *not-regular* ]] || [[ $fileout == *empty* ]] || [[ $fileout == *swap* ]]
        then
            # Ignore special files as checked above...
            counter=`expr ${counter} + 1`
            continue
        fi
        mountpath=${mountpathlist[$totalcount]}
        totalcount=$((totalcount-1))
        echo "device is $currdevice"
        echo "mount path is $mountpath"
        counter=`expr ${counter} + 1`
        mount_volume "/dev/${currdevice}" ${mountpath}
    done
else
# Last device exists, which means the volumes are created or few might have been skipped in the first part since the device is already present
# Mount of rest of the volumes will be fine and contine
    for i in "${!ADDR[@]}"; do
        # expected PERSISTANCE_MAPPING format is device1|volumeID1|mountPoint1|device2|volumeID2|mountpoint2...
        if (( $i  % 3 == 0 ))
        then
           mount_volume ${ADDR[$i]} ${ADDR[$i + 2]}
        fi
    done
fi

echo -e "\n Volumes after mounting...." | tee -a $log
output=`/bin/lsblk`
echo -e "\n$output\n" | tee -a $log
output=`/sbin/blkid`
echo -e "\n$output\n" | tee -a $log
