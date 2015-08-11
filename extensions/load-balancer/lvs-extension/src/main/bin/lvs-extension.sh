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

echo "Starting lvs extension..."
script_path="$( cd -P "$( dirname "$SOURCE" )" && pwd )/`dirname $0`"
lib_path=${script_path}/../lib/
class_path=`echo ${lib_path}/*.jar | tr ' ' ':'`
properties="-Dlvs.private.ip=127.0.0.1
            -Dexecutable.file.path=lvs
            -Djndi.properties.dir=${script_path}/../conf
            -Dtemplates.path=${script_path}/../templates
            -Dtemplates.name=keepalived.conf.template
            -Dscripts.path=${script_path}/../scripts
            -Dconf.file.path=/tmp/keepalived.conf
            -Dstats.socket.file.path=/tmp/nginx-stats.socket
            -Dlog4j.properties.file.path=${script_path}/../conf/log4j.properties
            -Djavax.net.ssl.trustStore=${script_path}/../security/client-truststore.jks
            -Djavax.net.ssl.trustStorePassword=wso2carbon
            -Dthrift.client.config.file.path=${script_path}/../conf/thrift-client-config.xml
            -Dcep.stats.publisher.enabled=false
            -Dthrift.receiver.ip=127.0.0.1
            -Dthrift.receiver.port=7615
            -Dnetwork.partition.id=network-partition-1
            -Dcluster.id=cluster-1
            -Dservice.name=service-1
            -Dlvs.service.virtualip.set=tomcat2|192.168.56.40,tomcat1|192.168.56.41
            -Dschedule.algorithm=rr
            -Dserver.state=MASTER
            -Dkeepalived=false"


# Uncomment below line to enable remote debugging
#debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

java -cp "${class_path}" ${properties} ${debug} org.apache.stratos.lvs.extension.Main $*
