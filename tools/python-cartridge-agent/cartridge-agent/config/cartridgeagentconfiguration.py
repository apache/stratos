import ConfigParser
import logging

from ..util import cartridgeagentconstants
from ..exception import ParameterNotFoundException


class CartridgeAgentConfiguration:
    class __CartridgeAgentConfiguration:

        def __init__(self):
            # set log level
            logging.basicConfig(level=logging.DEBUG)
            self.log = logging.getLogger(__name__)

            self.__read_conf_file()
            self.__read_parameter_file()

            try:
                self.__service_group = self.payload_params[cartridgeagentconstants.SERVICE_GROUP] \
                    if cartridgeagentconstants.SERVICE_GROUP in self.payload_params \
                    else None

                if cartridgeagentconstants.CLUSTERING in self.payload_params and \
                                str(self.payload_params[cartridgeagentconstants.CLUSTERING]).strip().lower() == "true":
                    self.__isClustered = True
                else:
                    self.__isClustered = False
                # self.__isClustered = self.payload_params[
                #     cartridgeagentconstants.CLUSTERING] if cartridgeagentconstants.CLUSTERING in self.payload_params else None

                self.__service_name = self.read_property(cartridgeagentconstants.SERVICE_NAME)
                self.__cluster_id = self.read_property(cartridgeagentconstants.CLUSTER_ID)
                self.__network_partition_id = self.read_property(cartridgeagentconstants.NETWORK_PARTITION_ID)
                self.__partitionId = self.read_property(cartridgeagentconstants.PARTITION_ID)
                self.__memberId = self.read_property(cartridgeagentconstants.MEMBER_ID)
                self.__cartridgeKey = self.read_property(cartridgeagentconstants.CARTRIDGE_KEY)
                self.__appPath = self.read_property(cartridgeagentconstants.APP_PATH)
                self.__repoUrl = self.read_property(cartridgeagentconstants.REPO_URL)
                self.__ports = str(self.read_property(cartridgeagentconstants.PORTS)).split("|")

                try:
                    self.__logFilePaths = str(self.read_property(cartridgeagentconstants.CLUSTER_ID)).strip().split("|")
                except ParameterNotFoundException as ex:
                    self.log.debug("Cannot read log file path : %r" % ex.get_message())
                    self.__logFilePaths = None

                is_multi_str = self.read_property(cartridgeagentconstants.CLUSTER_ID)
                self.__isMultitenant = True if str(is_multi_str).lower().strip() == "true" else False

                try:
                    self.__persistenceMappings = self.read_property("PERSISTENCE_MAPPING")
                except ParameterNotFoundException as ex:
                    self.log.debug("Cannot read persistence mapping : %r" % ex.get_message())
                    self.__persistenceMappings = None

                try:
                    is_commit_str = self.read_property(cartridgeagentconstants.COMMIT_ENABLED)
                    self.__isCommitsEnabled = True if str(is_commit_str).lower().strip() == "true" else False
                except ParameterNotFoundException:
                    try:
                        is_commit_str = self.read_property(cartridgeagentconstants.AUTO_COMMIT)
                        self.__isCommitsEnabled = True if str(is_commit_str).lower().strip() == "true" else False
                    except ParameterNotFoundException:
                        self.log.info("%r is not found and setting it to false" % cartridgeagentconstants.COMMIT_ENABLED)
                        self.__isCommitsEnabled = False

                auto_checkout_str = self.read_property(cartridgeagentconstants.AUTO_CHECKOUT)
                self.__isCheckoutEnabled = True if str(auto_checkout_str).lower().strip() == "true" else False

                self.__listenAddress = self.read_property(cartridgeagentconstants.LISTEN_ADDRESS)

                try:
                    int_repo_str = self.read_property(cartridgeagentconstants.PROVIDER)
                    self.__isInternalRepo = True if str(int_repo_str).strip().lower() == cartridgeagentconstants.INTERNAL else False
                except ParameterNotFoundException:
                    self.log.info(" INTERNAL payload parameter is not found")
                    self.__isInternalRepo = False

                self.__tenantId = self.read_property(cartridgeagentconstants.TENANT_ID)
                self.__lbClusterId = self.read_property(cartridgeagentconstants.LB_CLUSTER_ID)
                self.__minCount = self.read_property(cartridgeagentconstants.MIN_INSTANCE_COUNT)
                self.__lbPrivateIp = self.read_property(cartridgeagentconstants.LB_PRIVATE_IP)
                self.__lbPublicIp = self.read_property(cartridgeagentconstants.LB_PUBLIC_IP)
                self.__tenantRepositoryPath = self.read_property(cartridgeagentconstants.TENANT_REPO_PATH)
                self.__superTenantRepositoryPath = self.read_property(cartridgeagentconstants.SUPER_TENANT_REPO_PATH)

                try:
                    self.__deployment = self.read_property(cartridgeagentconstants.DEPLOYMENT)
                except ParameterNotFoundException:
                    self.__deployment = None

                # Setting worker-manager setup - manager service name
                if self.__deployment is None:
                    self.__managerServiceName = None

                if str(self.__deployment).lower() == cartridgeagentconstants.DEPLOYMENT_MANAGER.lower():
                    self.__managerServiceName = self.__service_name

                elif str(self.__deployment).lower() == cartridgeagentconstants.DEPLOYMENT_WORKER.lower():
                    self.__deployment = self.read_property(cartridgeagentconstants.MANAGER_SERVICE_TYPE)

                elif str(self.__deployment).lower() == cartridgeagentconstants.DEPLOYMENT_DEFAULT.lower():
                    self.__deployment = None
                else:
                    self.__deployment = None

                # Setting worker-manager setup - worker service name
                if self.__deployment is None:
                    self.__workerServiceName = None

                if str(self.__deployment).lower() == cartridgeagentconstants.DEPLOYMENT_WORKER.lower():
                    self.__managerServiceName = self.__service_name

                elif str(self.__deployment).lower() == cartridgeagentconstants.DEPLOYMENT_MANAGER.lower():
                    self.__deployment = self.read_property(cartridgeagentconstants.WORKER_SERVICE_TYPE)

                elif str(self.__deployment).lower() == cartridgeagentconstants.DEPLOYMENT_DEFAULT.lower():
                    self.__deployment = None
                else:
                    self.__deployment = None

                try:
                    self.__isPrimary = self.read_property(cartridgeagentconstants.CLUSTERING_PRIMARY_KEY)
                except ParameterNotFoundException:
                    self.__isPrimary = None
            except ParameterNotFoundException as ex:
                raise RuntimeError(ex)

            self.log.info("Cartridge agent configuration initialized")

            self.log.debug("service-name: %r" % self.__service_name)
            self.log.debug("cluster-id: %r" % self.__cluster_id)
            self.log.debug("network-partition-id: %r" % self.__network_partition_id)
            self.log.debug("partition-id: %r" % self.__partitionId)
            self.log.debug("member-id: %r" % self.__memberId)
            self.log.debug("cartridge-key: %r" % self.__cartridgeKey)
            self.log.debug("app-path: %r" % self.__appPath)
            self.log.debug("repo-url: %r" % self.__repoUrl)
            self.log.debug("ports: %r" % str(self.__ports))
            self.log.debug("lb-private-ip: %r" % self.__lbPrivateIp)
            self.log.debug("lb-public-ip: %r" % self.__lbPublicIp)

        def __read_conf_file(self):
            """
            Reads and stores the agent's configuration file
            :return:
            """

            self.properties = ConfigParser.SafeConfigParser()
            self.properties.read('agent.conf')

        def __read_parameter_file(self):
            """
            Reads the payload file of the cartridge and stores the values in a dictionary
            :return:
            """

            param_file = self.read_property(cartridgeagentconstants.PARAM_FILE_PATH)

            try:
                if param_file is not None:
                    metadata_file = open(param_file)
                    metadata_payload_content = metadata_file.read()
                    self.payload_params = dict(param.split("=") for param in metadata_payload_content.split(","))
                    metadata_file.close()
                else:
                    self.log.error("File not found: %r" % param_file)
            except:
                self.log.error("Could not read launch parameter file, hence trying to read from System properties.")

        def read_property(self, property_key):

            if self.properties.has_option("agent", property_key):
                temp_str = self.properties.get("agent", property_key)
                if temp_str != "" and temp_str is not None:
                    return temp_str

            if self.payload_params.has_key(property_key):
                temp_str = self.payload_params[property_key]
                if temp_str != "" and temp_str is not None:
                    return temp_str

            raise ParameterNotFoundException("Cannot find the value of required parameter: %r" % property_key)

        def get_service_name(self):
            return self.__service_name

        def get_cluster_id(self):
            return self.__cluster_id

        def get_network_partition_id(self):
            return self.__network_partition_id

        def get_partition_id(self):
            return self.__partitionId

        def get_member_id(self):
            return self.__memberId

        def get_cartridge_key(self):
            return self.__cartridgeKey

        def get_app_path(self):
            return self.__appPath

        def get_repo_url(self):
            return self.__repoUrl

        def get_ports(self):
            return self.__ports

        def get_log_file_paths(self):
            return self.__logFilePaths

        def is_multitenant(self):
            return self.__isMultitenant

        def get_persistance_mappings(self):
            return self.__persistenceMappings

        def is_commits_enabled(self):
            return self.__isCommitsEnabled

        def get_listen_address(self):
            return self.__listenAddress

        def is_internal_repo(self):
            return self.__isInternalRepo

        def get_tenant_id(self):
            return self.__tenantId

        def get_lb_cluster_id(self):
            return self.__lbClusterId

        def get_service_group(self):
            return self.__service_group

        def is_clustered(self):
            return self.__isClustered

        def get_min_count(self):
            return self.__minCount

        def is_primary(self):
            return self.__isPrimary

        def get_lb_public_ip(self):
            return self.__lbPublicIp

        def set_lb_public_ip(self, ip):
            self.__lbPublicIp = ip

        def get_lb_private(self):
            return self.__lbPrivateIp

        def set_lb_privateip(self, ip):
            self.__lbPrivateIp = ip

        def get_deployment(self):
            return self.__deployment

        def set_deployment(self, dep):
            self.__deployment = dep

        def get_manager_service_name(self):
            return self.__managerServiceName

        def set_manager_service_name(self, mgr):
            self.__managerServiceName = mgr

        def get_worker_service_name(self):
            return self.__workerServiceName

        def set_worker_service_name(self, wrkr):
            self.__workerServiceName = wrkr

        def get_super_tenant_repo_path(self):
            return self.__superTenantRepositoryPath

        def get_tenant_repo_path(self):
            return self.__tenantRepositoryPath

        def is_checkout_enabled(self):
            return self.__isCheckoutEnabled

    instance = None

    def __init__(self):
        if not CartridgeAgentConfiguration.instance:
            CartridgeAgentConfiguration.instance = CartridgeAgentConfiguration.__CartridgeAgentConfiguration()

    def __getattr__(self, item):
        return getattr(self.instance, item)


