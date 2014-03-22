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
# This script is for starting Apache Stratos servers.
# ----------------------------------------------------------------------------

# Die on any error:
set -e
product_list=$1
export LOG=$log_path/stratos.log
SLEEP=40

if [[ -f ./conf/stratos-setup.conf ]]; then
    source "./conf/stratos-setup.conf"
fi

function help {
    echo ""
    echo "Give one or more of the servers to start on this machine. The available servers are"
    echo "mb, cc, as, sm, cep, all. 'all' means you need to start all servers."
    echo "usage:"
    echo "stratos-start-servers.sh -p\"<product list>\""
    echo "eg."
    echo "stratos-start-servers.sh -p\"cc sm\""
    echo ""
}

while getopts p: opts
do
  case $opts in
    p)
        product_list=${OPTARG}
        echo $product_list
        ;;
    *)
        help
        exit 1
        ;;
  esac
done
arr=$(echo $product_list | tr ";" "\n")

for x in $arr
do
    if [[ $x = "mb" ]]; then
        mb="true"
    fi
    if [[ $x = "cep" ]]; then
        cep="true"
    fi
    if [[ $x = "cc" ]]; then
        cc="true"
    fi
    if [[ $x = "as" ]]; then
        as="true"
    fi
    if [[ $x = "sm" ]]; then
        sm="true"
    fi
    if [[ $x = "all" ]]; then
	mb="true"
        cc="true"
        as="true"
        sm="true"
        cep="true"
    fi
done
product_list=`echo $product_list | sed 's/^ *//g' | sed 's/ *$//g'`
if [[ -z $product_list || $product_list = "" ]]; then
    help
    exit 1
fi

if [[ $mb = "true" ]]; then
    echo ${mb_path}

    echo "Starting ActiveMQ server ..." >> $LOG
    bits=$(uname -m)
    if [[ $bits =~ 64 ]]; then
	${mb_path}/bin/linux-x86-64/activemq start
    elif [[ $bits =~ 32 ]]; then
	${mb_path}/bin/linux-x86-32/activemq start
    else
	${mb_path}/bin/activemq start
    fi
    
    echo "ActiveMQ server started" >> $LOG
    sleep $SLEEP
    sleep $SLEEP
fi

echo ${stratos_dist_path}

    echo "Starting Stratos server ..." >> $LOG
    ${stratos_dist_path}/bin/stratos.sh start
    echo "Stratos server started" >> $LOG
    sleep $SLEEP
    sleep $SLEEP


