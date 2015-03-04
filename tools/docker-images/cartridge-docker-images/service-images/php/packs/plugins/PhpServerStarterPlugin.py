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


class PhpServerStarterPlugin(ICartridgeAgentPlugin):

    def run_plugin(self, values):
        log = LogFactory().get_log(__name__)
        # php_start_command = "/usr/sbin/apache2ctl -D FOREGROUND"
        php_start_command = "/etc/init.d/apache2 restart"
        p = subprocess.Popen(php_start_command, shell=True)
        output, errors = p.communicate()
        log.debug("Apache server started: [command] %s, [output] %s" % (php_start_command, output))

