#!/bin/bash
# ---------------------------------------------------------------
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
# ---------------------------------------------------------------

set -e

prgdir=`dirname "$0"`
script_path=`cd "$prgdir"; pwd`
pca_distribution_path=`cd "$script_path/../../../products/python-cartridge-agent/distribution/"; pwd`

pushd ${pca_distribution_path}
mvn clean install -Dmaven.test.skip=true
cp -vf target/apache-stratos-python-cartridge-agent-4.1.0.zip ${script_path}/base-image/packs/
popd

pushd ${script_path}/base-image/
echo "Building base docker image..."
docker build -t stratos/base-image:4.1.0 .

pushd ${script_path}/service-images/php
echo "Building php docker image..."
docker build -t stratos/php:4.1.0 .

pushd ${script_path}/service-images/tomcat
echo "Building tomcat docker image..."
docker build -t stratos/tomcat:4.1.0 .

