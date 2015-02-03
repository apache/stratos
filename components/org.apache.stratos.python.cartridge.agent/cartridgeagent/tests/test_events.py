# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

from .. cartridgeagent.modules.event.topology.events import CompleteTopologyEvent
import pytest


def test_complete_topology_event():
    with open("conf/complete_topology_event.json", "r") as f:
        event_json = f.read()

    event_object = CompleteTopologyEvent.create_from_json(event_json)
    topology = event_object.get_topology()

    assert not topology.initialized and \
        len(topology.service_map.keys()) == 1 and \
        topology.service_map.keys()[0] == "tomcat", \
        "Topology object not serialized properly"

    service = topology.get_service("tomcat")

    assert service.service_name == "tomcat", \
        "Service object not serialized properly. [serviceName] %s" % service.service_name
    assert service.service_type == "SingleTenant", \
        "Service object not serialized properly. [srviceType] %s" % service.service_type
    assert len(service.cluster_id_cluster_map.keys()) == 1, \
        "Service object not serialized properly. Count[clusterIdClusterMap] %s" % \
        len(service.cluster_id_cluster_map.keys())
    assert service.cluster_id_cluster_map.keys()[0] == "php1.tomcat.domain", \
        "Service object not serialized properly. [clusterIdClusterMap] %s" % service.cluster_id_cluster_map.keys()[0]
    assert len(service.port_map.keys()) == 1, \
        "Service object not serialized properly. Count[portMap] %s" % len(service.port_map.keys())
    assert service.port_map.keys()[0] == "8280", \
        "Service object not serialized properly. [portMap] %s" % service.port_map.keys()[0]
    assert len(service.properties.keys()) == 0,  \
        "Service object not serialized properly. Count[properties] %s" % len(service.properties.keys())

    cluster = service.get_cluster("php1.tomcat.domain")

    assert cluster.service_name == "tomcat", \
        "Cluster object not serialized properly. [serviceName] %s" % cluster.service_name
    assert cluster.cluster_id == "php1.tomcat.domain", \
        "Cluster object not serialized properly. [clusterId] %s" % cluster.cluster_id
    assert cluster.deployment_policy_name is None, \
        "Cluster object not serialized properly. [deploymentPolicyName] %s" % cluster.deployment_policy_name
    assert cluster.autoscale_policy_name == "autoscale_policy_1", \
        "Cluster object not serialized properly. [autoscalePolicyName] %s" % cluster.autoscale_policy_name
    assert len(cluster.hostnames) == 1, \
        "Cluster object not serialized properly. Count[hostNames] %s" % len(cluster.hostnames)
    assert cluster.tenant_range == "*", \
        "Cluster object not serialized properly. [tenantRange] %s" % cluster.tenant_range
    assert not cluster.is_lb_cluster, \
        "Cluster object not serialized properly. [isLbCluster] %s" % cluster.is_lb_cluster
    assert not cluster.is_kubernetes_cluster, \
        "Cluster object not serialized properly. [isKubernetesCluster] %s" % cluster.is_kubernetes_cluster
    assert cluster.load_balancer_algorithm_name is None, \
        "Cluster object not serialized properly. [loadBalancerAlogrithmName] %s" % cluster.load_balancer_algorithm_name
    assert cluster.app_id == "single-cartridge-app", \
        "Cluster object not serialized properly. [appId] %s" % cluster.app_id
    assert len(cluster.properties.keys()) == 0, \
        "Cluster object not serialized properly. Count[properties] %s" % len(cluster.properties.keys())
    assert len(cluster.member_map.keys()) == 1, \
        "Cluster object not serialized properly. Count[memberMap] %s" % len(cluster.member_map.keys())
    assert cluster.member_exists("php1.tomcat.domain6d4d09ee-2ec8-4c00-962d-3449305a4dfa"), \
        "Cluster object not serialized properly. Targeted member not found."

    # assert cluster.tenant_id_in_range("222"), "Checking tenant id in range"

    try:
        cluster.validate_tenant_range("*")
    except RuntimeError:
        pytest.fail("Validating tenant range logic failed. [input] *")

    try:
        cluster.validate_tenant_range("22-45")
    except RuntimeError:
        pytest.fail("Validating tenant range logic failed. [input] 22-45")

    with pytest.raises(RuntimeError):
        cluster.validate_tenant_range("rf-56")

    member = cluster.get_member("php1.tomcat.domain6d4d09ee-2ec8-4c00-962d-3449305a4dfa")

    assert member.service_name == "tomcat",\
        "Member object not serialized properly. [serviceName] %s " % member.service_name
    assert member.cluster_id == "php1.tomcat.domain",\
        "Member object not serialized properly. [clusterId] %s " % member.cluster_id
    assert member.member_id == "php1.tomcat.domain6d4d09ee-2ec8-4c00-962d-3449305a4dfa",\
        "Member object not serialized properly. [memberId] %s " % member.member_id
    assert member.cluster_instance_id == "single-cartridge-app-1",\
        "Member object not serialized properly. [clusterInstanceId] %s " % member.cluster_instance_id
    assert member.network_partition_id == "openstack_R1",\
        "Member object not serialized properly. [networkPartitionId] %s " % member.network_partition_id
    assert member.partition_id == "P1",\
        "Member object not serialized properly. [partitionId] %s " % member.partition_id
    assert member.init_time == 1422699519228,\
        "Member object not serialized properly. [initTime] %s " % member.init_time
    assert member.member_public_ips[0] == "192.168.17.200",\
        "Member object not serialized properly. [memberPublicIps] %s " % member.member_public_ips[0]
    assert member.member_default_public_ip == member.member_public_ips[0],\
        "Member object not serialized properly. [defaultPublicIp] %s " % member.member_default_public_ip
    assert member.member_private_ips[0] == "10.0.0.59",\
        "Member object not serialized properly. [memberPrivateIps] %s " % member.member_private_ips[0]
    assert member.member_default_private_ip == member.member_private_ips[0],\
        "Member object not serialized properly. [defaultPrivateIp] %s " % member.member_default_private_ip
    assert member.properties["MIN_COUNT"] == "1",\
        "Member object not serialized properly. [properties] %s " % member.properties["MIN_COUNT"]
    assert member.status == "Initialized",\
        "Member object not serialized properly. [status] %s " % member.status
    assert member.lb_cluster_id is None,\
        "Member object not serialized properly. [lbClusterId] %s " % member.lb_cluster_id