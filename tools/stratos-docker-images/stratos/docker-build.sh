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

set -e

#
# Stratos files to be copied to the image
#

rm -rf files/apache-stratos files/apache-stratos-*
unzip $STRATOS_SOURCE/products/stratos/modules/distribution/target/apache-stratos-*.zip -d files/
mv files/apache-stratos-* files/apache-stratos

#
# ActiveMQ files to be copied to the image
#

wget -N -q -P files/ http://archive.apache.org/dist/activemq/5.9.1/apache-activemq-5.9.1-bin.tar.gz
rm -rf files/apache-activemq-lib files/apache-activemq-lib-tmp
mkdir files/apache-activemq-lib files/apache-activemq-lib-tmp
tar -xzf files/apache-activemq-5.9.1-bin.tar.gz -C files/apache-activemq-lib-tmp
cp files/apache-activemq-lib-tmp/apache-activemq-5.9.1/lib/activemq-broker-5.9.1.jar files/apache-activemq-lib
cp files/apache-activemq-lib-tmp/apache-activemq-5.9.1/lib/activemq-client-5.9.1.jar files/apache-activemq-lib
cp files/apache-activemq-lib-tmp/apache-activemq-5.9.1/lib/geronimo-j2ee-management_1.1_spec-1.0.1.jar files/apache-activemq-lib
cp files/apache-activemq-lib-tmp/apache-activemq-5.9.1/lib/geronimo-jms_1.1_spec-1.1.1.jar files/apache-activemq-lib

#
# MySQL jar to be copied to the image
#

wget -N -q -P files/ http://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.29/mysql-connector-java-5.1.29.jar

#
# Stratos installer files to copy to the image
#

cp -rf $STRATOS_SOURCE/tools/stratos-installer files/

#
# Extensions files to copy to the image
#

cp -rf $STRATOS_SOURCE/extensions/ files/

docker build -t=apachestratos/stratos:$VERSION .
