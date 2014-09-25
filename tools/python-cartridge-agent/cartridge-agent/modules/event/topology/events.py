import json

from ... topology.topologycontext import *


class MemberActivatedEvent:

    def __init__(self):
        self.service_name = None
        self.cluster_id = None
        self.network_partition_id = None
        self.partition_id = None
        self.member_id = None
        self.port_map = {}
        self.member_ip = None

    def get_port(self, proxy_port):
        if proxy_port in self.port_map:
            return self.port_map[proxy_port]

        return None

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberActivatedEvent()

        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.network_partition_id = json_obj["networkPartitionId"] if "networkPartitionId" in json_obj else None
        instance.partition_id = json_obj["partitionId"] if "partitionId" in json_obj else None
        instance.member_id = json_obj["memberId"] if "memberId" in json_obj else None
        #instance.port_map = json_obj["portMap"] if "portMap" in json_obj else {}
        instance.member_ip = json_obj["memberIp"] if "memberIp" in json_obj else None

        for port_proxy in json_obj["portMap"]:
            port_str = json_obj["portMap"][port_proxy]
            port_obj = Port(port_str["protocol"], port_str["value"], port_proxy)
            instance.port_map[port_proxy] = port_obj


class MemberTerminatedEvent:

    def __init__(self):
        self.service_name = None
        self.cluster_id = None
        self.network_partition_id = None
        self.partition_id = None
        self.member_id = None
        self.properties = {}

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberTerminatedEvent()

        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.network_partition_id = json_obj["networkPartitionId"] if "networkPartitionId" in json_obj else None
        instance.partition_id = json_obj["partitionId"] if "partitionId" in json_obj else None
        instance.member_id = json_obj["memberId"] if "memberId" in json_obj else None


class MemberSuspendedEvent:

    def __init__(self):
        self.service_name = None
        self.cluster_id = None
        self.network_partition_id = None
        self.partition_id = None
        self.member_id = None

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberSuspendedEvent()

        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.network_partition_id = json_obj["networkPartitionId"] if "networkPartitionId" in json_obj else None
        instance.partition_id = json_obj["partitionId"] if "partitionId" in json_obj else None
        instance.member_id = json_obj["memberId"] if "memberId" in json_obj else None


class CompleteTopologyEvent:

    def __init__(self):
        self.topology = None

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = CompleteTopologyEvent()

        topology_str = json_obj["topology"] if "topology" in json_obj else None
        if topology_str is not None:
            topology_obj = Topology()
            topology_obj.json_str = topology_str

            #add service map
            for service_name in topology_str["serviceMap"]:
                service_str = topology_str["serviceMap"][service_name]

                service_obj = Service(service_name, service_str["serviceType"])
                service_obj.properties = service_str["properties"]
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
                    cl_deployment_policy_name = cluster_str["deploymentPolicyName"]

                    cluster_obj = Cluster(cl_service_name, cluster_id, cl_deployment_policy_name, cl_autoscale_policy_name)
                    cluster_obj.hostnames = cluster_str["hostNames"]
                    cluster_obj.tenant_range = cluster_str["tenantRange"]
                    cluster_obj.is_lb_cluster = cluster_str["isLbCluster"]
                    cluster_obj.status = cluster_str["status"]
                    cluster_obj.load_balancer_algorithm_name = cluster_str["loadBalanceAlgorithmName"]
                    cluster_obj.properties = cluster_str["properties"]

                    #add member map
                    for member_id in cluster_str["memberMap"]:
                        member_str = cluster_str["memberMap"][member_id]
                        mm_service_name = member_str["serviceName"]
                        mm_cluster_id = member_str["clusterId"]
                        mm_network_partition_id = member_str["networkPartitionId"]
                        mm_partition_id = member_str["partitionId"]

                        member_obj = Member(mm_service_name, mm_cluster_id, mm_network_partition_id, mm_partition_id, member_id)
                        member_obj.member_public_ip = member_str["memberPublicIp"]
                        member_obj.status = member_str["status"]
                        member_obj.member_ip = member_str["memberIp"]
                        member_obj.properties = member_str["properties"]
                        member_obj.lb_cluster_id = member_str["lbClusterId"]
                        member_obj.json_str = member_str

                        #add port map
                        for mm_port_proxy in member_str["portMap"]:
                            mm_port_str = member_str["portMap"][port_proxy]
                            mm_port_obj = Port(mm_port_str["protocol"], mm_port_str["value"], mm_port_proxy)
                            member_obj.add_port(mm_port_obj)
                        cluster_obj.add_member(member_obj)
                    service_obj.add_cluster(cluster_obj)
                topology_obj.add_service(service_obj)
            instance.topology = topology_obj

        return instance


class MemberStartedEvent:

    def __init__(self):
        self.service_name = None
        self.cluster_id = None
        self.network_partition_id = None
        self.partition_id = None
        self.member_id = None
        self.status = None
        self.properties = {}

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberStartedEvent()

        instance.service_name = json_obj["serviceName"] if "serviceName" in json_obj else None
        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.network_partition_id = json_obj["networkPartitionId"] if "networkPartitionId" in json_obj else None
        instance.partition_id = json_obj["partitionId"] if "partitionId" in json_obj else None
        instance.member_id = json_obj["memberId"] if "memberId" in json_obj else None

