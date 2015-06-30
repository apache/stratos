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
from xml.dom.minidom import parse
import socket
from modules.util.log import LogFactory
import time
import subprocess
import os


class WSO2ISMetaDataHandler(ICartridgeAgentPlugin):

    def run_plugin(self, values):
        log = LogFactory().get_log(__name__)
        log.info("Starting wso2is metadata handler...")

        # read tomcat app related values from metadata
        mds_response = None
        while mds_response is None:
            log.debug("Waiting for SSO_ISSUER and CALLBACK_URL to be available from metadata service for app ID: %s"
                      % values["APPLICATION_ID"])
            time.sleep(5)
            mds_response = mdsclient.get(app=True)
            if mds_response is not None:
                if mds_response.properties.get("SSO_ISSUER") is None or \
                        mds_response.properties.get("CALLBACK_URL") is None:
                    mds_response = None
        # mds_response = mdsclient.get()
        issuer = mds_response.properties["SSO_ISSUER"]
        acs = mds_response.properties["CALLBACK_URL"]

        # add a service provider in the security/sso-idp-config.xml file
        # is_root = values["APPLICATION_PATH"]
        is_root = os.environ.get("CARBON_HOME")
        sso_idp_file = "%s/repository/conf/security/sso-idp-config.xml" % is_root

        # <SSOIdentityProviderConfig>
        #     <ServiceProviders>
        #         <ServiceProvider>
        #         <Issuer>wso2.my.dashboard</Issuer>
        #         <AssertionConsumerService>https://is.wso2.com/dashboard/acs</AssertionConsumerService>
        #         <SignAssertion>true</SignAssertion>
        #         <SignResponse>true</SignResponse>
        #         <EnableAttributeProfile>false</EnableAttributeProfile>
        #         <IncludeAttributeByDefault>false</IncludeAttributeByDefault>
        #         <Claims>
        #             <Claim>http://wso2.org/claims/role</Claim>
        #         </Claims>
        #         <EnableSingleLogout>false</EnableSingleLogout>
        #         <SingleLogoutUrl></SingleLogoutUrl>
        #         <EnableAudienceRestriction>true</EnableAudienceRestriction>
        #         <AudiencesList>
        #             <Audience>carbonServer</Audience>
        #         </AudiencesList>
        #         <ConsumingServiceIndex></ConsumingServiceIndex>
        #     </ServiceProvider>
        with open(sso_idp_file, "r") as f:
            sp_dom = parse(f)

        root_element = sp_dom.documentElement
        sps_element = sp_dom.getElementsByTagName("ServiceProviders")[0]

        sp_entry = sp_dom.createElement("ServiceProvider")

        sp_entry_issuer = sp_dom.createElement("Issuer")
        sp_entry_issuer.appendChild(sp_dom.createTextNode(issuer))

        sp_entry_acs = sp_dom.createElement("AssertionConsumerService")
        sp_entry_acs.appendChild(sp_dom.createTextNode(acs))

        sp_entry_sign_resp = sp_dom.createElement("SignResponse")
        sp_entry_sign_resp.appendChild(sp_dom.createTextNode("true"))

        sp_entry_sign_assert = sp_dom.createElement("SignAssertion")
        sp_entry_sign_assert.appendChild(sp_dom.createTextNode("true"))

        sp_entry_single_logout = sp_dom.createElement("EnableSingleLogout")
        sp_entry_single_logout.appendChild(sp_dom.createTextNode("true"))

        sp_entry_attribute_profile = sp_dom.createElement("EnableAttributeProfile")
        sp_entry_attribute_profile.appendChild(sp_dom.createTextNode("true"))

        sp_entry.appendChild(sp_entry_issuer)
        sp_entry.appendChild(sp_entry_acs)
        sp_entry.appendChild(sp_entry_sign_resp)
        sp_entry.appendChild(sp_entry_sign_assert)
        sp_entry.appendChild(sp_entry_single_logout)
        sp_entry.appendChild(sp_entry_attribute_profile)

        sps_element.appendChild(sp_entry)

        with open(sso_idp_file, 'w+') as f:
            root_element.writexml(f, newl="\n")
        # root_element.writexml(f)

        # data = json.loads(urllib.urlopen("http://ip.jsontest.com/").read())
        # ip_entry = data["ip"]

        # publish SAML_ENDPOINT to metadata service
        # member_hostname = socket.gethostname()
        member_hostname = values["HOST_NAME"]

        # read kubernetes service https port
        log.info("Reading port mappings...")
        port_mappings_str = values["PORT_MAPPINGS"]
        https_port = None

        # port mappings format: """NAME:mgt-console|PROTOCOL:https|PORT:4500|PROXY_PORT:8443;
        #                          NAME:tomcat-http|PROTOCOL:http|PORT:4501|PROXY_PORT:7280;"""

        log.info("Port mappings: %s" % port_mappings_str)
        if port_mappings_str is not None:

            port_mappings_array = port_mappings_str.split(";")
            if port_mappings_array:

                for port_mapping in port_mappings_array:
                    log.debug("port_mapping: %s" % port_mapping)
                    name_value_array = port_mapping.split("|")
                    protocol = name_value_array[1].split(":")[1]
                    port = name_value_array[2].split(":")[1]
                    if protocol == "https":
                        https_port = port

        log.info("Kubernetes service port of wso2is management console https transport: %s" % https_port)

        saml_endpoint = "https://%s:%s/samlsso" % (member_hostname, https_port)
        saml_endpoint_property = {"key": "SAML_ENDPOINT", "values": [ saml_endpoint ]}
        mdsclient.put(saml_endpoint_property, app=True)
        log.info("Published property to metadata API: SAML_ENDPOINT: %s" % saml_endpoint)

        # start servers
        log.info("Starting WSO2 IS server")

        # set configurations
        carbon_replace_command = "sed -i \"s/CLUSTER_HOST_NAME/%s/g\" %s" % (member_hostname, "${CARBON_HOME}/repository/conf/carbon.xml")

        p = subprocess.Popen(carbon_replace_command, shell=True)
        output, errors = p.communicate()
        log.debug("Set carbon.xml hostname")

        catalina_replace_command = "sed -i \"s/STRATOS_IS_PROXY_PORT/%s/g\" %s" % (https_port, "${CARBON_HOME}/repository/conf/tomcat/catalina-server.xml")

        p = subprocess.Popen(catalina_replace_command, shell=True)
        output, errors = p.communicate()
        log.debug("Set catalina-server.xml proxy port")

        wso2is_start_command = "exec ${CARBON_HOME}/bin/wso2server.sh start"
        env_var = os.environ.copy()
        p = subprocess.Popen(wso2is_start_command, env=env_var, shell=True)
        output, errors = p.communicate()
        log.debug("WSO2 IS server started")

        log.info("wso2is metadata handler completed")
