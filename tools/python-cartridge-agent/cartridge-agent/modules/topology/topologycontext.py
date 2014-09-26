from ..util import cartridgeagentutils, cartridgeagentconstants


class TopologyContext:
    topology = None
    # TODO: read write locks, Lock() and RLock()

    @staticmethod
    def get_topology():
        #TODO: thread-safety missing
        if TopologyContext.topology is None:
            TopologyContext.topology = Topology()
        return TopologyContext.topology

        # @staticmethod
        # def update(topology):
        #     TopologyContext.topology = topology
        #     #TODO: persist in registry

    @staticmethod
    def update(topology):
        TopologyContext.topology = topology


class Topology:
    def __init__(self):
        self.service_map = {}
        self.initialized = False
        self.json_str = None

    def get_services(self):
        return self.service_map.values()

    def get_service(self, service_name):
        """

        :param str service_name: service name to be retrieved
        :return: Service object of the service
        :rtype: Service
        """
        if service_name in self.service_map:
            return self.service_map[service_name]

        return None

    def add_service(self, service):
        self.service_map[service.service_name] = service

    def add_services(self, services):
        for service in services:
            self.add_service(service)

    def remove_service(self, service_name):
        if service_name in self.service_map:
            self.service_map.pop(service_name)

    def service_exists(self, service_name):
        return service_name in self.service_map

    def clear(self):
        self.service_map = {}

    def __str__(self):
        return "Topology [serviceMap= %r , initialized= %r ]" % (self.service_map, self.initialized)


class Service:
    def __init__(self, service_name, service_type):
        self.service_name = service_name
        self.service_type = service_type
        self.cluster_id_cluster_map = {}
        self.port_map = {}
        self.properties = {}

    def get_clusters(self):
        return self.cluster_id_cluster_map.values()

    def add_cluster(self, cluster):
        self.cluster_id_cluster_map[cluster.cluster_id] = cluster

    def remove_cluster(self, cluster_id):
        if cluster_id in self.cluster_id_cluster_map:
            self.cluster_id_cluster_map.pop(cluster_id)

    def cluster_exists(self, cluster_id):
        return cluster_id in self.cluster_id_cluster_map

    def get_cluster(self, cluster_id):
        if cluster_id in self.cluster_id_cluster_map:
            return self.cluster_id_cluster_map[cluster_id]

        return None

    def get_ports(self):
        return self.port_map.values()

    def get_port(self, proxy):
        if proxy in self.port_map:
            return self.port_map[proxy]

        return None

    def add_port(self, port):
        self.port_map[port.proxy] = port

    def add_ports(self, ports):
        for port in ports:
            self.add_port(port)


class Cluster:
    def __init__(self, service_name, cluster_id, deployment_policy_name, autoscale_policy_name):
        self.service_name = service_name
        self.cluster_id = cluster_id
        self.deployment_policy_name = deployment_policy_name
        self.autoscale_policy_name = autoscale_policy_name
        self.hostnames = []
        self.member_map = {}

        self.tenant_range = None
        self.is_lb_cluster = False
        self.status = None
        self.load_balancer_algorithm_name = None
        self.properties = {}

    def add_hostname(self, hostname):
        self.hostnames.append(hostname)

    def set_tenant_range(self, tenant_range):
        cartridgeagentutils.validate_tenant_range(tenant_range)
        self.tenant_range = tenant_range

    def get_members(self):
        return self.member_map.values()

    def add_member(self, member):
        self.member_map[member.member_id] = member

    def remove_member(self, member_id):
        if self.member_exists(member_id):
            self.member_map.pop(member_id)

    def get_member(self, member_id):
        """

        :param member_id:
        :return:
        :rtype: Member
        """
        if self.member_exists(member_id):
            return self.member_map[member_id]

        return None

    def member_exists(self, member_id):
        return member_id in self.member_map

    def __str__(self):
        return "Cluster [serviceName=%r, clusterId=%r, autoscalePolicyName=%r, deploymentPolicyName=%r, hostNames=%r, tenantRange=%r, isLbCluster=%r, properties=%r]" % \
               (self.service_name, self.cluster_id, self.autoscale_policy_name, self.deployment_policy_name, self.hostnames, self.tenant_range, self.is_lb_cluster, self.properties)

    def tenant_id_in_range(self, tenant_id):
        if self.tenant_range is None:
            return False

        if self.tenant_range == "*":
            return True
        else:
            arr = self.tenant_range.split(cartridgeagentconstants.TENANT_RANGE_DELIMITER)
            tenant_start = int(arr[0])
            if tenant_start <= tenant_id:
                tenant_end = arr[1]
                if tenant_end == "*":
                    return True
                else:
                    if tenant_id <= int(tenant_end):
                        return True

        return False


class Member:

    def __init__(self, service_name, cluster_id, network_partition_id, parition_id, member_id):
        self.service_name = service_name
        self.cluster_id = cluster_id
        self.network_partition_id = network_partition_id
        self.partition_id = parition_id
        self.member_id = member_id
        self.port_map = {}

        self.member_public_ip = None
        self.status = None
        self.member_ip = None
        self.properties = {}
        self.lb_cluster_id = None
        self.json_str = None

    def is_active(self):
        return self.status == MemberStatus.Activated

    def get_ports(self):
        return self.port_map.values()

    def get_port(self, proxy):
        if proxy in self.port_map:
            return self.port_map[proxy]

        return None

    def add_port(self, port):
        self.port_map[port.proxy] = port

    def add_ports(self, ports):
        for port in ports:
            self.add_port(port)


class Port:

    def __init__(self, protocol, value, proxy):
        self.protocol = protocol
        self.value = value
        self.proxy = proxy

    def __str__(self):
        return "Port [protocol=%r, value=%r proxy=%r]" % (self.protocol, self.value, self.proxy)


class ServiceType:
    SingleTenant = 1
    MultiTenant = 2


class ClusterStatus:
    Created = 1
    In_Maintenance = 2
    Removed = 3


class MemberStatus:
    Created = 1
    Starting = 2
    Activated = 3
    In_Maintenance = 4
    ReadyToShutDown = 5
    Terminated = 6
    Suspended = 0
    ShuttingDown = 0