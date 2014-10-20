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

# General commands
if [ "$(uname)" == "Darwin" ]; then
    # Do something under Mac OS X platform
        SED=`which gsed` && : || (echo "Command 'gsed' is not installed."; exit 10;)
else
    # Do something else under some other platform
    SED=`which sed` && : || (echo "Command 'sed' is not installed."; exit 10;)
fi

SLEEP=60
export LOG=$log_path/stratos-gce.log

source "./conf/setup.conf"

stratos_extract_path=$1

if [[ ! -d $log_path ]]; then
    mkdir -p $log_path
fi

pushd $stratos_extract_path

echo "Set vCloud provider specific info in repository/conf/cloud-controller.xml" >> $LOG

${SED} -i "s@VCLOUD_PROVIDER_START@!--@g" repository/conf/cloud-controller.xml
${SED} -i "s@VCLOUD_PROVIDER_END@--@g" repository/conf/cloud-controller.xml
${SED} -i "s@EC2_PROVIDER_START@!--@g" repository/conf/cloud-controller.xml
${SED} -i "s@EC2_PROVIDER_END@--@g" repository/conf/cloud-controller.xml
${SED} -i "s@OPENSTACK_PROVIDER_START@!--@g" repository/conf/cloud-controller.xml
${SED} -i "s@OPENSTACK_PROVIDER_END@--@g" repository/conf/cloud-controller.xml
${SED} -i "s@GCE_PROVIDER_START@@g" repository/conf/cloud-controller.xml
${SED} -i "s@GCE_IDENTITY@${gce_identity//@/\\@}@g" repository/conf/cloud-controller.xml
# first replace the newlines in $gce_credential to newline, then add that in the place where 
# GCE_CREDENTIAL is found in the xml file
${SED} -i "s@GCE_CREDENTIAL@${gce_credential//$'\n'/\\n}@g" repository/conf/cloud-controller.xml
${SED} -i "s/GCE_PROJECTNAME/$gce_projectname/g" repository/conf/cloud-controller.xml
${SED} -i "s@GCE_PROVIDER_END@@g" repository/conf/cloud-controller.xml

popd
