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
cartridge_private_key=""

function help {
    echo "Usage: copy-private-key <mandatory arguments>"
    echo "    Usage:"
    echo "    	  copy-private-key <instance ip> <cartridge private key>"
    echo "    eg:"
    echo "    	  copy-private-key 172.17.1.2 /tmp/foo-php"
    echo ""
}

function main {

if [[ (-z $instance_ip || -z $cartridge_private_key) ]]; then
    help
    exit 1
fi

}

instance_ip=$1
cartridge_private_key=$2

if [[ (-n $instance_ip && -n $cartridge_private_key) ]]; then
    scp -i ${cartridge_private_key} ${cartridge_private_key} ${user}@${instance_ip}:/home/${user}/.ssh/id_rsa
    ssh -o "BatchMode yes" -i ${cartridge_private_key} ${user}@${instance_ip} chown ${user}:${user} /home/${user}/.ssh/id_rsa
    ssh -o "BatchMode yes" -i ${cartridge_private_key} ${user}@${instance_ip} chmod 0600 /home/${user}/.ssh/id_rsa
fi

main
