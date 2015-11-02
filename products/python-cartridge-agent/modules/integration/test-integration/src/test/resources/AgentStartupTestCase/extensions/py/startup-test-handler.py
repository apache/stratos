# ------------------------------------------------------------------------
#
# Copyright 2005-2015 WSO2, Inc. (http://wso2.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License
#
# ------------------------------------------------------------------------

from plugins.contracts import ICartridgeAgentPlugin
from modules.util.log import LogFactory
from entity import *
import time
from threading import Thread


class StartupTestHandler(ICartridgeAgentPlugin):
    log = LogFactory().get_log(__name__)

    def run_plugin(self, values):
        StartupTestHandler.log.info("Topology: %r" % TopologyContext.topology.json_str)
        thread = Thread(target=self.threaded_function)
        thread.start()

    def threaded_function(self):
        memberFound = False
        service_name = "php"
        cluster_id = "php.php.domain"
        member_id = "new-member"

        while (not memberFound):
            StartupTestHandler.log.info("Checking topology for new member...")
            StartupTestHandler.log.info("Topology: %r" % TopologyContext.topology.json_str)
            service = TopologyContext.topology.get_service(service_name)
            if service is None:
                StartupTestHandler.log.error("Service not found in topology [service] %r" % service_name)
                return False

            cluster = service.get_cluster(cluster_id)
            if cluster is None:
                StartupTestHandler.log.error("Cluster id not found in topology [cluster] %r" % cluster_id)
                return False
            StartupTestHandler.log.info("Member found in cluster: %r" % cluster.member_exists(member_id))

            new_member = cluster.get_member(member_id)
            if (new_member is not None):
                StartupTestHandler.log.info("new-member was found in topology: %r" % new_member.to_json())
                memberFound = True
            time.sleep(5)

        StartupTestHandler.log.info("Topology context update test passed!")
