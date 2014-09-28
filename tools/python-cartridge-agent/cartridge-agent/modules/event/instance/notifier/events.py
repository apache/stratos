import json


class ArtifactUpdatedEvent:
    def __init__(self):
        self.cluster_id = None
        """ :type : str  """
        self.status = None
        """ :type : str  """
        self.repo_username = None
        """ :type : str  """
        self.repo_password = None
        """ :type : str  """
        self.repo_url = None
        """ :type : str  """
        self.tenant_id = None
        """ :type : int  """
        self.commit_enabled = None
        """ :type : bool  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = ArtifactUpdatedEvent()

        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.status = json_obj["status"] if "status" in json_obj else None
        instance.repo_username = json_obj["repoUserName"] if "repoUserName" in json_obj else None
        instance.repo_password = json_obj["repoPassword"] if "repoPassword" in json_obj else None
        instance.tenant_id = json_obj["tenantId"] if "tenantId" in json_obj else None
        instance.commit_enabled = json_obj["commitEnabled"] if "commitEnabled" in json_obj else None

        return instance


class InstanceCleanupClusterEvent:
    def __init__(self, cluster_id):
        self.cluster_id = cluster_id
        """ :type : str  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        c_id = json_obj["clusterId"] if "clusterId" in json_obj else None

        return InstanceCleanupClusterEvent(c_id)


class InstanceCleanupMemberEvent:
    def __init__(self, member_id):
        self.member_id = member_id
        """ :type : str  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        m_id = json_obj["memberId"] if "memberId" in json_obj else None

        return InstanceCleanupMemberEvent(m_id)