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


class TomcatWSO2ISMetadataPublisher(ICartridgeAgentPlugin):

    def run_plugin(self, values):
        # publish callback and issuer id from tomcat for IS to pickup
        publish_data = mdsclient.MDSPutRequest()
        # hostname_entry = {"key": "TOMCAT_HOSTNAME", "values": member_hostname}
        cluster_hostname = values["HOST_NAME"]
        # set port name checking if lb is present or not
        payload_ports = values["PORT_MAPPINGS"].split("|")
        if values.get("LB_CLUSTER_ID") is not None:
            port_no = payload_ports[2].split(":")[1]
        else:
            port_no = payload_ports[1].split(":")[1]

        callback_url = "https://%s:%s/travelocity.com/home.jsp" % (cluster_hostname, port_no)
        saml_callback_entry = {"key": "CALLBACK_URL", "values": callback_url}
        issuer_entry = {"key": "SSO_ISSUER", "values": "travelocity.com"}
        # properties_data = [hostname_entry, saml_callback_entry]
        properties_data = [saml_callback_entry, issuer_entry]
        publish_data.properties = properties_data

        mdsclient.put(publish_data, app=True)









