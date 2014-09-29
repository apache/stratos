import ConfigParser
import logging
import os

from ..util import cartridgeagentconstants
from ..exception.parameternotfoundexception import ParameterNotFoundException


class CartridgeAgentConfiguration:
    """
    Handles the configuration information of the particular Cartridge Agent
    """
    # set log level
    logging.basicConfig(level=logging.DEBUG, filename='/tmp/cartridge-agent.log')
    log = logging.getLogger(__name__)

    payload_params = {}
    properties = None

    service_group = None
    """ :type : str  """
    is_clustered = False
    """ :type : bool  """
    service_name = None
    """ :type : str  """
    cluster_id = None
    """ :type : str  """
    network_partition_id = None
    """ :type : str  """
    partition_id = None
    """ :type : str  """
    member_id = None
    """ :type : str  """
    cartridge_key = None
    """ :type : str  """
    app_path = None
    """ :type : str  """
    repo_url = None
    """ :type : str  """
    ports = []
    """ :type : list[str]  """
    log_file_paths = []
    """ :type : list[str]  """
    is_multitenant = False
    """ :type : bool  """
    persistence_mappings = None
    """ :type : str  """
    is_commits_enabled = False
    """ :type : bool  """
    is_checkout_enabled = False
    """ :type : bool  """
    listen_address = None
    """ :type : str  """
    is_internal_repo = False
    """ :type : bool  """
    tenant_id = None
    """ :type : str  """
    lb_cluster_id = None
    """ :type : str  """
    min_count = None
    """ :type : str  """
    lb_private_ip = None
    """ :type : str  """
    lb_public_ip = None
    """ :type : str  """
    tenant_repository_path = None
    """ :type : str  """
    super_tenant_repository_path = None
    """ :type : str  """
    deployment = None
    """ :type : str  """
    manager_service_name = None
    """ :type : str  """
    worker_service_name = None
    """ :type : str  """
    is_primary = False
    """ :type : bool  """

    @staticmethod
    def initialize_configuration():
        """
        Initializes the configuration by reading and parsing properties
        from configuration file and payload parameter file
        :return: void
        """

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

            auto_checkout_str = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.AUTO_CHECKOUT, False)
            CartridgeAgentConfiguration.is_checkout_enabled = True if str(
                auto_checkout_str).lower().strip() == "true" else False

            CartridgeAgentConfiguration.listen_address = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.LISTEN_ADDRESS, False)

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
                cartridgeagentconstants.LB_PRIVATE_IP, False)
            CartridgeAgentConfiguration.lb_public_ip = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.LB_PUBLIC_IP, False)
            CartridgeAgentConfiguration.tenant_repository_path = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.TENANT_REPO_PATH, False)
            CartridgeAgentConfiguration.super_tenant_repository_path = CartridgeAgentConfiguration.read_property(
                cartridgeagentconstants.SUPER_TENANT_REPO_PATH, False)

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
        :return: void
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
        :return: void
        """

        param_file = CartridgeAgentConfiguration.read_property(cartridgeagentconstants.PARAM_FILE_PATH, False)

        try:
            if param_file is not None:
                metadata_file = open(param_file)
                metadata_payload_content = metadata_file.read()
                for param in metadata_payload_content.split(","):
                    if param.strip() != "":
                        param_value = param.strip().split("=")
                        CartridgeAgentConfiguration.payload_params[param_value[0]] = param_value[1]

                # CartridgeAgentConfiguration.payload_params = dict(
                #     param.split("=") for param in metadata_payload_content.split(","))
                metadata_file.close()
            else:
                CartridgeAgentConfiguration.log.error("File not found: %r" % param_file)
        except:
            CartridgeAgentConfiguration.log.exception(
                "Could not read launch parameter file, hence trying to read from System properties.")

    @staticmethod
    def read_property(property_key, critical=True):
        """
        Returns the value of the provided property
        :param str property_key: the name of the property to be read
        :return: Value of the property,
        :rtype: str
        :exception: ParameterNotFoundException if the provided property cannot be found
        """

        if CartridgeAgentConfiguration.properties.has_option("agent", property_key):
            CartridgeAgentConfiguration.log.debug("Has key: %r" % property_key)
            temp_str = CartridgeAgentConfiguration.properties.get("agent", property_key)
            if temp_str != "" and temp_str is not None:
                return temp_str

        if property_key in CartridgeAgentConfiguration.payload_params:
            temp_str = CartridgeAgentConfiguration.payload_params[property_key]
            if temp_str != "" and temp_str is not None:
                return temp_str

        if critical:
            raise ParameterNotFoundException("Cannot find the value of required parameter: %r" % property_key)
