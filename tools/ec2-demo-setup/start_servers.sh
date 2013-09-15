#!/bin/bash
# ----------------------------------------------------------------------------
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
# ----------------------------------------------------------------------------
#  Server start-up script for Apache Stratos
# ----------------------------------------------------------------------------

SLEEP=60
LOG=stratos-server.log
sc_path=/opt/apache-stratos-sc-3.0.0-incubating
cc_path=/opt/apache-stratos-cc-3.0.0-incubating
elb_path=/opt/apache-stratos-elb-3.0.0-incubating
agent_path=/opt/apache-stratos-agent-3.0.0-incubating
gitblit_path=/opt/GitBlit

start_servers() {

    echo ${cc_path} >> $LOG

    echo "Starting CC server ..." | tee -a $LOG
    nohup ${cc_path}/bin/stratos.sh -DapplyPatches &
    #echo "CC server started" | tee -a $LOG
    sleep 120

    echo ${sc_path} >> $LOG

    echo "Starting SC server ..." | tee -a $LOG
    nohup ${sc_path}/bin/stratos.sh -DapplyPatches &
    #echo "SC server started" | tee -a $LOG
    sleep $SLEEP


    echo ${elb_path} >> $LOG

    echo "Starting ELB server ..." | tee -a $LOG
    nohup ${elb_path}/bin/stratos.sh -DapplyPatches &
    #echo "ELB server started" | tee -a $LOG
    sleep $SLEEP

    echo ${agent_path} >> $LOG

    echo "Starting Agent server ..." | tee -a $LOG
    nohup ${agent_path}/bin/stratos.sh -DapplyPatches &
    #echo "Agent server started" | tee -a $LOG
    sleep $SLEEP

    echo "Starting internal Git Server ..." | tee -a $LOG
    cd ${gitblit_path}
    git config --global --bool --add http.sslVerify false
    nohup java -jar gitblit.jar --baseFolder data &
    #echo "GitBlit server started" | tee -a $LOG
    sleep $SLEEP


    echo "Stratos 2.0 servers started up successfully!"


}


while true; do

read -p "Make sure you have read the configuration guide and have setup properly, press [y] to continue, [n] to exit.   "  yn
    case $yn in
        [Yy]* ) start_servers;
		 break;;
        [Nn]* ) echo "Exiting..."; break;;
        * ) echo "Please answer [y] or [n].";;
    esac
done
