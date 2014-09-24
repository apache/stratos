import logging
import os

import cartridgeagentconstants
from .. config.cartridgeagentconfiguration import CartridgeAgentConfiguration

logging.basicConfig(level=logging.DEBUG)
log = logging.getLogger(__name__)

cartridge_agent_configuration = CartridgeAgentConfiguration()


def execute_copy_artifact_extension(source, destination):
    raise NotImplementedError


def execute_instance_started_extention(env_params):
    log.debug("Executing instance started extension")

    script_name = cartridgeagentconstants.INSTANCE_STARTED_SCRIPT
    command = prepare_command(script_name)



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
    extensions_dir = cartridge_agent_configuration.read_property(cartridgeagentconstants.EXTENSIONS_DIR)
    if extensions_dir.strip() == "":
        raise RuntimeError("System property not found: %r" % cartridgeagentconstants.EXTENSIONS_DIR)

    file_path = extensions_dir + script_name if str(extensions_dir).endswith("/") else extensions_dir + "/" + script_name

    if os.path.isfile(file_path):
        return file_path

    raise IOError("Script file not found : %r" % file_path)


def add_payload_parameters(params):
    params["STRATOS_APP_PATH"] = cartridge_agent_configuration.get_app_path()
    params["STRATOS_PARAM_FILE_PATH"] = cartridge_agent_configuration.read_property(cartridgeagentconstants.PARAM_FILE_PATH)
    params["STRATOS_SERVICE_NAME"] = cartridge_agent_configuration.get_service_name()
    params["STRATOS_TENANT_ID"] = cartridge_agent_configuration.get_tenant_id()
    params["STRATOS_CARTRIDGE_KEY"] = cartridge_agent_configuration.get_cartridge_key()
    params["STRATOS_LB_CLUSTER_ID"] = cartridge_agent_configuration.get_lb_cluster_id()
    params["STRATOS_CLUSTER_ID"] = cartridge_agent_configuration.get_cluster_id()
    params["STRATOS_NETWORK_PARTITION_ID"] = cartridge_agent_configuration.get_network_partition_id()
    params["STRATOS_PARTITION_ID"] = cartridge_agent_configuration.get_partition_id()
    params["STRATOS_PERSISTENCE_MAPPINGS"] = cartridge_agent_configuration.get_persistance_mappings()
    params["STRATOS_REPO_URL"] = cartridge_agent_configuration.get_repo_url()

    lb_cluster_id_in_payload = cartridge_agent_configuration.get_lb_cluster_id()


