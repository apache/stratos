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

import json


class ApplicationSignUpAddedEvent:
    def __init__(self):
        self.applicationId = None
        """ :type : str  """
        self.tenantId = None
        """ :type : str  """
        self.clusterIds = None
        """ :type : list[str]  """


    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        applicationSignUp = ApplicationSignUpAddedEvent()

        applicationSignUp.applicationId = json_obj["applicationId"] if "applicationId" in json_obj else None
        applicationSignUp.tenantId = json_obj["tenantId"] if "tenantId" in json_obj else None
        applicationSignUp.clusterIds = json_obj["clusterIds"] if "clusterIds" in json_obj else None

        return applicationSignUp


class ApplicationSignUpRemovedEvent:
    def __init__(self):
        self.applicationId = None
        """ :type : str  """
        self.tenantId = None
        """ :type : str  """
        self.clusterIds = None
        """ :type : list[str]  """


    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        applicationSignUp = ApplicationSignUpRemovedEvent()

        applicationSignUp.applicationId = json_obj["applicationId"] if "applicationId" in json_obj else None
        applicationSignUp.tenantId = json_obj["tenantId"] if "tenantId" in json_obj else None

        return applicationSignUp

