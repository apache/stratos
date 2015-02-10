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


class SubscriptionDomainAddedEvent():

    def __init__(self):
        self.tenant_id = None
        """ :type : int  """
        self.service_name = None
        """ :type : str  """
        self.cluster_ids = None
        """ :type : list[str]  """
        self.domain_name = None
        """ :type : str  """
        self.application_context = None
        """ :type : str  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = SubscriptionDomainAddedEvent()

        instance.cluster_ids = json_obj["clusterIds"] if "clusterIds" in json_obj else None
        instance.tenant_id = json_obj["tenantId"] if "tenantId" in json_obj else None
        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.domain_name = json_obj["domainName"] if "domainName" in json_obj else None
        instance.application_context = json_obj["applicationContext"] if "applicationContext" in json_obj else None

        return instance


class SubscriptionDomainRemovedEvent:

    def __init__(self, tenant_id, service_name, cluster_ids, domain_name):
        self.tenant_id = tenant_id
        """ :type : int  """
        self.service_name = service_name
        """ :type : str  """
        self.cluster_ids = cluster_ids
        """ :type : list[str]  """
        self.domain_name = domain_name
        """ :type : str  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = SubscriptionDomainRemovedEvent()

        instance.cluster_ids = json_obj["clusterIds"] if "clusterIds" in json_obj else None
        instance.tenant_id = json_obj["tenantId"] if "tenantId" in json_obj else None
        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.domain_name = json_obj["domainName"] if "domainName" in json_obj else None

        return instance


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
                for service_name in tenant_str["serviceNameSubscriptionMap"]:
                    sub_str = tenant_str["serviceNameSubscriptionMap"][service_name]
                    sub = Subscription(sub_str["serviceName"], sub_str["clusterIds"])
                    for domain_name in sub_str["subscriptionDomainMap"]:
                        subdomain_str = sub_str["subscriptionDomainMap"][domain_name]
                        sub.add_subscription_domain(domain_name, subdomain_str["applicationContext"])
                    tenant_obj.add_subscription(sub)
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