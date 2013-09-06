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
#  This script is invoked by setup.sh for configuring cartridges for Amazon EC2.
# ----------------------------------------------------------------------------

# Die on any error:
set -e

SLEEP=60
export LOG=$log_path/stratos-ec2-cartridge.log

source "./conf/setup.conf"

if [[ ! -d $log_path ]]; then
    mkdir -p $log_path
fi
   

echo "Creating payload directory ... " | tee $LOG
if [[ ! -d $cc_path/repository/resources/payload ]]; then
   mkdir -p $cc_path/repository/resources/payload
fi

echo "Creating cartridges directory ... " | tee $LOG
if [[ ! -d $cc_path/repository/deployment/server/cartridges/ ]]; then
   mkdir -p $cc_path/repository/deployment/server/cartridges/
fi


# Copy cartridge configuration files to CC
if [ "$(find ./cartridges/ec2/ -type f)" ]; then
    cp -f ./cartridges/ec2/*.xml $cc_path/repository/deployment/server/cartridges/
fi


pushd $cc_path
echo "Updating repository/deployment/server/cartridges/ec2-mysql.xml" | tee $LOG
# <iaasProvider type="openstack" >
#    <imageId>nova/d6e5dbe9-f781-460d-b554-23a133a887cd</imageId>
#    <property name="keyPair" value="stratos-demo"/>
#    <property name="instanceType" value="nova/1"/>
#    <property name="securityGroups" value="default"/>
#    <!--<property name="payload" value="resources/as.txt"/>-->
# </iaasProvider>
 

cp -f repository/deployment/server/cartridges/ec2-mysql.xml repository/deployment/server/cartridges/ec2-mysql.xml.orig
cat repository/deployment/server/cartridges/ec2-mysql.xml.orig | sed -e "s@<property name=\"keyPair\" value=\"*.*\"/>@<property name=\"keyPair\" value=\"$ec2_keypair_name\"/>@g" > repository/deployment/server/cartridges/ec2-mysql.xml

cp -f repository/deployment/server/cartridges/ec2-mysql.xml repository/deployment/server/cartridges/ec2-mysql.xml.orig
cat repository/deployment/server/cartridges/ec2-mysql.xml.orig | sed -e "s@<property name=\"instanceType\" value=\"*.*\"/>@<property name=\"instanceType\" value=\"$ec2_instance_type\"/>@g" > repository/deployment/server/cartridges/ec2-mysql.xml

cp -f repository/deployment/server/cartridges/ec2-mysql.xml repository/deployment/server/cartridges/ec2-mysql.xml.orig
cat repository/deployment/server/cartridges/ec2-mysql.xml.orig | sed -e "s@<property name=\"securityGroups\" value=\"*.*\"/>@<property name=\"securityGroups\" value=\"$ec2_security_groups\"/>@g" > repository/deployment/server/cartridges/ec2-mysql.xml

cp -f repository/deployment/server/cartridges/ec2-mysql.xml repository/deployment/server/cartridges/ec2-mysql.xml.orig
cat repository/deployment/server/cartridges/ec2-mysql.xml.orig | sed -e "s@<imageId>*.*</imageId>@<imageId>$ec2_region/$ec2_mysql_cartridge_image_id</imageId>@g" > repository/deployment/server/cartridges/ec2-mysql.xml

cp -f repository/deployment/server/cartridges/ec2-mysql.xml repository/deployment/server/cartridges/ec2-mysql.xml.orig
cat repository/deployment/server/cartridges/ec2-mysql.xml.orig | sed -e "s@STRATOS_DOMAIN@$stratos_domain@g" > repository/deployment/server/cartridges/ec2-mysql.xml


echo "Updating repository/deployment/server/cartridges/ec2-php.xml" | tee $LOG
# <iaasProvider type="openstack" >
#     <imageId>nova/250cd0bb-96a3-4ce8-bec8-7f9c1efea1e6</imageId>
#     <property name="keyPair" value="stratos-demo"/>
#     <property name="instanceType" value="nova/1"/>
#     <property name="securityGroups" value="default"/>
#     <!--<property name="payload" value="resources/as.txt"/>-->
# </iaasProvider>

cp -f repository/deployment/server/cartridges/ec2-php.xml repository/deployment/server/cartridges/ec2-php.xml.orig
cat repository/deployment/server/cartridges/ec2-php.xml.orig | sed -e "s@<property name=\"keyPair\" value=\"*.*\"/>@<property name=\"keyPair\" value=\"$ec2_keypair_name\"/>@g" > repository/deployment/server/cartridges/ec2-php.xml

cp -f repository/deployment/server/cartridges/ec2-php.xml repository/deployment/server/cartridges/ec2-php.xml.orig
cat repository/deployment/server/cartridges/ec2-php.xml.orig | sed -e "s@<property name=\"instanceType\" value=\"*.*\"/>@<property name=\"instanceType\" value=\"$ec2_instance_type\"/>@g" > repository/deployment/server/cartridges/ec2-php.xml

cp -f repository/deployment/server/cartridges/ec2-php.xml repository/deployment/server/cartridges/ec2-php.xml.orig
cat repository/deployment/server/cartridges/ec2-php.xml.orig | sed -e "s@<property name=\"securityGroups\" value=\"*.*\"/>@<property name=\"securityGroups\" value=\"$ec2_security_groups\"/>@g" > repository/deployment/server/cartridges/ec2-php.xml

cp -f repository/deployment/server/cartridges/ec2-php.xml repository/deployment/server/cartridges/ec2-php.xml.orig
cat repository/deployment/server/cartridges/ec2-php.xml.orig | sed -e "s@<imageId>*.*</imageId>@<imageId>$ec2_region/$ec2_php_cartridge_image_id</imageId>@g" > repository/deployment/server/cartridges/ec2-php.xml

cp -f repository/deployment/server/cartridges/ec2-php.xml repository/deployment/server/cartridges/ec2-php.xml.orig
cat repository/deployment/server/cartridges/ec2-php.xml.orig | sed -e "s@STRATOS_DOMAIN@$stratos_domain@g" > repository/deployment/server/cartridges/ec2-php.xml


echo "Updating repository/deployment/server/cartridges/ec2-tomcat.xml" | tee $LOG
# <iaasProvider type="openstack" >
#    <imageId>RegionOne/9701eb18-d7e1-4a53-a2bf-a519899d451c</imageId>
#    <property name="keyPair" value="manula_openstack"/>
#    <property name="instanceType" value="RegionOne/2"/>
#    <property name="securityGroups" value="im-security-group1"/>
#    <!--<property name="payload" value="resources/as.txt"/>-->
# </iaasProvider>

cp -f repository/deployment/server/cartridges/ec2-tomcat.xml repository/deployment/server/cartridges/ec2-tomcat.xml.orig
cat repository/deployment/server/cartridges/ec2-tomcat.xml.orig | sed -e "s@<property name=\"keyPair\" value=\"*.*\"/>@<property name=\"keyPair\" value=\"$ec2_keypair_name\"/>@g" > repository/deployment/server/cartridges/ec2-tomcat.xml

cp -f repository/deployment/server/cartridges/ec2-tomcat.xml repository/deployment/server/cartridges/ec2-tomcat.xml.orig
cat repository/deployment/server/cartridges/ec2-tomcat.xml.orig | sed -e "s@<property name=\"instanceType\" value=\"*.*\"/>@<property name=\"instanceType\" value=\"$ec2_instance_type\"/>@g" > repository/deployment/server/cartridges/ec2-tomcat.xml

cp -f repository/deployment/server/cartridges/ec2-tomcat.xml repository/deployment/server/cartridges/ec2-tomcat.xml.orig
cat repository/deployment/server/cartridges/ec2-tomcat.xml.orig | sed -e "s@<property name=\"securityGroups\" value=\"*.*\"/>@<property name=\"securityGroups\" value=\"$ec2_security_groups\"/>@g" > repository/deployment/server/cartridges/ec2-tomcat.xml

cp -f repository/deployment/server/cartridges/ec2-tomcat.xml repository/deployment/server/cartridges/ec2-tomcat.xml.orig
cat repository/deployment/server/cartridges/ec2-tomcat.xml.orig | sed -e "s@<imageId>*.*</imageId>@<imageId>$ec2_region/$ec2_tomcat_cartridge_image_id</imageId>@g" > repository/deployment/server/cartridges/ec2-tomcat.xml

cp -f repository/deployment/server/cartridges/ec2-tomcat.xml repository/deployment/server/cartridges/ec2-tomcat.xml.orig
cat repository/deployment/server/cartridges/ec2-tomcat.xml.orig | sed -e "s@STRATOS_DOMAIN@$stratos_domain@g" > repository/deployment/server/cartridges/ec2-tomcat.xml

popd # cc_path 
