import json


class InstanceActivatedEvent:
    def __init__(self, service_name, cluster_id, network_partition_id, parition_id, member_id):
        self.serviceName = service_name
        self.clusterId = cluster_id
        self.networkPartitionId = network_partition_id
        self.partitionId = parition_id
        self.memberId = member_id

    def to_json(self):
        return to_json(self)


class InstanceStartedEvent:
    def __init__(self, service_name, cluster_id, network_partition_id, parition_id, member_id):
        self.serviceName = service_name
        self.clusterId = cluster_id
        self.networkPartitionId = network_partition_id
        self.partitionId = parition_id
        self.memberId = member_id

    def to_json(self):
        return to_json(self)


class InstanceMaintenanceModeEvent:

    def __init__(self, service_name, cluster_id, network_partition_id, partition_id, member_id):
        self.serviceName = service_name
        self.clusterId = cluster_id
        self.networkPartitionId = network_partition_id
        self.partitionId = partition_id
        self.memberId = member_id

    def to_json(self):
        return to_json(self)


class InstanceReadyToShutdownEvent:

    def __init__(self, service_name, cluster_id, network_partition_id, partition_id, member_id):
        self.serviceName = service_name
        self.clusterId = cluster_id
        self.networkPartitionId = network_partition_id
        self.partitionId = partition_id
        self.memberId = member_id

    def to_json(self):
        return to_json(self)


def to_json(instance):
    """
    common function to serialize status event object
    :param obj instance:
    :return: serialized json string
    :rtype str
    """
    return json.dumps(instance, default=lambda o: o.__dict__, sort_keys=True, indent=4)