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
#  This script is for cleaning the host machine where one or more of the Stratos servers are run.
# ----------------------------------------------------------------------------

source "./conf/setup.conf"

if [ "$UID" -ne "0" ]; then
	echo ; echo "  You must be root to run $0.  (Try running 'sudo bash' first.)" ; echo 
	exit 69
fi

function help {
    echo ""
    echo "Clean the host machine where one or more of the Stratos2 servers are run."
    echo "usage:"
    echo "clean.sh -u <mysql username> -p <mysql password>"
    echo ""
}

while getopts u:p: opts
do
  case $opts in
    u)
        mysql_user=${OPTARG}
        ;;
    p)
        mysql_pass=${OPTARG}
        ;;
    *)
        help
        #exit 1
        ;;
  esac
done

function helpclean {
    echo ""
    echo "Enter DB credentials if you need to clear Stratos DB"
    echo "usage:"
    echo "clean.sh -u <mysql username> -p <mysql password>"
    echo ""
}

function clean_validate {
    if [ -z $stratos_path ]; then
        echo "stratos_path is not set"
        exit 1
    fi
    if [ -z $log_path ]; then
        echo "log_path is not set"
        exit 1
    fi
}

clean_validate
if [[ ( -n $mysql_user && -n $mysql_pass ) ]]; then
	read -p "Please confirm that you want to remove stratos databases, servers and logs [y/n] " answer
	if [[ $answer != y ]] ; then
    		exit 1
	fi
fi
echo 'Stopping Carbon java processes'
#killing carbon processes
for pid in $(ps aux | grep "[o]rg.wso2.carbon.bootstrap.Bootstrap" | awk '{print $2}')
do
    echo "killing Carbon process $pid"
    kill $pid
done

#killing activemq
for pid in $(ps aux | grep "[a]pache-activemq" | awk '{print $2}')
do
    echo "killing ActiveMQ $pid"
    kill $pid
done

echo 'Waiting for applications to exit'
sleep 15

if [[ ( -n $mysql_user && -n $mysql_pass ) ]]; then
   echo 'Removing userstore database'
   mysql -u $mysql_user -p$mysql_pass -e "DROP DATABASE IF EXISTS $userstore_db_schema;"
fi

if [[ -d $stratos_path/scripts ]]; then
   echo 'Removing scripts'
   rm -rf $stratos_path/scripts
fi

if [[ -d $stratos_path ]]; then
   echo 'Removing Stratos'
   rm -rf $stratos_path/*
fi

echo 'Removing logs'
rm -rf $log_path/*
