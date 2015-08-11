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

from yapsy.IPlugin import IPlugin


class ICartridgeAgentPlugin(IPlugin):
    """
    To implement a Cartridge Agent plugin to be executed on a MB event
    """

    def run_plugin(self, values):
        raise NotImplementedError


class IArtifactManagementPlugin(IPlugin):
    """
    To implement an artifact management plugin to manage artifact distribution using a custom version control tool
    """

    def checkout(self):
        raise NotImplementedError

    def push(self):
        raise NotImplementedError


class IHealthStatReaderPlugin(IPlugin):
    """
    To implement a health statistics reader plugin to read health statistics using a custom factor
    """

    def stat_cartridge_health(self, health_stat):
        raise NotImplementedError