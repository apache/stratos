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
import os
import subprocess
from modules.util.log import LogFactory


class ExtensionExecutor(ICartridgeAgentPlugin):

    def run_plugin(self, values):
        log = LogFactory().get_log(__name__)
        event_name = values["EVENT"]
        log.debug("Running extension for %s" % event_name)
        extension_values = {}
        for key in values.keys():
            extension_values["STRATOS_" + key] = values[key]
            os.environ["STRATOS_" + key] = values[key]
            # log.debug("%s => %s" % ("STRATOS_" + key, extension_values["STRATOS_" + key]))

        try:
            output, errors = ExtensionExecutor.execute_script(event_name + ".sh", extension_values)
        except OSError:
            raise RuntimeError("Could not find an extension file for event %s" % event_name)

        if len(errors) > 0:
            raise RuntimeError("Extension execution failed for script %s: %s" % (event_name, errors))

        log.info("%s Extension executed. [output]: %s" % (event_name, output))

    @staticmethod
    def execute_script(bash_file, extension_values):
        """ Execute the given bash files in the <PCA_HOME>/extensions/bash folder
        :param bash_file: name of the bash file to execute
        :return: tuple of (output, errors)
        """
        log = LogFactory().get_log(__name__)

        working_dir = os.path.abspath(os.path.dirname(__file__)).split("modules")[0]
        command = working_dir[:-2] + "bash/" + bash_file
        current_env_vars = os.environ.copy()
        extension_values.update(current_env_vars)

        log.debug("Execute bash script :: %s" % command)
        p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=extension_values)
        output, errors = p.communicate()

        return output, errors