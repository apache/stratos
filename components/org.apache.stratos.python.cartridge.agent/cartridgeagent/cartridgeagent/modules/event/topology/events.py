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

from ... topology.topologycontext import *


class MemberActivatedEvent:

    def __init__(self):
        self.service_name = None
        """ :type : str  """
        self.cluster_id = None
        """ :type : str  """
        self.clusterInstanceId = None
        """ :type : str  """
        self.member_id = None
        """ :type : str  """
        self.instance_id = None
        """ :type : str  """
        self.network_partition_id = None
        """ :type : str  """
        self.partition_id = None
        """ :type : str  """
        self.port_map = {}
        """ :type : dict[str, Port]  """
        self.member_private_ips = None
        """ :type : str  """

    def get_port(self, proxy_port):
        """
        Returns the port object of the provided port id
        :param str proxy_port:
        :return: Port object, None if the port id is invalid
        :rtype: topology.topologycontext.Port
        """
        if proxy_port in self.port_map:
            return self.port_map[proxy_port]

        return None

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberActivatedEvent()

        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.cluster_instance_id = json_obj["clusterInstanceId"] if "clusterInstanceId" in json_obj else None
        instance.member_id = json_obj["memberId"] if "memberId" in json_obj else None
        instance.instance_id = json_obj["instanceId"] if "instanceId" in json_obj else None
        instance.network_partition_id = json_obj["networkPartitionId"] if "networkPartitionId" in json_obj else None
        instance.partition_id = json_obj["partitionId"] if "partitionId" in json_obj else None
        #instance.port_map = json_obj["portMap"] if "portMap" in json_obj else {}
        instance.member_private_ips = json_obj["memberPrivateIPs"] if "memberPrivateIPs" in json_obj else None
        instance.member_public_ips = json_obj["memberPublicIPs"] if "memberPublicIPs" in json_obj else None
        instance.member_default_public_ip = json_obj["defaultPublicIP"] if "defaultPublicIP" in json_obj else None
        instance.member_default_private_ip = json_obj["defaultPrivateIP"] if "defaultPrivateIP" in json_obj else None

        for port_proxy in json_obj["portMap"]:
            port_str = json_obj["portMap"][port_proxy]
            port_obj = Port(port_str["protocol"], port_str["value"], port_proxy)
            instance.port_map[port_proxy] = port_obj

        return instance


class MemberTerminatedEvent:

    def __init__(self):
        self.service_name = None
        """ :type : str  """
        self.cluster_id = None
        """ :type : str  """
        self.clusterInstanceId = None
        """ :type : str  """
        self.member_id = None
        """ :type : str  """
        self.instance_id = None
        """ :type : str  """
        self.network_partition_id = None
        """ :type : str  """
        self.partition_id = None
        """ :type : str  """
        self.properties = {}
        """ :type : dict[str, str]  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberTerminatedEvent()

        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.cluster_instance_id = json_obj["clusterInstanceId"] if "clusterInstanceId" in json_obj else None
        instance.member_id = json_obj["memberId"] if "memberId" in json_obj else None
        instance.instance_id = json_obj["instanceId"] if "instanceId" in json_obj else None
        instance.network_partition_id = json_obj["networkPartitionId"] if "networkPartitionId" in json_obj else None
        instance.partition_id = json_obj["partitionId"] if "partitionId" in json_obj else None
        instance.properties = json_obj["properties"] if "properties" in json_obj else None

        return instance


class MemberSuspendedEvent:

    def __init__(self):
        self.service_name = None
        """ :type : str  """
        self.cluster_id = None
        """ :type : str  """
        self.clusterInstanceId = None
        """ :type : str  """
        self.member_id = None
        """ :type : str  """
        self.instance_id = None
        """ :type : str  """
        self.network_partition_id = None
        """ :type : str  """
        self.partition_id = None
        """ :type : str  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberSuspendedEvent()

        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.cluster_instance_id = json_obj["clusterInstanceId"] if "clusterInstanceId" in json_obj else None
        instance.member_id = json_obj["memberId"] if "memberId" in json_obj else None
        instance.instance_id = json_obj["instanceId"] if "instanceId" in json_obj else None
        instance.network_partition_id = json_obj["networkPartitionId"] if "networkPartitionId" in json_obj else None
        instance.partition_id = json_obj["partitionId"] if "partitionId" in json_obj else None

        return instance


