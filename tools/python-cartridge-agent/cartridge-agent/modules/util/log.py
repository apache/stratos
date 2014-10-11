# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import logging
import logging.config
import os


class LogFactory(object):
    """
    Singleton implementation for handling logging in CartridgeAgent
    """
    class __LogFactory:
        def __init__(self):
            self.logs = {}
            logging_conf = os.path.abspath(os.path.dirname(__file__)).split("modules")[0] + "logging.ini"
            logging.config.fileConfig(logging_conf)

        def get_log(self, name):
            if name not in self.logs:
                self.logs[name] = logging.getLogger(name)

            return self.logs[name]

    instance = None

    def __new__(cls, *args, **kwargs):
        if not LogFactory.instance:
            LogFactory.instance = LogFactory.__LogFactory()

        return LogFactory.instance

    def get_log(self, name):
        """
        Returns a logger class with the specified channel name. Creates a new logger if one doesn't exists for the
        specified channel
        :param str name: Channel name
        :return: The logger class
        :rtype: RootLogger
        """
        return self.instance.get_log(name)