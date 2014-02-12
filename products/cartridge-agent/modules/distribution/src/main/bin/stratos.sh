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

echo "Starting cartridge agent..."
script_path="$( cd -P "$( dirname "$SOURCE" )" && pwd )/`dirname $0`"
lib_path=${script_path}/../lib/
class_path=`echo ${lib_path}/*.jar | tr ' ' ':'`
properties="-Dmb.ip=MB-IP
            -Dmb.port=MB-PORT
            -Dthrift.receiver.ip=CEP-IP
            -Dthrift.receiver.port=CEP-PORT
            -Djndi.properties.template.file.path=${script_path}/../conf/templates/jndi.properties.template
            -Djndi.properties.dir=${script_path}/../conf
            -Dlog4j.properties.file.path=${script_path}/../conf/log4j.properties
            -Dparam.file.path=/opt/apache-stratos-cartridge-agent/payload/launch-params
            -Dextensions.dir=${script_path}/../extensions
            -Dcep.stats.publisher.enabled=true
            -Djavax.net.ssl.trustStore=CERT-TRUSTSTORE
            -Djavax.net.ssl.trustStorePassword=TRUSTSTORE-PASSWORD
	    -Denable.artifact.update=true
	    -Dartifact.update.interval=10
	    -Denable.data.publisher=false
            -Dmonitoring.server.ip=MONITORING-SERVER-IP
	    -Dmonitoring.server.port=MONITORING-SERVER-PORT
	    -Dmonitoring.server.secure.port=MONITORING-SERVER-SECURE-PORT
	    -Dmonitoring.server.admin.username=MONITORING-SERVER-ADMIN-USERNAME
	    -Dmonitoring.server.admin.password=MONITORING-SERVER-ADMIN-PASSWORD"

# Uncomment below line to enable remote debugging
#debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

java -cp "${class_path}" ${properties} ${debug} org.apache.stratos.cartridge.agent.Main
