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
#
#  This script is invoked by setup.sh for configuring OpenStack IaaS information.
# ----------------------------------------------------------------------------

# Die on any error:
set -e

SLEEP=60
export LOG=$log_path/stratos-vcloud.log

source "./conf/setup.conf"

if [[ ! -d $log_path ]]; then
    mkdir -p $log_path
fi

pushd $cc_path

echo "Set vCloud provider specific info in repository/conf/cloud-controller.xml" >> $LOG

cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
cat repository/conf/cloud-controller.xml.orig | sed -e "s@VCLOUD_PROVIDER_START@@g" > repository/conf/cloud-controller.xml

cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
cat repository/conf/cloud-controller.xml.orig | sed -e "s/VCLOUD_IDENTITY/$vcloud_identity/g" > repository/conf/cloud-controller.xml

cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
cat repository/conf/cloud-controller.xml.orig | sed -e "s/VCLOUD_CREDENTIAL/$vcloud_credential/g" > repository/conf/cloud-controller.xml

cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
cat repository/conf/cloud-controller.xml.orig | sed -e "s@VCLOUD_ENDPOINT@$vcloud_jclouds_endpoint@g" > repository/conf/cloud-controller.xml

cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
cat repository/conf/cloud-controller.xml.orig | sed -e "s@VCLOUD_PROVIDER_END@@g" > repository/conf/cloud-controller.xml

cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_PROVIDER_START@!--@g" > repository/conf/cloud-controller.xml

cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_PROVIDER_END@--@g" > repository/conf/cloud-controller.xml

cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_PROVIDER_START@!--@g" > repository/conf/cloud-controller.xml

cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_PROVIDER_END@--@g" > repository/conf/cloud-controller.xml

popd
