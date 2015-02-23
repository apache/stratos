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


class DomainMappingAddedEvent:

    def __init__(self):
        self.application_id = None
        """ :type : str """
        self.tenant_id = None
        """ :type : int  """
        self.service_name = None
        """ :type : str  """
        self.cluster_id = None
        """ :type : str  """
        self.domain_name = None
        """ :type : str  """
        self.context_path = None
        """ :type : str  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = DomainMappingAddedEvent()

        instance.tenant_id = json_obj["applicationId"] if "applicationId" in json_obj else None
        instance.tenant_id = json_obj["tenantId"] if "tenantId" in json_obj else None
        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_ids = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.cluster_ids = json_obj["domainName"] if "domainName" in json_obj else None
        instance.cluster_ids = json_obj["contextPath"] if "contextPath" in json_obj else None

        return instance


class DomainMappingRemovedEvent:

    def __init__(self):
        self.application_id = None
        """ :type : str """
        self.tenant_id = None
        """ :type : int  """
        self.service_name = None
        """ :type : str  """
        self.cluster_id = None
        """ :type : str  """
        self.domain_name = None
        """ :type : str  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = DomainMappingRemovedEvent()

        instance.tenant_id = json_obj["applicationId"] if "applicationId" in json_obj else None
        instance.tenant_id = json_obj["tenantId"] if "tenantId" in json_obj else None
        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_ids = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.cluster_ids = json_obj["domainName"] if "domainName" in json_obj else None

        return instance
