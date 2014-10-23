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
        instance.repo_url = json_obj["repoURL"] if "repoURL" in json_obj else ""
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