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
        exit 1
        ;;
  esac
done

function helpclean {
    echo ""
    echo "usage:"
    echo "clean.sh -u <mysql username> -p <mysql password>"
    echo ""
}

function clean_validate {
    if [[ ( -z $mysql_user || -z $mysql_pass ) ]]; then
        helpclean
        exit 1
    fi
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

read -p "Please confirm that you want to remove stratos databases, servers and logs [y/n] " answer
if [[ $answer != y ]] ; then
    exit 1
fi

echo 'Stopping all java processes'
killall java
echo 'Waiting for applications to exit'
sleep 15

echo 'Removing stratos_foundation database'
mysql -u $mysql_user -p$mysql_pass -e "DROP DATABASE IF EXISTS stratos_foundation;"

echo 'Removing userstore database'
mysql -u $mysql_user -p$mysql_pass -e "DROP DATABASE IF EXISTS userstore;"

if [[ -d $stratos_path/scripts ]]; then
   echo 'Removing scripts'
   rm -rf $stratos_path/scripts
fi

if [[ -d $mb_path ]]; then
   echo 'Removing MB'
   rm -rf $mb_path
fi

if [[ -d $cep_path ]]; then
   echo 'Removing CEP'
   rm -rf $cep_path
fi

if [[ -d $cc_path ]]; then
   echo 'Removing CC'
   rm -rf $cc_path
fi

if [[ -d $as_path ]]; then
   echo 'Removing AS'
   rm -rf $as_path
fi

if [[ -d $sm_path ]]; then
   echo 'Removing SM'
   rm -rf $sm_path
fi

echo 'Removing logs'
rm -rf $log_path/*

