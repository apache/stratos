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
#  This script is invoked by setup.sh for configuring cartridges for OpenStack.
# ----------------------------------------------------------------------------

# Die on any error:
set -e

SLEEP=60
export LOG=$log_path/stratos-openstack-cartridge.log

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


# Copy the cartridge specific configuration files into the CC
if [ "$(find ./cartridges/openstack/ -type f)" ]; then
    cp -f ./cartridges/openstack/*.xml $cc_path/repository/deployment/server/cartridges/
fi

pushd $cc_path
echo "Updating repository/deployment/server/cartridges/openstack-mysql.xml" | tee $LOG
# <iaasProvider type="openstack" >
#    <imageId>nova/d6e5dbe9-f781-460d-b554-23a133a887cd</imageId>
#    <property name="keyPair" value="stratos-demo"/>
#    <property name="instanceType" value="nova/1"/>
#    <property name="securityGroups" value="default"/>
#    <!--<property name="payload" value="resources/as.txt"/>-->
# </iaasProvider>
 

cp -f repository/deployment/server/cartridges/openstack-mysql.xml repository/deployment/server/cartridges/openstack-mysql.xml.orig
cat repository/deployment/server/cartridges/openstack-mysql.xml.orig | sed -e "s@<property name=\"keyPair\" value=\"*.*\"/>@<property name=\"keyPair\" value=\"$openstack_keypair_name\"/>@g" > repository/deployment/server/cartridges/openstack-mysql.xml

cp -f repository/deployment/server/cartridges/openstack-mysql.xml repository/deployment/server/cartridges/openstack-mysql.xml.orig
cat repository/deployment/server/cartridges/openstack-mysql.xml.orig | sed -e "s@<property name=\"instanceType\" value=\"*.*\"/>@<property name=\"instanceType\" value=\"$openstack_instance_type_tiny\"/>@g" > repository/deployment/server/cartridges/openstack-mysql.xml

cp -f repository/deployment/server/cartridges/openstack-mysql.xml repository/deployment/server/cartridges/openstack-mysql.xml.orig
cat repository/deployment/server/cartridges/openstack-mysql.xml.orig | sed -e "s@<property name=\"securityGroups\" value=\"*.*\"/>@<property name=\"securityGroups\" value=\"$openstack_security_groups\"/>@g" > repository/deployment/server/cartridges/openstack-mysql.xml

cp -f repository/deployment/server/cartridges/openstack-mysql.xml repository/deployment/server/cartridges/openstack-mysql.xml.orig
cat repository/deployment/server/cartridges/openstack-mysql.xml.orig | sed -e "s@<imageId>*.*</imageId>@<imageId>$nova_region/$openstack_mysql_cartridge_image_id</imageId>@g" > repository/deployment/server/cartridges/openstack-mysql.xml

cp -f repository/deployment/server/cartridges/openstack-mysql.xml repository/deployment/server/cartridges/openstack-mysql.xml.orig
cat repository/deployment/server/cartridges/openstack-mysql.xml.orig | sed -e "s@STRATOS_DOMAIN@$stratos_domain@g" > repository/deployment/server/cartridges/openstack-mysql.xml


echo "Updating repository/deployment/server/cartridges/openstack-php.xml" | tee $LOG
# <iaasProvider type="openstack" >
#     <imageId>nova/250cd0bb-96a3-4ce8-bec8-7f9c1efea1e6</imageId>
#     <property name="keyPair" value="stratos-demo"/>
#     <property name="instanceType" value="nova/1"/>
#     <property name="securityGroups" value="default"/>
#     <!--<property name="payload" value="resources/as.txt"/>-->
# </iaasProvider>

cp -f repository/deployment/server/cartridges/openstack-php.xml repository/deployment/server/cartridges/openstack-php.xml.orig
cat repository/deployment/server/cartridges/openstack-php.xml.orig | sed -e "s@<property name=\"keyPair\" value=\"*.*\"/>@<property name=\"keyPair\" value=\"$openstack_keypair_name\"/>@g" > repository/deployment/server/cartridges/openstack-php.xml

cp -f repository/deployment/server/cartridges/openstack-php.xml repository/deployment/server/cartridges/openstack-php.xml.orig
cat repository/deployment/server/cartridges/openstack-php.xml.orig | sed -e "s@<property name=\"instanceType\" value=\"*.*\"/>@<property name=\"instanceType\" value=\"$openstack_instance_type_tiny\"/>@g" > repository/deployment/server/cartridges/openstack-php.xml

cp -f repository/deployment/server/cartridges/openstack-php.xml repository/deployment/server/cartridges/openstack-php.xml.orig
cat repository/deployment/server/cartridges/openstack-php.xml.orig | sed -e "s@<property name=\"securityGroups\" value=\"*.*\"/>@<property name=\"securityGroups\" value=\"$openstack_security_groups\"/>@g" > repository/deployment/server/cartridges/openstack-php.xml

cp -f repository/deployment/server/cartridges/openstack-php.xml repository/deployment/server/cartridges/openstack-php.xml.orig
cat repository/deployment/server/cartridges/openstack-php.xml.orig | sed -e "s@<imageId>*.*</imageId>@<imageId>$nova_region/$openstack_php_cartridge_image_id</imageId>@g" > repository/deployment/server/cartridges/openstack-php.xml

cp -f repository/deployment/server/cartridges/openstack-php.xml repository/deployment/server/cartridges/openstack-php.xml.orig
cat repository/deployment/server/cartridges/openstack-php.xml.orig | sed -e "s@STRATOS_DOMAIN@$stratos_domain@g" > repository/deployment/server/cartridges/openstack-php.xml


echo "Updating repository/deployment/server/cartridges/openstack-tomcat.xml" | tee $LOG
# <iaasProvider type="openstack" >
#    <imageId>RegionOne/9701eb18-d7e1-4a53-a2bf-a519899d451c</imageId>
#    <property name="keyPair" value="manula_openstack"/>
#    <property name="instanceType" value="RegionOne/2"/>
#    <property name="securityGroups" value="im-security-group1"/>
#    <!--<property name="payload" value="resources/as.txt"/>-->
# </iaasProvider>

cp -f repository/deployment/server/cartridges/openstack-tomcat.xml repository/deployment/server/cartridges/openstack-tomcat.xml.orig
cat repository/deployment/server/cartridges/openstack-tomcat.xml.orig | sed -e "s@<property name=\"keyPair\" value=\"*.*\"/>@<property name=\"keyPair\" value=\"$openstack_keypair_name\"/>@g" > repository/deployment/server/cartridges/openstack-tomcat.xml

cp -f repository/deployment/server/cartridges/openstack-tomcat.xml repository/deployment/server/cartridges/openstack-tomcat.xml.orig
cat repository/deployment/server/cartridges/openstack-tomcat.xml.orig | sed -e "s@<property name=\"instanceType\" value=\"*.*\"/>@<property name=\"instanceType\" value=\"$openstack_instance_type_tiny\"/>@g" > repository/deployment/server/cartridges/openstack-tomcat.xml

cp -f repository/deployment/server/cartridges/openstack-tomcat.xml repository/deployment/server/cartridges/openstack-tomcat.xml.orig
cat repository/deployment/server/cartridges/openstack-tomcat.xml.orig | sed -e "s@<property name=\"securityGroups\" value=\"*.*\"/>@<property name=\"securityGroups\" value=\"$openstack_security_groups\"/>@g" > repository/deployment/server/cartridges/openstack-tomcat.xml

cp -f repository/deployment/server/cartridges/openstack-tomcat.xml repository/deployment/server/cartridges/openstack-tomcat.xml.orig
cat repository/deployment/server/cartridges/openstack-tomcat.xml.orig | sed -e "s@<imageId>*.*</imageId>@<imageId>$nova_region/$openstack_tomcat_cartridge_image_id</imageId>@g" > repository/deployment/server/cartridges/openstack-tomcat.xml

cp -f repository/deployment/server/cartridges/openstack-tomcat.xml repository/deployment/server/cartridges/openstack-tomcat.xml.orig
cat repository/deployment/server/cartridges/openstack-tomcat.xml.orig | sed -e "s@STRATOS_DOMAIN@$stratos_domain@g" > repository/deployment/server/cartridges/openstack-tomcat.xml

popd # cc_path
