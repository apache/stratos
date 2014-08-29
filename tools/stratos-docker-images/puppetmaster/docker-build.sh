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

set -e # exit on error

if [[ -z $STRATOS_SOURCE ]]; then
  echo "STRATOS_SOURCE environment variable must be set"
  exit 1
fi

ACTIVEMQ_URL="http://archive.apache.org/dist/activemq/5.9.1/apache-activemq-5.9.1-bin.tar.gz"

MYSQLJ_URL="http://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.29/mysql-connector-java-5.1.29.jar"

# if you change the tomcat version, you will need to change the version in the Dockerfile too
TOMCAT_URL="http://archive.apache.org/dist/tomcat/tomcat-7/v7.0.52/bin/apache-tomcat-7.0.52.tar.gz"

HAWTBUF_URL="http://repo1.maven.org/maven2/org/fusesource/hawtbuf/hawtbuf/1.2/hawtbuf-1.2.jar"

# if you change the JDK version, you will need to change the version in the Dockerfile too
JDK_URL="http://download.oracle.com/otn-pub/java/jdk/7u51-b13/jdk-7u51-linux-x64.tar.gz"

wget -N -c -P files $TOMCAT_URL

wget -N -c -P files \
  --no-cookies --no-check-certificate \
  --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
  ${JDK_URL}

cp -rf $STRATOS_SOURCE/tools/puppet3/manifests files/
cp -rf $STRATOS_SOURCE/tools/puppet3/modules files/

cp -f $STRATOS_SOURCE/products/cartridge-agent/modules/distribution/target/apache-stratos-cartridge-agent-*.zip files/

cp -f $STRATOS_SOURCE/products/load-balancer/modules/distribution/target/apache-stratos-load-balancer-*.zip files/

cd files/
tar -cvzf agentfiles.tgz apache-stratos-cartridge-agent-*.zip apache-stratos-load-balancer-*.zip 
cd ..

docker build -t=apachestratos/puppetmaster:$VERSION .
