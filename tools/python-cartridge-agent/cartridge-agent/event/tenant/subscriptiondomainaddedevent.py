import json


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