class CompleteTopologyEvent:

    def __init__(self):
        self.topology = None
        """ :type :  Topology """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = CompleteTopologyEvent()

        topology_str = json_obj["topology"] if "topology" in json_obj else None
        if topology_str is not None:
            topology_obj = Topology()
            topology_obj.initialized = True if str(topology_str["initialized"]).lower == "true" else False
            topology_obj.json_str = topology_str

            #add service map
            for service_name in topology_str["serviceMap"]:
                service_str = topology_str["serviceMap"][service_name]

                service_obj = Service(service_name, service_str["serviceType"])
                service_obj.properties = service_str["properties"] if "properties" in service_str else None
                # add ports to port map
                for port_proxy in service_str["portMap"]:
                    port_str = service_str["portMap"][port_proxy]
                    port_obj = Port(port_str["protocol"], port_str["value"], port_proxy)
                    service_obj.add_port(port_obj)

                #add cluster map
                for cluster_id in service_str["clusterIdClusterMap"]:
                    cluster_str = service_str["clusterIdClusterMap"][cluster_id]
                    cl_service_name = cluster_str["serviceName"]
                    cl_autoscale_policy_name = cluster_str["autoscalePolicyName"]
                    cl_deployment_policy_name = cluster_str["deploymentPolicyName"] \
                        if "deploymentPolicyName" in cluster_str else None

                    cluster_obj = Cluster(cl_service_name, cluster_id,
                                          cl_deployment_policy_name, cl_autoscale_policy_name)
                    cluster_obj.hostnames = cluster_str["hostNames"]
                    cluster_obj.tenant_range = cluster_str["tenantRange"] if "tenantRange" in cluster_str else None
                    cluster_obj.is_lb_cluster = True if str(cluster_str["isLbCluster"]).lower == "true" else False
                    cluster_obj.is_kubernetes_cluster = True if str(cluster_str["isKubernetesCluster"]).lower == "true" \
                        else False
                    # cluster_obj.status = cluster_str["status"] if "status" in cluster_str else None
                    cluster_obj.load_balancer_algorithm_name = cluster_str["loadBalanceAlgorithmName"] \
                        if "loadBalanceAlgorithmName" in cluster_str else None
                    cluster_obj.properties = cluster_str["properties"] if "properties" in cluster_str else None
                    cluster_obj.member_list_json = cluster_str["memberMap"]
                    cluster_obj.app_id = cluster_str["appId"]

                    # add member map
                    for member_id in cluster_str["memberMap"]:
                        member_str = cluster_str["memberMap"][member_id]
                        mm_service_name = member_str["serviceName"]
                        mm_cluster_id = member_str["clusterId"]
                        mm_network_partition_id = member_str["networkPartitionId"] if "networkPartitionId" in member_str \
                            else None
                        mm_partition_id = member_str["partitionId"] if "partitionId" in member_str else None
                        mm_cluster_instance_id = member_str["clusterInstanceId"] if "clusterInstanceId" in member_str \
                            else None

                        member_obj = Member(mm_service_name, mm_cluster_id, mm_network_partition_id,
                                            mm_partition_id, member_id, mm_cluster_instance_id)
                        member_obj.member_public_ips = member_str["memberPublicIPs"] if "memberPublicIPs" in member_str \
                            else None
                        member_obj.member_default_public_ip = member_str["defaultPublicIP"] \
                            if "defaultPublicIP" in member_str else None
                        member_obj.status = member_str["memberStateManager"]["stateStack"][-1]
                        member_obj.member_private_ips = member_str["memberPrivateIPs"] if "memberPrivateIPs" in member_str \
                            else None
                        member_obj.member_default_private_ip = member_str["defaultPrivateIP"] \
                            if "defaultPrivateIP" in member_str else None
                        member_obj.properties = member_str["properties"] if "properties" in member_str else None
                        member_obj.lb_cluster_id = member_str["lbClusterId"] if "lbClusterId" in member_str else None
                        member_obj.init_time = member_str["initTime"] if "initTime" in member_str else None
                        member_obj.json_str = member_str

                        # add port map
                        for mm_port_proxy in member_str["portMap"]:
                            mm_port_str = member_str["portMap"][mm_port_proxy]
                            mm_port_obj = Port(mm_port_str["protocol"], mm_port_str["value"], mm_port_proxy)
                            member_obj.add_port(mm_port_obj)
                        cluster_obj.add_member(member_obj)
                    service_obj.add_cluster(cluster_obj)
                topology_obj.add_service(service_obj)
            instance.topology = topology_obj

        return instance

    def get_topology(self):
        return self.topology


