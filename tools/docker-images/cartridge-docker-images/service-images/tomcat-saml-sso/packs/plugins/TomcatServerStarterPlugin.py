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
        env_var["JAVA_HOME"] = "/opt/jdk1.7.0_67"
        p = subprocess.Popen(tomcat_start_command, env=env_var, shell=True)
        output, errors = p.communicate()
        log.debug("Tomcat server started")








