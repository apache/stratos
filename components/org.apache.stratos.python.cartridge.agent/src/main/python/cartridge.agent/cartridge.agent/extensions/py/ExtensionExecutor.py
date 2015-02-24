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

from plugins.contracts import ICartridgeAgentPlugin
import util


class ExtensionExecutor(ICartridgeAgentPlugin):

    def run_plugin(self, values, log):
        event_name = values["EVENT"]
        log.debug("Running extension for %s" % event_name)
        extension_values = {}
        for key in values.keys():
            extension_values["STRATOS_" + key] = values[key]
            log.debug("%s => %s" % ("STRATOS_" + key, extension_values["STRATOS_" + key]))

        # script_name = "mount-volumes.sh"
        output, errors = util.execute_bash(event_name)
        if len(errors) > 0:
            raise RuntimeError("Extension execution failed for script %s: %s" % (event_name, errors))

        log.info("%s Extension executed. [output]: %s" % (event_name, output))
