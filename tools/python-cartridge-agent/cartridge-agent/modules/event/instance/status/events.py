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


class InstanceActivatedEvent:
    def __init__(self, service_name, cluster_id, network_partition_id, parition_id, member_id):
        self.serviceName = service_name
        """ :type : str  """
        self.clusterId = cluster_id
        """ :type : str  """
        self.networkPartitionId = network_partition_id
        """ :type : str  """
        self.partitionId = parition_id
        """ :type : str  """
        self.memberId = member_id
        """ :type : str  """

    def to_json(self):
        return to_json(self)


class InstanceStartedEvent:
    def __init__(self, service_name, cluster_id, network_partition_id, parition_id, member_id):
        self.serviceName = service_name
        """ :type : str  """
        self.clusterId = cluster_id
        """ :type : str  """
        self.networkPartitionId = network_partition_id
        """ :type : str  """
        self.partitionId = parition_id
        """ :type : str  """
        self.memberId = member_id
        """ :type : str  """

    def to_json(self):
        return to_json(self)


class InstanceMaintenanceModeEvent:

    def __init__(self, service_name, cluster_id, network_partition_id, partition_id, member_id):
        self.serviceName = service_name
        """ :type : str  """
        self.clusterId = cluster_id
        """ :type : str  """
        self.networkPartitionId = network_partition_id
        """ :type : str  """
        self.partitionId = partition_id
        """ :type : str  """
        self.memberId = member_id
        """ :type : str  """

    def to_json(self):
        return to_json(self)


class InstanceReadyToShutdownEvent:

    def __init__(self, service_name, cluster_id, network_partition_id, partition_id, member_id):
        self.serviceName = service_name
        """ :type : str  """
        self.clusterId = cluster_id
        """ :type : str  """
        self.networkPartitionId = network_partition_id
        """ :type : str  """
        self.partitionId = partition_id
        """ :type : str  """
        self.memberId = member_id
        """ :type : str  """

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