import ConfigParser
import logging
import os

from ..util import cartridgeagentconstants
from ..exception.parameternotfoundexception import ParameterNotFoundException


class CartridgeAgentConfiguration:
    # set log level
    logging.basicConfig(level=logging.DEBUG)
    log = logging.getLogger(__name__)

    payload_params = {}
    properties = None

    service_group = None
    is_clustered = False
    service_name = None
    cluster_id = None
    network_partition_id = None
    partition_id = None
    member_id = None
    cartridge_key = None
    app_path = None
    repo_url = None
    ports = {}
    log_file_paths = {}
    is_multitenant = False
    persistence_mappings = None
    is_commits_enabled = False
    is_checkout_enabled = False
    listen_address = None
    is_internal_repo = False
    tenant_id = None
    lb_cluster_id = None
    min_count = None
    lb_private_ip = None
    lb_public_ip = None
    tenant_repository_path = None
    super_tenant_repository_path = None
    deployment = None
    manager_service_name = None
    worker_service_name = None
    is_primary = False

    @staticmethod
    def initialize_configuration():

        CartridgeAgentConfiguration.payload_params = {}
        CartridgeAgentConfiguration.__read_conf_file()
        CartridgeAgentConfiguration.__read_parameter_file()

        try:
            CartridgeAgentConfiguration.service_group = CartridgeAgentConfiguration.payload_params[
                cartridgeagentconstants.SERVICE_GROUP] \
                if cartridgeagentconstants.SERVICE_GROUP in CartridgeAgentConfiguration.payload_params \
                else None

            if cartridgeagentconstants.CLUSTERING in CartridgeAgentConfiguration.payload_params and \
                            str(CartridgeAgentConfiguration.payload_params[
                                cartridgeagentconstants.CLUSTERING]).strip().lower() == "true":
                CartridgeAgentConfiguration.is_clustered = True
            else:
                CartridgeAgentConfiguration.is_clustered = False
            # CartridgeAgentConfiguration.__isClustered = CartridgeAgentConfiguration.payload_params[
            # cartridgeagentconstants.CLUSTERING] if cartridgeagentconstants.CLUSTERING in CartridgeAgentConfiguration.payload_params else None

            CartridgeAgentConfiguration.service_name = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.SERVICE_NAME)
            CartridgeAgentConfiguration.cluster_id = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.CLUSTER_ID)
            CartridgeAgentConfiguration.network_partition_id = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.NETWORK_PARTITION_ID)
            CartridgeAgentConfiguration.partition_id = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.PARTITION_ID)
            CartridgeAgentConfiguration.member_id = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.MEMBER_ID)
            CartridgeAgentConfiguration.cartridge_key = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.CARTRIDGE_KEY)
            CartridgeAgentConfiguration.app_path = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.APP_PATH)
            CartridgeAgentConfiguration.repo_url = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.REPO_URL)
            CartridgeAgentConfiguration.ports = str(
                CartridgeAgentConfiguration.read_property(cartridgeagentconstants.PORTS)).split("|")

            try:
                CartridgeAgentConfiguration.log_file_paths = str(
                    CartridgeAgentConfiguration.read_property(cartridgeagentconstants.CLUSTER_ID)).strip().split("|")
            except ParameterNotFoundException as ex:
                CartridgeAgentConfiguration.log.debug("Cannot read log file path : %r" % ex.get_message())
                CartridgeAgentConfiguration.log_file_paths = None

            is_multi_str = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.CLUSTER_ID)
            CartridgeAgentConfiguration.is_multitenant = True if str(
                is_multi_str).lower().strip() == "true" else False

            try:
                CartridgeAgentConfiguration.persistence_mappings = CartridgeAgentConfiguration.read_property(
                    "PERSISTENCE_MAPPING")
            except ParameterNotFoundException as ex:
                CartridgeAgentConfiguration.log.debug("Cannot read persistence mapping : %r" % ex.get_message())
                CartridgeAgentConfiguration.persistence_mappings = None

            try:
                is_commit_str = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.COMMIT_ENABLED)
                CartridgeAgentConfiguration.is_commits_enabled = True if str(
                    is_commit_str).lower().strip() == "true" else False
            except ParameterNotFoundException:
                try:
                    is_commit_str = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.AUTO_COMMIT)
                    CartridgeAgentConfiguration.is_commits_enabled = True if str(
                        is_commit_str).lower().strip() == "true" else False
                except ParameterNotFoundException:
                    CartridgeAgentConfiguration.log.info(
                        "%r is not found and setting it to false" % cartridgeagentconstants.COMMIT_ENABLED)
                    CartridgeAgentConfiguration.is_commits_enabled = False

            auto_checkout_str = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.AUTO_CHECKOUT)
            CartridgeAgentConfiguration.is_checkout_enabled = True if str(
                auto_checkout_str).lower().strip() == "true" else False

            CartridgeAgentConfiguration.listen_address = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.LISTEN_ADDRESS)

            try:
                int_repo_str = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.PROVIDER)
                CartridgeAgentConfiguration.is_internal_repo = True if str(
                    int_repo_str).strip().lower() == cartridgeagentconstants.INTERNAL else False
            except ParameterNotFoundException:
                CartridgeAgentConfiguration.log.info(" INTERNAL payload parameter is not found")
                CartridgeAgentConfiguration.is_internal_repo = False

            CartridgeAgentConfiguration.tenant_id = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.TENANT_ID)
            CartridgeAgentConfiguration.lb_cluster_id = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.LB_CLUSTER_ID)
            CartridgeAgentConfiguration.min_count = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.MIN_INSTANCE_COUNT)
            CartridgeAgentConfiguration.lb_private_ip = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.LB_PRIVATE_IP)
            CartridgeAgentConfiguration.lb_public_ip = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.LB_PUBLIC_IP)
            CartridgeAgentConfiguration.tenant_repository_path = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.TENANT_REPO_PATH)
            CartridgeAgentConfiguration.super_tenant_repository_path = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.SUPER_TENANT_REPO_PATH)

            try:
                CartridgeAgentConfiguration.deployment = CartridgeAgentConfiguration.read_property(
                    cartridgeagentconstants.DEPLOYMENT)
            except ParameterNotFoundException:
                CartridgeAgentConfiguration.deployment = None

            # Setting worker-manager setup - manager service name
            if CartridgeAgentConfiguration.deployment is None:
                CartridgeAgentConfiguration.manager_service_name = None

            if str(
                    CartridgeAgentConfiguration.deployment).lower() == cartridgeagentconstants.DEPLOYMENT_MANAGER.lower():
                CartridgeAgentConfiguration.manager_service_name = CartridgeAgentConfiguration.service_name

            elif str(
                    CartridgeAgentConfiguration.deployment).lower() == cartridgeagentconstants.DEPLOYMENT_WORKER.lower():
                CartridgeAgentConfiguration.deployment = CartridgeAgentConfiguration.read_property(
                    cartridgeagentconstants.MANAGER_SERVICE_TYPE)

            elif str(
                    CartridgeAgentConfiguration.deployment).lower() == cartridgeagentconstants.DEPLOYMENT_DEFAULT.lower():
                CartridgeAgentConfiguration.deployment = None
            else:
                CartridgeAgentConfiguration.deployment = None

            # Setting worker-manager setup - worker service name
            if CartridgeAgentConfiguration.deployment is None:
                CartridgeAgentConfiguration.worker_service_name = None

            if str(
                    CartridgeAgentConfiguration.deployment).lower() == cartridgeagentconstants.DEPLOYMENT_WORKER.lower():
                CartridgeAgentConfiguration.manager_service_name = CartridgeAgentConfiguration.service_name

            elif str(
                    CartridgeAgentConfiguration.deployment).lower() == cartridgeagentconstants.DEPLOYMENT_MANAGER.lower():
                CartridgeAgentConfiguration.deployment = CartridgeAgentConfiguration.read_property(
                    cartridgeagentconstants.WORKER_SERVICE_TYPE)

            elif str(
                    CartridgeAgentConfiguration.deployment).lower() == cartridgeagentconstants.DEPLOYMENT_DEFAULT.lower():
                CartridgeAgentConfiguration.deployment = None
            else:
                CartridgeAgentConfiguration.deployment = None

            try:
                CartridgeAgentConfiguration.is_primary = CartridgeAgentConfiguration.read_property(
                    cartridgeagentconstants.CLUSTERING_PRIMARY_KEY)
            except ParameterNotFoundException:
                CartridgeAgentConfiguration.is_primary = None
        except ParameterNotFoundException as ex:
            raise RuntimeError(ex)

        CartridgeAgentConfiguration.log.info("Cartridge agent configuration initialized")

        CartridgeAgentConfiguration.log.debug("service-name: %r" % CartridgeAgentConfiguration.service_name)
        CartridgeAgentConfiguration.log.debug("cluster-id: %r" % CartridgeAgentConfiguration.cluster_id)
        CartridgeAgentConfiguration.log.debug(
            "network-partition-id: %r" % CartridgeAgentConfiguration.network_partition_id)
        CartridgeAgentConfiguration.log.debug("partition-id: %r" % CartridgeAgentConfiguration.partition_id)
        CartridgeAgentConfiguration.log.debug("member-id: %r" % CartridgeAgentConfiguration.member_id)
        CartridgeAgentConfiguration.log.debug("cartridge-key: %r" % CartridgeAgentConfiguration.cartridge_key)
        CartridgeAgentConfiguration.log.debug("app-path: %r" % CartridgeAgentConfiguration.app_path)
        CartridgeAgentConfiguration.log.debug("repo-url: %r" % CartridgeAgentConfiguration.repo_url)
        CartridgeAgentConfiguration.log.debug("ports: %r" % str(CartridgeAgentConfiguration.ports))
        CartridgeAgentConfiguration.log.debug("lb-private-ip: %r" % CartridgeAgentConfiguration.lb_private_ip)
        CartridgeAgentConfiguration.log.debug("lb-public-ip: %r" % CartridgeAgentConfiguration.lb_public_ip)

    @staticmethod
    def __read_conf_file():
        """
        Reads and stores the agent's configuration file
        :return:
        """

        base_working_dir = os.path.abspath(os.path.dirname(__file__)).replace("modules/config", "")
        conf_file_path = base_working_dir + "agent.conf"
        CartridgeAgentConfiguration.log.debug("Config file path : %r" % conf_file_path)
        CartridgeAgentConfiguration.properties = ConfigParser.SafeConfigParser()
        CartridgeAgentConfiguration.properties.read(conf_file_path)

    @staticmethod
    def __read_parameter_file():
        """
        Reads the payload file of the cartridge and stores the values in a dictionary
        :return:
        """

        param_file = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.PARAM_FILE_PATH)

        try:
            if param_file is not None:
                metadata_file = open(param_file)
                metadata_payload_content = metadata_file.read()
                CartridgeAgentConfiguration.payload_params = dict(
                    param.split("=") for param in metadata_payload_content.split(","))
                metadata_file.close()
            else:
                CartridgeAgentConfiguration.log.error("File not found: %r" % param_file)
        except:
            CartridgeAgentConfiguration.log.error(
                "Could not read launch parameter file, hence trying to read from System properties.")

    @staticmethod
    def read_property(property_key):

        if CartridgeAgentConfiguration.properties.has_option("agent", property_key):
            CartridgeAgentConfiguration.log.debug("Has key: %r" % property_key)
            temp_str = CartridgeAgentConfiguration.properties.get("agent", property_key)
            if temp_str != "" and temp_str is not None:
                return temp_str

        if property_key in CartridgeAgentConfiguration.payload_params:
            temp_str = CartridgeAgentConfiguration.payload_params[property_key]
            if temp_str != "" and temp_str is not None:
                return temp_str

        raise ParameterNotFoundException("Cannot find the value of required parameter: %r" % property_key)