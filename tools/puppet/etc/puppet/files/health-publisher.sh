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

echo "Starting health publisher..."

lib_path=./../lib/

class_path=${lib_path}andes-client-0.13.wso2v8.jar:${lib_path}ant-1.7.0.jar:${lib_path}ant-1.7.0.wso2v1.jar:${lib_path}ant-launcher-1.7.0.jar:${lib_path}apache-stratos-health-publisher-4.0.0-SNAPSHOT.jar:${lib_path}axiom-1.2.11.wso2v4.jar:${lib_path}axiom-api-1.2.11.jar:${lib_path}axiom-impl-1.2.11.jar:${lib_path}axis2-1.6.1.wso2v10.jar:${lib_path}commons-cli-1.0.jar:${lib_path}commons-codec-1.8.jar:${lib_path}commons-fileupload-1.2.0.wso2v1.jar:${lib_path}commons-fileupload-1.2.jar:${lib_path}commons-httpclient-3.1.0.wso2v2.jar:${lib_path}commons-httpclient-3.1.jar:${lib_path}commons-io-2.0.jar:${lib_path}commons-lang-1.0.jar:${lib_path}commons-lang3-3.1.jar:${lib_path}commons-logging-1.1.1.jar:${lib_path}commons-pool-1.5.0.wso2v1.jar:${lib_path}commons-pool-1.5.jar:${lib_path}dom4j-1.6.1.jar:${lib_path}geronimo-activation_1.1_spec-1.0.2.jar:${lib_path}geronimo-javamail_1.4_spec-1.6.jar:${lib_path}geronimo-jms_1.1_spec-1.1.0.wso2v1.jar:${lib_path}geronimo-jms_1.1_spec-1.1.jar:${lib_path}geronimo-stax-api_1.0_spec-1.0.1.jar:${lib_path}gson-2.2.4.jar:${lib_path}httpclient-4.1.1-wso2v1.jar:${lib_path}httpclient-4.2.5.jar:${lib_path}httpcore-4.1.0-wso2v1.jar:${lib_path}httpcore-4.2.4.jar:${lib_path}icu4j-2.6.1.jar:${lib_path}javax.servlet-3.0.0.v201112011016.jar:${lib_path}jaxen-1.1.1.jar:${lib_path}jdom-1.0.jar:${lib_path}jline-0.9.94.jar:${lib_path}json-2.0.0.wso2v1.jar:${lib_path}junit-3.8.1.jar:${lib_path}libthrift-0.7.wso2v1.jar:${lib_path}libthrift-0.9.1.jar:${lib_path}log4j-1.2.13.jar:${lib_path}not-yet-commons-ssl-0.3.9.jar:${lib_path}org.apache.log4j-1.2.13.v200706111418.jar:${lib_path}org.apache.stratos.messaging-4.0.0-SNAPSHOT.jar:${lib_path}org.eclipse.osgi-3.8.1.v20120830-144521.jar:${lib_path}org.eclipse.osgi.services-3.3.100.v20120522-1822.jar:${lib_path}org.wso2.carbon.base-4.2.0.jar:${lib_path}org.wso2.carbon.core.common-4.2.0.jar:${lib_path}org.wso2.carbon.databridge.agent.thrift-4.2.0.jar:${lib_path}org.wso2.carbon.databridge.commons-4.2.0.jar:${lib_path}org.wso2.carbon.databridge.commons.thrift-4.2.0.jar:${lib_path}org.wso2.carbon.logging-4.1.0.jar:${lib_path}org.wso2.carbon.queuing-4.2.0.jar:${lib_path}org.wso2.carbon.registry.api-4.2.0.jar:${lib_path}org.wso2.carbon.securevault-4.2.0.jar:${lib_path}org.wso2.carbon.user.api-4.2.0.jar:${lib_path}org.wso2.carbon.utils-4.2.0.jar:${lib_path}org.wso2.securevault-1.0.0-wso2v2.jar:${lib_path}slf4j-1.5.10.wso2v1.jar:${lib_path}slf4j-api-1.7.5.jar:${lib_path}slf4j-log4j12-1.7.5.jar:${lib_path}smack-3.0.4.wso2v1.jar:${lib_path}smackx-3.0.4.wso2v1.jar:${lib_path}wstx-asl-3.2.9.jar:${lib_path}xalan-2.6.0.jar:${lib_path}xercesImpl-2.6.2.jar:${lib_path}xml-apis-1.3.02.jar:${lib_path}xmlParserAPIs-2.6.2.jar:${lib_path}xom-1.0.jar

current_path=`pwd`

${JAVA_HOME}/bin/java -cp $class_path -Dmember.id=$1 -Dkey.file.path=$current_path/../security/client-truststore.jks -Dthrift.receiver.ip=$2 -Dthrift.receiver.port=$3 -Dopen.ports=$4 -Dcluster.id=$5 -Dpartition.id=$6 org.apache.stratos.cartridge.agent.health.publisher.Main $*

echo "Health publisher completed"
