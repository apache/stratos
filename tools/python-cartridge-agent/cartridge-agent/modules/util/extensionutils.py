import logging
import os
import subprocess

from .. config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from .. topology.topologycontext import *

logging.basicConfig(level=logging.DEBUG)
log = logging.getLogger(__name__)


def execute_copy_artifact_extension(source, destination):
    raise NotImplementedError


def execute_instance_started_extension(env_params):
    try:
        log.debug("Executing instance started extension")

        script_name = cartridgeagentconstants.INSTANCE_STARTED_SCRIPT
        command = prepare_command(script_name)
        env_params = add_payload_parameters(env_params)
        env_params = clean_process_parameters(env_params)

        output, errors = execute_command(command, env_params)
        log.debug("Instance started script returned: %r" % output)
    except:
        log.exception("Could not execute instance started extension")


def execute_instance_activated_extension():
    raise NotImplementedError


def execute_artifacts_updated_extension(env_params):
    raise NotImplementedError


def execute_subscription_domain_added_extension(tenant_id, tenant_domain, domain_name, application_context):
    raise NotImplementedError


def execute_subscription_domain_removed_extension(tenant_id, tenant_domain, domain_name):
    raise NotImplementedError


def wait_for_complete_topology():
    raise NotImplementedError


def check_topology_consistency(service_name, cluster_id, member_id):
    raise NotImplementedError


def execute_volume_mount_extension(persistance_mappings_payload):
    raise NotImplementedError


def prepare_command(script_name):
    extensions_dir = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.EXTENSIONS_DIR)
    if extensions_dir.strip() == "":
        raise RuntimeError("System property not found: %r" % cartridgeagentconstants.EXTENSIONS_DIR)

    file_path = extensions_dir + script_name if str(extensions_dir).endswith("/") else extensions_dir + "/" + script_name

    if os.path.isfile(file_path):
        return file_path

    raise IOError("Script file not found : %r" % file_path)


def clean_process_parameters(params):
    """

    :param dict params:
    :return: cleaned parameters
    :rtype: dict
    """
    for key, value in params.items():
        if value is None:
            del params[key]

    return params


def add_payload_parameters(params):
    params["STRATOS_APP_PATH"] = CartridgeAgentConfiguration.app_path
    params["STRATOS_PARAM_FILE_PATH"] = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.PARAM_FILE_PATH)
    params["STRATOS_SERVICE_NAME"] = CartridgeAgentConfiguration.service_name
    params["STRATOS_TENANT_ID"] = CartridgeAgentConfiguration.tenant_id
    params["STRATOS_CARTRIDGE_KEY"] = CartridgeAgentConfiguration.cartridge_key
    params["STRATOS_LB_CLUSTER_ID"] = CartridgeAgentConfiguration.lb_cluster_id
    params["STRATOS_CLUSTER_ID"] = CartridgeAgentConfiguration.cluster_id
    params["STRATOS_NETWORK_PARTITION_ID"] = CartridgeAgentConfiguration.network_partition_id
    params["STRATOS_PARTITION_ID"] = CartridgeAgentConfiguration.partition_id
    params["STRATOS_PERSISTENCE_MAPPINGS"] = CartridgeAgentConfiguration.persistence_mappings
    params["STRATOS_REPO_URL"] = CartridgeAgentConfiguration.repo_url

    lb_cluster_id_in_payload = CartridgeAgentConfiguration.lb_cluster_id
    member_ips = get_lb_member_ip(lb_cluster_id_in_payload)
    if member_ips is not None:
        params["STRATOS_LB_IP"] = member_ips[0]
        params["STRATOS_LB_PUBLIC_IP"] = member_ips[1]
    else:
        params["STRATOS_LB_IP"] = CartridgeAgentConfiguration.lb_private_ip
        params["STRATOS_LB_PUBLIC_IP"] = CartridgeAgentConfiguration.lb_public_ip

    topology = TopologyContext.get_topology()
    if topology.initialized:
        service = topology.service_map[CartridgeAgentConfiguration.service_name]
        cluster = service.get_cluster(CartridgeAgentConfiguration.cluster_id)
        member_id_in_payload = CartridgeAgentConfiguration.member_id
        add_properties(service.properties, params, "SERVICE_PROPERTY")
        add_properties(cluster.properties, params, "CLUSTER_PROPERTY")
        add_properties(cluster.get_member(member_id_in_payload).properties, params, "MEMBER_PROPERTY")

    return params


def add_properties(properties, params, prefix):
    """
    :param dict properties: service properties
    :param dict params:
    :param str prefix:
    :return:
    """
    if properties is None or properties.items() is None:
        return

    for key in properties:
        params["STRATOS_" + prefix + "_" + key] = properties[key]
        log.debug("Property added: [key] STRATOS_ " +  prefix + "_" + key + "[value] " + properties[key])


def get_lb_member_ip(lb_cluster_id):
    topology = TopologyContext.get_topology()
    services = topology.get_services()

    for service in services:
        clusters = service.get_clusters()
        for cluster in clusters:
            members = cluster.get_members()
            for member in members:
                if member.cluster_id == lb_cluster_id:
                    return [member.member_ip, member.member_public_ip]

    return None


def execute_command(command, env_params):
    """

    :param str command:
    :param dict env_params:
    :return: output and error tuple
    :rtype: tuple
    """
    os_env = os.environ.copy()
    os_env.update(env_params)
    p = subprocess.Popen([command], stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=os_env)
    output, errors = p.communicate()
    log.debug("output = %r" % output)
    log.debug("error = %r" % errors)
    if len(errors) > 0:
        raise RuntimeError("Command execution failed: \n %r" % errors)

    return output, errors