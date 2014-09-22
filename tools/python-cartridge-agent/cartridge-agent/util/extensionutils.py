import logging

logging.basicConfig(level=logging.DEBUG)
log = logging.getLogger(__name__)


def execute_copy_artifact_extension(source, destination):
    raise NotImplementedError


def execute_instance_started_extention(env_params):
    raise NotImplementedError


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