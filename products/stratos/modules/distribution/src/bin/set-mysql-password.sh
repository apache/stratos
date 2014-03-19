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
password=""


function help {
    echo "Usage: set-mysql-password <mandatory arguments>"
    echo "    Usage:"
    echo "    	  set-mysql-password <instance ip> <cartridge private key> <password>"
    echo "    eg:"
    echo "    	  set-mysql-password 172.17.1.2 /tmp/foo-php qazxsw"
    echo ""
}

function main {

if [[ (-z $password || -z $instance_ip) ]]; then
    help
    exit 1
fi

}

instance_ip=$1
cartridge_private_key=$2
password=$3

echo "#!/bin/bash
echo \"GRANT ALL PRIVILEGES ON *.* TO 'root'@'%'   IDENTIFIED BY '${password}' WITH GRANT OPTION;flush privileges;\" | mysql -uroot -p${password}
" > /tmp/${password}.sh

if [[ (-n $password && -n $instance_ip) ]]; then
	ssh -o "BatchMode yes" -i ${cartridge_private_key} ${user}@${instance_ip} mysqladmin -u root password "${password}"
#	ssh -o "BatchMode yes" -i ${cartridge_private_key} ${user}@${instance_ip} echo "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%'   IDENTIFIED BY '${password}' WITH GRANT OPTION;flush privileges;" | mysql -u root -p"${password}"
	scp -i ${cartridge_private_key} /tmp/${password}.sh ${user}@${instance_ip}:
	ssh -o "BatchMode yes" -i ${cartridge_private_key} ${user}@${instance_ip} chmod 755 /home/${user}/${password}.sh
	ssh -o "BatchMode yes" -i ${cartridge_private_key} ${user}@${instance_ip} /home/${user}/${password}.sh
	ssh -o "BatchMode yes" -i ${cartridge_private_key} ${user}@${instance_ip} rm /home/${user}/${password}.sh
fi
rm /tmp/${password}.sh

main