class MemberStartedEvent:

    def __init__(self):
        self.service_name = None
        """ :type : str  """
        self.cluster_id = None
        """ :type : str  """
        self.clusterInstanceId = None
        """ :type : str  """
        self.member_id = None
        """ :type : str  """
        self.instance_id = None
        """ :type : str  """
        self.network_partition_id = None
        """ :type : str  """
        self.partition_id = None
        """ :type : str  """
        self.status = None
        """ :type : str  """
        self.properties = {}
        """ :type : dict[str, str]  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberStartedEvent()

        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.cluster_instance_id = json_obj["clusterInstanceId"] if "clusterInstanceId" in json_obj else None
        instance.member_id = json_obj["memberId"] if "memberId" in json_obj else None
        instance.instance_id = json_obj["instanceId"] if "instanceId" in json_obj else None
        instance.network_partition_id = json_obj["networkPartitionId"] if "networkPartitionId" in json_obj else None
        instance.partition_id = json_obj["partitionId"] if "partitionId" in json_obj else None
        instance.properties = json_obj["properties"] if "properties" in json_obj else None

        return instance


class MemberCreatedEvent:

    def __init__(self):
        self.service_name = None
        """ :type : str  """
        self.cluster_id = None
        """ :type : str  """
        self.clusterInstanceId = None
        """ :type : str  """
        self.member_id = None
        """ :type : str  """
        self.network_partition_id = None
        """ :type : str  """
        self.partition_id = None
        """ :type : str  """
        self.lb_cluster_id = None
        """ :type : str  """
        self.member_public_ips = None
        """ :type : str  """
        self.member_private_ips = None
        """ :type : str  """
        self.properties = {}
        """ :type : dict[str, str]  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberCreatedEvent()

        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.cluster_instance_id = json_obj["clusterInstanceId"] if "clusterInstanceId" in json_obj else None
        instance.member_id = json_obj["memberId"] if "memberId" in json_obj else None
        instance.network_partition_id = json_obj["networkPartitionId"] if "networkPartitionId" in json_obj else None
        instance.partition_id = json_obj["partitionId"] if "partitionId" in json_obj else None
        instance.lb_cluster_id = json_obj["lbClusterId"] if "lbClusterId" in json_obj else None
        instance.member_private_ips = json_obj["memberPrivateIPs"] if "memberPrivateIPs" in json_obj else None
        instance.member_public_ips = json_obj["memberPublicIPs"] if "memberPublicIPs" in json_obj else None
        instance.member_default_public_ip = json_obj["defaultPublicIP"] if "defaultPublicIP" in json_obj else None
        instance.member_default_private_ip = json_obj["defaultPrivateIP"] if "defaultPrivateIP" in json_obj else None
        instance.properties = json_obj["properties"]

        return instance


class MemberInitializedEvent:

    def __init__(self):
        self.service_name = None
        """ :type : str  """
        self.cluster_id = None
        """ :type : str  """
        self.clusterInstanceId = None
        """ :type : str  """
        self.member_id = None
        """ :type : str  """
        self.network_partition_id = None
        """ :type : str  """
        self.partition_id = None
        """ :type : str  """
        self.lb_cluster_id = None
        """ :type : str  """
        self.member_public_ips = None
        """ :type : str  """
        self.member_private_ips = None
        """ :type : str  """
        self.properties = {}
        """ :type : dict[str, str]  """

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberCreatedEvent()

        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.cluster_instance_id = json_obj["clusterInstanceId"] if "clusterInstanceId" in json_obj else None
        instance.member_id = json_obj["memberId"] if "memberId" in json_obj else None
        instance.network_partition_id = json_obj["networkPartitionId"] if "networkPartitionId" in json_obj else None
        instance.partition_id = json_obj["partitionId"] if "partitionId" in json_obj else None
        instance.lb_cluster_id = json_obj["lbClusterId"] if "lbClusterId" in json_obj else None
        instance.member_private_ips = json_obj["memberPrivateIPs"] if "memberPrivateIPs" in json_obj else None
        instance.member_public_ips = json_obj["memberPublicIPs"] if "memberPublicIPs" in json_obj else None
        instance.member_default_public_ip = json_obj["defaultPublicIP"] if "defaultPublicIP" in json_obj else None
        instance.member_default_private_ip = json_obj["defaultPrivateIP"] if "defaultPrivateIP" in json_obj else None
        instance.properties = json_obj["properties"]

        return instance