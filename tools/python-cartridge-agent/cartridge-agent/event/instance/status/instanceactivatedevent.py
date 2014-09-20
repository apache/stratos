import json

class InstanceActivatedEvent:
    def __init__(self, service_name, cluster_id, network_partition_id, parition_id, member_id):
        self.serviceName = service_name
        self.clusterId = cluster_id
        self.networkPartitionId = network_partition_id
        self.partitionId = parition_id
        self.memberId = member_id

    def to_Json(self):
        return json.dumps(self, default=lambda o: o.__dict__, sort_keys=True, indent=4)