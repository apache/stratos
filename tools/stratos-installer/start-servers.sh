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

if [[ -f ./conf/setup.conf ]]; then
    source "./conf/setup.conf"
fi

function help {
    echo ""
    echo "Give one or more of the servers to start on this machine. The available servers are"
    echo "cc, elb, agent, sc, all. 'all' means you need to start all servers."
    echo "usage:"
    echo "setup.sh -p\"<product list>\""
    echo "eg."
    echo "setup.sh -p\"cc elb\""
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
    if [[ $x = "cc" ]]; then
        cc="true"
    fi
    if [[ $x = "elb" ]]; then
        elb="true"
    fi
    if [[ $x = "agent" ]]; then
        agent="true"
    fi
    if [[ $x = "sc" ]]; then
        sc="true"
    fi
    if [[ $x = "all" ]]; then
        cc="true"
        elb="true"
        agent="true"
        sc="true"
        bam="true"
    fi
    if [[ $x = "demo" ]]; then
        demo="true"
        cc="true"
        elb="true"
        agent="true"
        sc="true"
	bam="true"
    fi
done
product_list=`echo $product_list | sed 's/^ *//g' | sed 's/ *$//g'`
if [[ -z $product_list || $product_list = "" ]]; then
    help
    exit 1
fi


if [[ $cc = "true" ]]; then
    echo ${cc_path}

    echo "Starting CC server ..." >> $LOG
    nohup ${cc_path}/bin/stratos.sh -DportOffset=$cc_port_offset &
    echo "CC server started" >> $LOG
    sleep $SLEEP
    sleep $SLEEP
fi

if [[ $elb = "true" ]]; then
    echo ${elb_path} 

    echo "Starting ELB server ..." >> $LOG
    nohup ${elb_path}/bin/stratos.sh -DportOffset=$elb_port_offset &
    echo "ELB server started" >> $LOG
    sleep $SLEEP
fi

if [[ $agent = "true" ]]; then
    echo ${agent_path}

    echo "Starting AGENT server ..." >> $LOG
    nohup ${agent_path}/bin/stratos.sh -DportOffset=$agent_port_offset &
    echo "AGENT server started" >> $LOG
    sleep $SLEEP
fi


if [[ $sc = "true" ]]; then
    
    echo ${sc_path}

    echo "Starting SC server ..." >> $LOG
    nohup ${sc_path}/bin/stratos.sh -DportOffset=$sc_port_offset &
    echo "SC server started" >> $LOG
    sleep $SLEEP

fi


