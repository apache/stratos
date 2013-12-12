#!/bin/bash
# --------------------------------------------------------------
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# --------------------------------------------------------------

# --------------------------------------------------------------
# Load Balancer Start Script
# --------------------------------------------------------------
#
# This script will update the load balancer configuration with 
# parameter values received via the payload and start the server.
#

set -e # Terminate on any error
export LOG="/var/log/apache-stratos/start-load-balancer.log"
export JAVA_HOME=/opt/jdk1.6.0_45

script_home=/opt/apache-stratos-cartridge-agent/load-balancer
lb_home=/opt/apache-stratos-load-balancer
lb_conf_path=$lb_home/repository/conf

echo "Generating loadbalancer.conf file..." | tee -a $LOG
mb_ip=$1
mb_port=$2
cep_ip=$3
cep_port=$4
lb_cluster_id=$5

echo "mb-ip: $mb_ip" | tee -a $LOG
echo "mb-port: $mb_port" | tee -a $LOG
echo "cep-ip: $cep_ip" | tee -a $LOG
echo "cep-port: $cep_port" | tee -a $LOG
echo "lb-cluster-id: $lb_cluster_id" | tee -a $LOG

cp -f $script_home/templates/loadbalancer.conf.template $script_home/loadbalancer.conf.orig
cat $script_home/loadbalancer.conf.orig | sed -e "s@MB_IP@$mb_ip@g" > $script_home/loadbalancer.conf

cp -f $script_home/loadbalancer.conf $script_home/loadbalancer.conf.orig
cat $script_home/loadbalancer.conf.orig | sed -e "s@MB_PORT@$mb_port@g" > $script_home/loadbalancer.conf
    
cp -f $script_home/loadbalancer.conf $script_home/loadbalancer.conf.orig
cat $script_home/loadbalancer.conf.orig | sed -e "s@CEP_IP@$cep_ip@g" > $script_home/loadbalancer.conf

cp -f $script_home/loadbalancer.conf $script_home/loadbalancer.conf.orig
cat $script_home/loadbalancer.conf.orig | sed -e "s@CEP_PORT@$cep_port@g" > $script_home/loadbalancer.conf

cp -f $script_home/loadbalancer.conf $script_home/loadbalancer.conf.orig
cat $script_home/loadbalancer.conf.orig | sed -e "s@LB_CLUSTER_ID@$lb_cluster_id@g" > $script_home/loadbalancer.conf

rm $script_home/loadbalancer.conf.orig

echo "Moving generated loadbalancer.conf to $lb_conf_path" | tee -a $LOG
mv -f $script_home/loadbalancer.conf $lb_conf_path

echo "Starting load balancer..." | tee -a $LOG
sh $lb_home/bin/stratos.sh start &

echo "Load balancer started" | tee -a $LOG


