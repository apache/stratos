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

tenant=""
cartridge=""
ads_git_url="localhost"

function help {
    echo "Usage:git-folder-structure  <mandatory arguments>"
    echo "    Usage:"
    echo "    	  git-folder-structure <tenant> <cartridge> [webapp=readme file description with space replace with #] "
    echo "    eg:"
    echo "    	  git-folder-structure tenant1 as webapp=copy#war#files#here"
    echo ""
}

function main {

if [[ (-z $tenant || -z $cartridge ) ]]; then
    help
    exit 1
fi

}

tenant=$1
cartridge=$2

if [[ (-n $tenant && -n $cartridge) ]]; then
	cd /tmp/
	rm -fr ${tenant}.${cartridge}
	git clone git@localhost:${tenant}.${cartridge}
	cd ${tenant}.${cartridge}
	git pull origin master
	shift
	shift
	for IN in "$@"; do
		IFS='=' read -ra ADDR <<< "$IN"
		mkdir -p ${ADDR[0]}
		echo ${ADDR[1]} | sed -e 's/#/\s/g' > ${ADDR[0]}/README.txt
		git add ${ADDR[0]}
		git commit -a -m 'Folder structure commit'
		git push origin master
	done
	
fi	

main
