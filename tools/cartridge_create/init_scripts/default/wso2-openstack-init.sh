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
export LOG=/var/log/wso2-openstack.log
instance_path=/var/lib/cloud/instance

if [ ! -d ${instance_path}/payload ]; then

    echo "creating payload dir ... " >> $LOG
    mkdir ${instance_path}/payload
    echo "payload dir created ... " >> $LOG
    cp ${instance_path}/user-data.txt ${instance_path}/payload/user-data.zip
    echo "payload copied  ... "  >> $LOG
    unzip -d ${instance_path}/payload ${instance_path}/payload/user-data.zip
    echo "unzippeddd..." >> $LOG

    for i in `/usr/bin/ruby /opt/get-launch-params.rb`
    do
    echo "exporting to bashrc $i ... " >> $LOG
        echo "export" ${i} >> /home/ubuntu/.bashrc
    done
    source /home/ubuntu/.bashrc
    # Write a cronjob to execute wso2-openstack-init.sh periodically until public ip is assigned
    #crontab -l > ./mycron
    #echo "*/${CRON_DURATION} * * * * /opt/wso2-openstack-init.sh > /var/log/wso2-openstack-init.log" >> ./mycron
    #crontab ./mycron
    #rm ./mycron

fi



for i in `/usr/bin/ruby /opt/get-launch-params.rb`
do
    export ${i}
done

echo "Done" >> $LOG
# ========================== // End of script ===========================================================
