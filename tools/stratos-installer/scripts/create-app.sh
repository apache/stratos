#!/bin/bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#

user="ubuntu"
instance_ip=""
app_path=""
repo=""
cartridge_private_key=""
ads_git_url=""

function help {
    echo "Usage: create-app <mandatory arguments>"
    echo "    Usage:"
    echo "    	  create-app <instance_ip> <repo> <app path> <cartridge_private_key> <ADS git URL or ADS IP>"
    echo "    eg:"
    echo "    	  create-app 172.17.1.1 <foo.myapp.php.git> /var/www/myapp /tmp/foo-php 172.17.1.100"
    echo ""
}

function main {

if [[ (-z $instance_ip || -z $app_path || -z $repo || -z $cartridge_private_key ) ]]; then
    help
    exit 1
fi

}

instance_ip=$1
repo=$2
app_path=$3
cartridge_private_key=$4
ads_git_url=$5

if [[ (-n $instance_ip && -n $app_path && -n $repo && -n $cartridge_private_key ) ]]; then
    ssh -o "BatchMode yes" -i ${cartridge_private_key} ${user}@${instance_ip} sudo git clone git@${ads_git_url}:${repo} $app_path
fi

main
