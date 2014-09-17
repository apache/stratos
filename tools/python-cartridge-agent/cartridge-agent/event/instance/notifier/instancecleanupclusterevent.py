import json


class InstanceCleanupClusterEvent:
    def __init__(self, cluster_id):
        self.cluster_id = cluster_id

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        c_id = json_obj["clusterId"] if "clusterId" in json_obj else None

        return InstanceCleanupClusterEvent(c_id)