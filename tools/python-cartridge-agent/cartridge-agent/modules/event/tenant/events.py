import json
from ... tenant.tenantcontext import *


class SubscriptionDomainAddedEvent():

    def __init__(self):
        self.tenant_id = None
        self.service_name = None
        self.cluster_ids = None
        self.domain_name = None
        self.application_context = None

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
        self.service_name = service_name
        self.cluster_ids = cluster_ids
        self.domain_name = domain_name

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

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = CompleteTenantEvent()
        instance.tenants = []

        temp_tenants = json_obj["tenants"] if "tenants" in json_obj else None
        if temp_tenants is not None:
            for tenant_str in temp_tenants:
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
        self.service_name = None
        self.cluster_ids = None

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
        self.service_name = None
        self.cluster_ids = None

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = TenantUnsubscribedEvent()

        instance.tenant_id = json_obj["tenantId"] if "tenantId" in json_obj else None
        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_ids = json_obj["clusterIds"] if "clusterIds" in json_obj else None

        return instance