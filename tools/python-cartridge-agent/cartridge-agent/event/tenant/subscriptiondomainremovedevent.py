import json


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