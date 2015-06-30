# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import mdsclient
from plugins.contracts import ICartridgeAgentPlugin
import time
import zipfile
import subprocess
from modules.util.log import LogFactory
import os


class TomcatServerStarterPlugin(ICartridgeAgentPlugin):

    def run_plugin(self, values):
        log = LogFactory().get_log(__name__)
        log.info("Starting tomcat server starter plugin...")

        # wait till SAML_ENDPOINT becomes available
        mds_response = None
        while mds_response is None:
            log.debug("Waiting for SAML_ENDPOINT to be available from metadata service for app ID: %s" % values["APPLICATION_ID"])
            time.sleep(5)
            mds_response = mdsclient.get(app=True)
            if mds_response is not None and mds_response.properties.get("SAML_ENDPOINT") is None:
                mds_response = None

        saml_endpoint = mds_response.properties["SAML_ENDPOINT"]
        log.debug("SAML_ENDPOINT value read from Metadata service: %s" % saml_endpoint)

        # start tomcat
        tomcat_start_command = "exec /opt/tomcat/bin/startup.sh"
        log.info("Starting Tomcat server: [command] %s, [STRATOS_SAML_ENDPOINT] %s" % (tomcat_start_command, saml_endpoint))
        env_var = os.environ.copy()
        env_var["STRATOS_SAML_ENDPOINT"] = saml_endpoint
        env_var["STRATOS_HOST_NAME"] = values["HOST_NAME"]

        log.info("Reading port mappings...")
        port_mappings_str = values["PORT_MAPPINGS"]
        tomcat_http_port = None

        # port mappings format: """NAME:mgt-console|PROTOCOL:https|PORT:4500|PROXY_PORT:8443;
        #                          NAME:tomcat-http|PROTOCOL:http|PORT:4501|PROXY_PORT:7280;"""

        log.info("Port mappings: %s" % port_mappings_str)
        if port_mappings_str is not None:

            port_mappings_array = port_mappings_str.split(";")
            if port_mappings_array:

                for port_mapping in port_mappings_array:
                    log.debug("port_mapping: %s" % port_mapping)
                    name_value_array = port_mapping.split("|")
                    name = name_value_array[0].split(":")[1]
                    protocol = name_value_array[1].split(":")[1]
                    port = name_value_array[2].split(":")[1]
                    if name == "tomcat-http" and protocol == "http":
                        tomcat_http_port = port

        log.info("Kubernetes service port of tomcat http transport: %s" % tomcat_http_port)
        env_var["STRATOS_HOST_PORT"] = tomcat_http_port

        p = subprocess.Popen(tomcat_start_command, env=env_var, shell=True)
        output, errors = p.communicate()
        log.debug("Tomcat server started")

        log.info("Tomcat server starter plugin completed")









