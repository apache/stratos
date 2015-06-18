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

import json
from ... tenant.tenantcontext import *


class CompleteTenantEvent:

    def __init__(self):
        self.tenants = []
        """ :type : list[Tenant]  """
        self.tenant_list_json = None
        """ :type : str  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = CompleteTenantEvent()
        instance.tenants = []

        tenants_str = json_obj["tenants"] if "tenants" in json_obj else None
        instance.tenant_list_json = tenants_str
        if tenants_str is not None:
            for tenant_str in tenants_str:
                tenant_obj = Tenant(int(tenant_str["tenantId"]), tenant_str["tenantDomain"])
                instance.tenants.append(tenant_obj)

        return instance


class TenantSubscribedEvent:

    def __init__(self):
        self.tenant_id = None
        """ :type : int  """
        self.service_name = None
        """ :type : str  """
        self.cluster_ids = None
        """ :type : list[str]  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = TenantSubscribedEvent()

        instance.tenant_id = json_obj["tenantId"] if "tenantId" in json_obj else None
        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_ids = json_obj["clusterIds"] if "clusterIds" in json_obj else None

        return instance


class TenantUnsubscribedEvent:

    def __init__(self):
        self.tenant_id = None
        """ :type : int  """
        self.service_name = None
        """ :type : str  """
        self.cluster_ids = None
        """ :type : list[str]  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = TenantUnsubscribedEvent()

        instance.tenant_id = json_obj["tenantId"] if "tenantId" in json_obj else None
        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_ids = json_obj["clusterIds"] if "clusterIds" in json_obj else None

        return instance