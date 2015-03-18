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
            -Dlisten.address=localhost
            -Dthrift.client.config.file.path=${script_path}/../conf/thrift-client-config.xml
            -Djndi.properties.template.file.path=${script_path}/../conf/templates/jndi.properties.template
            -Djndi.properties.dir=${script_path}/../conf
            -Dlog4j.configuration=file://${script_path}/../conf/log4j.properties
            -Dparam.file.path=${script_path}/../payload/launch-params
            -Dextensions.dir=${script_path}/../extensions
            -Dcep.stats.publisher.enabled=true
            -Dlb.private.ip=
            -Dlb.public.ip=
            -Djavax.net.ssl.trustStore=CERT-TRUSTSTORE
            -Djavax.net.ssl.trustStorePassword=TRUSTSTORE-PASSWORD
	    -Denable.artifact.update=true
            -Dauto.commit=false
            -Dauto.checkout=true
            -Dartifact.update.interval=15
            -Denable.data.publisher=ENABLE-DATA-PUBLISHER
            -Dmonitoring.server.ip=MONITORING-SERVER-IP
	    -Dmonitoring.server.port=MONITORING-SERVER-PORT
	    -Dmonitoring.server.secure.port=MONITORING-SERVER-SECURE-PORT
	    -Dmonitoring.server.admin.username=MONITORING-SERVER-ADMIN-USERNAME
	    -Dmonitoring.server.admin.password=MONITORING-SERVER-ADMIN-PASSWORD
	    -Dlog.file.paths=LOG_FILE_PATHS
	    -DAPPLICATION_PATH=APP_PATH
            -Dsuper.tenant.repository.path=/repository/deployment/server/
            -Dtenant.repository.path=/repository/tenants/
	    -Dextension.instance.started=instance-started.sh
            -Dextension.start.servers=start-servers.sh
            -Dextension.instance.activated=instance-activated.sh
            -Dextension.artifacts.updated=artifacts-updated.sh
            -Dextension.clean=clean.sh
            -Dextension.mount.volumes=mount_volumes.sh
            -Dextension.member.started=member-started.sh
            -Dextension.member.activated=member-activated.sh
            -Dextension.member.suspended=member-suspended.sh
            -Dextension.member.terminated=member-terminated.sh
            -Dextension.complete.topology=complete-topology.sh
            -Dextension.complete.tenant=complete-tenant.sh
            -Dextension.domain.mapping.added=domain-mapping-added.sh
            -Dextension.domain.mapping.removed=domain-mapping-removed.sh
            -Dextension.artifacts.copy=artifacts-copy.sh
            -Dextension.tenant.subscribed=tenant-subscribed.sh
            -Dextension.tenant.unsubscribed=tenant-unsubscribed.sh"

# Uncomment below line to enable remote debugging
#debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

java -cp "${class_path}" ${properties} ${debug} org.apache.stratos.cartridge.agent.Main

# If you want to add your custom Cartridge Agent implementation, Please provide an implementation of 
# org.apache.stratos.cartridge.agent.CartridgeAgent as an argument to the Main class
#java -cp "${class_path}" ${properties} ${debug} org.apache.stratos.cartridge.agent.Main org.apache.stratos.cartridge.agent.CartridgeAgentABC
