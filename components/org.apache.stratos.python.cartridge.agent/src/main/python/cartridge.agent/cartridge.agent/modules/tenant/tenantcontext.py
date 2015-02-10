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

class Tenant:
    """
    Object type representing the tenant details of a single tenant
    """

    def __init__(self, tenant_id,  tenant_domain):
        self.tenant_id = tenant_id
        """ :type : int """
        self.tenant_domain = tenant_domain
        """ :type : str """
        self.service_name_subscription_map = {}
        """ :type : dict[str, Subscription] """

    def get_subscription(self, service_name):
        """
        Returns the Subscription object related to the provided service name
        :param str service_name: service name to be retrieved
        :return: Subscription of the service or None if the service name doesn't exist
        :rtype: Subscription
        """
        if service_name in self.service_name_subscription_map:
            return self.service_name_subscription_map[service_name]

        return None

    def is_subscribed(self, service_name):
        """
        Checks if the given service name has a subscription from this tenant
        :param str service_name: name of the service to check
        :return: True if the tenant is subscribed to the given service name, False if not
        :rtype: bool
        """
        return service_name in self.service_name_subscription_map

    def add_subscription(self, subscription):
        """
        Adds a subscription information entry on the subscription list for this tenant
        :param Subscription subscription: Subscription information to be added
        :return: void
        :rtype: void
        """
        self.service_name_subscription_map[subscription.service_name] = subscription

    def remove_subscription(self, service_name):
        """
        Removes the specified subscription details from the subscription list
        :param str service_name: The service name of the subscription to be removed
        :return: void
        :rtype: void
        """
        if service_name in self.service_name_subscription_map:
            self.service_name_subscription_map.pop(service_name)


class Subscription:
    """
    Subscription information of a particular subscription to a service
    """

    def __init__(self, service_name, cluster_ids):
        self.service_name = service_name
        """ :type : str """
        self.cluster_ids = cluster_ids
        """ :type : list[str]  """
        self.subscription_domain_map = {}
        """ :type : dict[str, SubscriptionDomain]  """

    def add_subscription_domain(self, domain_name, application_context):
        """
        Adds a subscription domain
        :param str domain_name:
        :param str application_context:
        :return: void
        :rtype: void
        """
        self.subscription_domain_map[domain_name] = SubscriptionDomain(domain_name, application_context)

    def remove_subscription_domain(self, domain_name):
        """
        Removes the subscription domain of the specified domain name
        :param str domain_name:
        :return: void
        :rtype: void
        """
        if domain_name in self.subscription_domain_map:
            self.subscription_domain_map.pop(domain_name)

    def subscription_domain_exists(self, domain_name):
        """
        Returns the SubscriptionDomain information of the specified domain name
        :param str domain_name:
        :return: SubscriptionDomain
        :rtype: SubscriptionDomain
        """
        return domain_name in self.subscription_domain_map

    def get_subscription_domains(self):
        """
        Returns the list of subscription domains of this subscription
        :return: List of SubscriptionDomain objects
        :rtype: list[SubscriptionDomain]
        """
        return self.subscription_domain_map.values()


class SubscriptionDomain:
    """
    Represents a Subscription Domain
    """

    def __init__(self, domain_name, application_context):
        self.domain_name = domain_name
        """ :type : str  """
        self.application_context = application_context
        """ :type : str  """


class TenantContext:
    """
    Handles and maintains a model of all the information related to tenants within this instance
    """
    tenants = {}
    initialized = False
    tenant_domains = {"carbon.super": Tenant(-1234, "carbon.super")}

    @staticmethod
    def add_tenant(tenant):
        TenantContext.tenants[tenant.tenant_id] = tenant
        TenantContext.tenant_domains[tenant.tenant_domain] = tenant

    @staticmethod
    def remove_tenant(tenant_id):
        if tenant_id in TenantContext.tenants:
            tenant = TenantContext.get_tenant(tenant_id)
            TenantContext.tenants.pop(tenant.tenant_id)
            TenantContext.tenant_domains.pop(tenant.tenant_domain)

    @staticmethod
    def update(tenants):
        for tenant in tenants:
            TenantContext.add_tenant(tenant)

    @staticmethod
    def get_tenant(tenant_id):
        """
        Gets the Tenant object of the provided tenant ID
        :param int tenant_id:
        :return: Tenant object of the provided tenant ID
        :rtype: Tenant
        """
        if tenant_id in TenantContext.tenants:
            return TenantContext.tenants[tenant_id]

        return None

    @staticmethod
    def get_tenant_by_domain(tenant_domain):
        """
        Gets the Tenant object of the provided tenant domain
        :param str tenant_domain:
        :return: Tenant object of the provided tenant domain
        :rtype: str
        """
        if tenant_domain in TenantContext.tenant_domains:
            return TenantContext.tenant_domains[tenant_domain]

        return None