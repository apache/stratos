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

class ParameterNotFoundException(Exception):
    """
    Exception raised when a property is not present in the configuration or the payload
    of the cartridge agent
    """
    __message = None

    def __init__(self, message):
        Exception.__init__(self, message)
        self.__message = message

    def get_message(self):
        """
        The message provided when the exception is raised
        :return: message
        :rtype: str
        """
        return self.__message
