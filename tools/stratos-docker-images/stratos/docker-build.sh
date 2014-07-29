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

cp -f $STRATOS_SOURCE/products/stratos/modules/distribution/target/apache-stratos-*.zip files/apache-stratos.zip

wget -N -q -P files/ http://archive.apache.org/dist/activemq/5.9.1/apache-activemq-5.9.1-bin.tar.gz

wget -N -q -P files/ http://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.29/mysql-connector-java-5.1.29.jar

cp -rf $STRATOS_SOURCE/tools/stratos-installer/ files/

cp -rf $STRATOS_SOURCE/extensions/ files/

sudo docker build -t=apachestratos/stratos .
