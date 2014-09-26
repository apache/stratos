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
    try:
        log.debug("Executing instance activated extension")
        script_name = cartridgeagentconstants.INSTANCE_ACTIVATED_SCRIPT
        command = prepare_command(script_name)

        output, errors = execute_command(command)
        log.debug("Instance activated script returned: %r" % output)
    except:
        log.exception("Could not execute instance activated extension")


def execute_artifacts_updated_extension(env_params):
    try:
        log.debug("Executing artifacts updated extension")

        script_name = cartridgeagentconstants.ARTIFACTS_UPDATED_SCRIPT
        command = prepare_command(script_name)
        env_params = add_payload_parameters(env_params)
        env_params = clean_process_parameters(env_params)

        output, errors = execute_command(command, env_params)
        log.debug("Artifacts updated script returned: %r" % output)
    except:
        log.exception("Could not execute artifacts updated extension")


def execute_subscription_domain_added_extension(tenant_id, tenant_domain, domain_name, application_context):
    raise NotImplementedError


def execute_subscription_domain_removed_extension(tenant_id, tenant_domain, domain_name):
    raise NotImplementedError


def wait_for_complete_topology():
    raise NotImplementedError


def check_topology_consistency(service_name, cluster_id, member_id):
    topology = TopologyContext.get_topology()
    service = topology.get_service(service_name)
    if service is None:
        log.error("Service not found in topology [service] %r" % service_name)
        return False

    cluster = service.get_cluster(cluster_id)
    if cluster is None:
        log.error("Cluster id not found in topology [cluster] %r" % cluster_id)
        return False

    activated_member = cluster.get_member(member_id)
    if activated_member is None:
        log.error("Member id not found in topology [member] %r" % member_id)
        return False

    return True


def is_relevant_member_event(service_name, cluster_id, lb_cluster_id):
    cluster_id_in_payload = CartridgeAgentConfiguration.cluster_id
    if cluster_id_in_payload is None:
        return False

    topology = TopologyContext.get_topology()
    if topology is None or not topology.initialized:
        return False

    if cluster_id_in_payload == cluster_id:
        return True

    if cluster_id_in_payload == lb_cluster_id:
        return True

    service_group_in_payload = CartridgeAgentConfiguration.service_group
    if service_group_in_payload is not None:
        service_properties = topology.get_service(service_name).properties
        if service_properties is None:
            return False

        member_service_group = service_properties[cartridgeagentconstants.SERVICE_GROUP_TOPOLOGY_KEY]
        if member_service_group is not None and member_service_group == service_group_in_payload:
            if service_name == CartridgeAgentConfiguration.service_name:
                log.debug("Service names are same")
                return True
            elif "apistore" == CartridgeAgentConfiguration.service_name and service_name == "publisher":
                log.debug("Service name in payload is [store]. Serivce name in event is [%r] " % service_name)
                return True
            elif "publisher" == CartridgeAgentConfiguration.service_name and service_name == "apistore":
                log.debug("Service name in payload is [publisher]. Serivce name in event is [%r] " % service_name)
                return True
            elif cartridgeagentconstants.DEPLOYMENT_WORKER == CartridgeAgentConfiguration.deployment and service_name == CartridgeAgentConfiguration.manager_service_name:
                log.debug("Deployment is worker. Worker's manager service name & service name in event are same")
                return True
            elif cartridgeagentconstants.DEPLOYMENT_MANAGER == CartridgeAgentConfiguration.deployment  and service_name == CartridgeAgentConfiguration.worker_service_name:
                log.debug("Deployment is manager. Manager's worker service name & service name in event are same")
                return True

    return False


def execute_volume_mount_extension(persistance_mappings_payload):
    raise NotImplementedError


def execute_cleanup_extension():
    try:
        log.debug("Executing cleanup extension")
        script_name = cartridgeagentconstants.CLEAN_UP_SCRIPT
        command = prepare_command(script_name)

        output, errors = execute_command(command)
        log.debug("Cleanup script returned: %r" % output)
    except:
        log.exception("Could not execute Cleanup extension")


def execute_member_activated_extension(env_params):
    try:
        log.debug("Executing member activated extension")

        script_name = cartridgeagentconstants.MEMBER_ACTIVATED_SCRIPT
        command = prepare_command(script_name)
        env_params = add_payload_parameters(env_params)
        env_params = clean_process_parameters(env_params)

        output, errors = execute_command(command, env_params)
        log.debug("Member activated script returned: %r" % output)
    except:
        log.exception("Could not execute member activated extension")


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
        service = topology.get_service(CartridgeAgentConfiguration.service_name)
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


def execute_command(command, env_params=None):
    """

    :param str command:
    :param dict env_params:
    :return: output and error tuple
    :rtype: tuple
    """
    os_env = os.environ.copy()
    if env_params is not None:
        os_env.update(env_params)

    p = subprocess.Popen([command], stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=os_env)
    output, errors = p.communicate()
    log.debug("output = %r" % output)
    log.debug("error = %r" % errors)
    if len(errors) > 0:
        raise RuntimeError("Command execution failed: \n %r" % errors)

    return output, errors