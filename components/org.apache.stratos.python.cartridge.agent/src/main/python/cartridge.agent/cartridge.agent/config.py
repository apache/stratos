# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import ConfigParser
import os
import sys

from yapsy.PluginManager import PluginManager

from modules.util.log import LogFactory
from exception import ParameterNotFoundException, InvalidConfigValueException
import constants
from plugins.contracts import ICartridgeAgentPlugin, IArtifactCommitPlugin, IArtifactCheckoutPlugin, \
    IHealthStatReaderPlugin


class Config:
    """
    Handles the configuration information of the particular Cartridge Agent
    """

    def __init__(self):
        pass

    AGENT_PLUGIN_EXT = "agent-plugin"
    ARTIFACT_CHECKOUT_PLUGIN = "ArtifactCheckoutPlugin"
    ARTIFACT_COMMIT_PLUGIN = "ArtifactCommitPlugin"
    CARTRIDGE_AGENT_PLUGIN = "CartridgeAgentPlugin"
    HEALTH_STAT_PLUGIN = "HealthStatReaderPlugin"

    # set log level
    log = LogFactory().get_log(__name__)

    payload_params = {}
    properties = None
    """ :type : ConfigParser.SafeConfigParser """

    plugins = {}
    """ :type dict{str: [PluginInfo]} : """
    artifact_checkout_plugin = None
    artifact_commit_plugin = None
    health_stat_plugin = None
    extension_executor = None

    application_id = None
    """ :type : str """
    service_group = None
    """ :type : str  """
    is_clustered = False
    """ :type : bool  """
    service_name = None
    """ :type : str  """
    cluster_id = None
    """ :type : str  """
    cluster_instance_id = None
    """ :type : str  """
    member_id = None
    """ :type : str  """
    instance_id = None
    """ :type : str  """
    network_partition_id = None
    """ :type : str  """
    partition_id = None
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
    is_multiTenant = False
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
    dependant_cluster_id = None
    """ :type : str  """
    export_metadata_keys = None
    """ :type : str  """
    import_metadata_keys = None
    """ :type : str  """
    is_primary = False
    """ :type : bool  """
    artifact_update_interval = None
    """ :type : str """
    lvs_virtual_ip = None
    """ :type : str """
    initialized = False
    """ :type : bool """
    activated = False
    """ :type : bool """
    started = False
    """ :type : bool """
    ready_to_shutdown = False
    """ :type : bool """
    maintenance = False
    """ :type : bool """
    mb_urls = []
    """ :type : list """
    mb_username = None
    """ :type : str """
    mb_password = None
    """ :type : str """
    mb_publisher_timeout = None
    """ :type : int """
    cep_username = None
    """ :type : str """
    cep_password = None
    """ :type : str """
    cep_urls = []
    """ :type : list """
    artifact_clone_retry_count = None
    """ :type : str """
    artifact_clone_retry_interval = None
    """ :type : str """
    port_check_timeout = None
    """ :type : str """

    @staticmethod
    def read_conf_file():
        """
        Reads and stores the agent's configuration file
        :return: properties object
        :rtype: ConfigParser.SafeConfigParser()
        """

        conf_file_path = os.path.abspath(os.path.dirname(__file__)) + "/agent.conf"
        Config.log.debug("Config file path : %r" % conf_file_path)

        properties = ConfigParser.SafeConfigParser()
        properties.read(conf_file_path)

        # set calculated values
        param_file = os.path.abspath(os.path.dirname(__file__)) + "/payload/launch-params"
        Config.log.debug("param_file: %r" % param_file)
        properties.set("agent", constants.PARAM_FILE_PATH, param_file)
        plugins_dir = os.path.abspath(os.path.dirname(__file__)) + "/plugins"
        Config.log.debug("plugins_dir: %r" % plugins_dir)
        properties.set("agent", constants.PLUGINS_DIR, plugins_dir)
        plugins_dir = os.path.abspath(os.path.dirname(__file__)) + "/extensions/py"
        properties.set("agent", constants.EXTENSIONS_DIR, plugins_dir)

        return properties

    @staticmethod
    def read_payload_file(param_file_path):
        """
        Reads the payload file of the cartridge and stores the values in a dictionary
        :param param_file_path: payload parameter file path
        :return: Payload parameter dictionary of values
        :rtype: dict
        """
        Config.log.debug("Param file path : %r" % param_file_path)

        try:
            payload_params = {}
            if param_file_path is not None:
                param_file = open(param_file_path)
                payload_content = param_file.read()
                for param in payload_content.split(","):
                    if param.strip() != "":
                        param_value = param.strip().split("=")
                        try:
                            if str(param_value[1]).strip().lower() == "null" or str(param_value[1]).strip() == "":
                                payload_params[param_value[0]] = None
                            else:
                                payload_params[param_value[0]] = param_value[1]
                        except IndexError:
                            # If an index error comes when reading values, keep on reading
                            pass

                param_file.close()
                return payload_params
            else:
                raise RuntimeError("Payload parameter file not found: %r" % param_file_path)
        except Exception as e:
            Config.log.exception("Could not read payload parameter file: %s" % e)

    @staticmethod
    def convert_to_type(value_string):
        """
        Determine what type of data to return from the provided string
        :param value_string:
        :return:
        """
        if value_string is None:
            return None

        value_string = str(value_string).strip()

        if value_string == "" or value_string.lower() == "null":
            # converted as a null value
            return None

        if value_string.lower() == "true":
            # boolean TRUE
            return True

        if value_string.lower() == "false":
            # boolean FALSE
            return False
        #
        # value_split = value_string.split("|")
        # if len(value_split) > 1:
        #     # can be split using the delimiter, array returned
        #     return value_split

        return value_string

    @staticmethod
    def read_property(property_key, critical=True):
        """
        Returns the value of the provided property
        :param critical: If absence of this value should throw an error
        :param str property_key: the name of the property to be read
        :return: Value of the property
        :exception: ParameterNotFoundException if the provided property cannot be found
        """
        if Config.properties.has_option("agent", property_key):
            temp_str = Config.properties.get("agent", property_key)
            Config.log.debug("Reading property: %s = %s", property_key, temp_str)
            real_value = Config.convert_to_type(temp_str)
            if real_value is not None:
                return real_value

        if property_key in Config.payload_params:
            temp_str = Config.payload_params[property_key]
            Config.log.debug("Reading payload parameter: %s = %s", property_key, temp_str)
            real_value = Config.convert_to_type(temp_str)
            if real_value is not None:
                return real_value

        # real value is None
        if critical:
            raise ParameterNotFoundException("Cannot find the value of required parameter: %r" % property_key)
        else:
            return None

    @staticmethod
    def get_payload_params():
        return Config.payload_params

    @staticmethod
    def initialize_config():
        """
        Read the two inputs and load values to fields
        :return: void
        """
        Config.properties = Config.read_conf_file()
        param_file_path = Config.properties.get("agent", constants.PARAM_FILE_PATH)
        Config.payload_params = Config.read_payload_file(param_file_path)

        try:
            Config.application_id = Config.read_property(constants.APPLICATION_ID)
            Config.service_name = Config.read_property(constants.SERVICE_NAME)
            Config.cluster_id = Config.read_property(constants.CLUSTER_ID)
            Config.ports = Config.read_property(constants.PORTS).replace("'", "").split("|")
            Config.is_multiTenant = Config.read_property(constants.MULTITENANT)
            Config.tenant_id = Config.read_property(constants.TENANT_ID)

            try:
                Config.is_clustered = Config.read_property(constants.CLUSTERING, False)
            except ParameterNotFoundException:
                Config.is_clustered = False

            try:
                Config.is_commits_enabled = Config.read_property(constants.COMMIT_ENABLED, False)
            except ParameterNotFoundException:
                try:
                    Config.is_commits_enabled = Config.read_property(constants.AUTO_COMMIT, False)
                except ParameterNotFoundException:
                    Config.is_commits_enabled = False

            try:
                Config.is_internal_repo = Config.read_property(constants.INTERNAL)
            except ParameterNotFoundException:
                Config.is_internal_repo = False

            try:
                Config.artifact_update_interval = Config.read_property(constants.ARTIFACT_UPDATE_INTERVAL)
            except ParameterNotFoundException:
                Config.artifact_update_interval = 10

            Config.service_group = Config.read_property(constants.SERVICE_GROUP, False)
            Config.cluster_instance_id = Config.read_property(constants.CLUSTER_INSTANCE_ID, False)
            Config.member_id = Config.read_property(constants.MEMBER_ID, False)
            Config.network_partition_id = Config.read_property(constants.NETWORK_PARTITION_ID, False)
            Config.partition_id = Config.read_property(constants.PARTITION_ID, False)
            Config.app_path = Config.read_property(constants.APPLICATION_PATH, False)
            Config.repo_url = Config.read_property(constants.REPO_URL, False)

            if Config.repo_url is not None:
                Config.cartridge_key = Config.read_property(constants.CARTRIDGE_KEY)
            else:
                Config.cartridge_key = Config.read_property(constants.CARTRIDGE_KEY, False)

            Config.dependant_cluster_id = Config.read_property(constants.DEPENDENCY_CLUSTER_IDS, False)
            Config.export_metadata_keys = Config.read_property(constants.EXPORT_METADATA_KEYS, False)
            Config.import_metadata_keys = Config.read_property(constants.IMPORT_METADATA_KEYS, False)
            Config.lvs_virtual_ip = Config.read_property(constants.LVS_VIRTUAL_IP, False)
            try:
                Config.log_file_paths = Config.read_property(constants.LOG_FILE_PATHS).split("|")
            except ParameterNotFoundException:
                Config.log_file_paths = None

            Config.persistence_mappings = Config.read_property(constants.PERSISTENCE_MAPPING, False)

            Config.is_checkout_enabled = Config.read_property(constants.AUTO_CHECKOUT, False)
            Config.listen_address = Config.read_property(constants.LISTEN_ADDRESS, False)
            Config.lb_cluster_id = Config.read_property(constants.LB_CLUSTER_ID, False)
            Config.min_count = Config.read_property(constants.MIN_INSTANCE_COUNT, False)
            Config.lb_private_ip = Config.read_property(constants.LB_PRIVATE_IP, False)
            Config.lb_public_ip = Config.read_property(constants.LB_PUBLIC_IP, False)
            Config.tenant_repository_path = Config.read_property(constants.TENANT_REPO_PATH, False)
            Config.super_tenant_repository_path = Config.read_property(constants.SUPER_TENANT_REPO_PATH, False)

            Config.is_primary = Config.read_property(constants.CLUSTERING_PRIMARY_KEY, False)

            Config.mb_username = Config.read_property(constants.MB_USERNAME, False)
            Config.mb_password = Config.read_property(constants.MB_PASSWORD, False)
            Config.mb_urls = Config.read_property(constants.MB_URLS)
            Config.mb_publisher_timeout = int(Config.read_property(constants.MB_PUBLISHER_TIMEOUT))

            Config.cep_username = Config.read_property(constants.CEP_SERVER_ADMIN_USERNAME)
            Config.cep_password = Config.read_property(constants.CEP_SERVER_ADMIN_PASSWORD)
            Config.cep_urls = Config.read_property(constants.CEP_RECEIVER_URLS)

            try:
                Config.artifact_clone_retry_count = Config.read_property(constants.ARTIFACT_CLONE_RETRIES)
            except ParameterNotFoundException:
                Config.artifact_clone_retry_count = "5"

            try:
                Config.artifact_clone_retry_interval = Config.read_property(constants.ARTIFACT_CLONE_INTERVAL)
            except ParameterNotFoundException:
                Config.artifact_clone_retry_interval = "10"

            try:
                Config.port_check_timeout = Config.read_property(constants.PORT_CHECK_TIMEOUT)
            except ParameterNotFoundException:
                Config.port_check_timeout = "600000"

            Config.validate_config()
        except ParameterNotFoundException as ex:
            raise RuntimeError(ex)

        Config.log.info("Cartridge agent configuration initialized")
        Config.log.debug("service-name: %r" % Config.service_name)
        Config.log.debug("cluster-id: %r" % Config.cluster_id)
        Config.log.debug("cluster-instance-id: %r" % Config.cluster_instance_id)
        Config.log.debug("member-id: %r" % Config.member_id)
        Config.log.debug("network-partition-id: %r" % Config.network_partition_id)
        Config.log.debug("partition-id: %r" % Config.partition_id)
        Config.log.debug("cartridge-key: %r" % Config.cartridge_key)
        Config.log.debug("app-path: %r" % Config.app_path)
        Config.log.debug("repo-url: %r" % Config.repo_url)
        Config.log.debug("ports: %r" % str(Config.ports))
        Config.log.debug("lb-private-ip: %r" % Config.lb_private_ip)
        Config.log.debug("lb-public-ip: %r" % Config.lb_public_ip)
        Config.log.debug("dependant_cluster_id: %r" % Config.dependant_cluster_id)
        Config.log.debug("export_metadata_keys: %r" % Config.export_metadata_keys)
        Config.log.debug("import_metadata_keys: %r" % Config.import_metadata_keys)
        Config.log.debug("artifact.update.interval: %r" % Config.artifact_update_interval)
        Config.log.debug("lvs-virtual-ip: %r" % Config.lvs_virtual_ip)
        Config.log.debug("log_file_paths: %s" % Config.log_file_paths)

        Config.log.info("Initializing plugins")
        Config.initialize_plugins()
        Config.extension_executor = Config.initialize_extensions()

    @staticmethod
    def validate_config():
        try:
            Config.validate_url_list(Config.mb_urls, constants.MB_URLS)
            Config.validate_int(Config.mb_publisher_timeout, constants.MB_PUBLISHER_TIMEOUT)
            Config.validate_url_list(Config.cep_urls, constants.CEP_RECEIVER_URLS)
            Config.validate_int(Config.artifact_update_interval, constants.ARTIFACT_UPDATE_INTERVAL)
            Config.validate_int(Config.artifact_clone_retry_count, constants.ARTIFACT_CLONE_RETRIES)
            Config.validate_int(Config.artifact_clone_retry_interval, constants.ARTIFACT_CLONE_INTERVAL)
            Config.validate_int(Config.port_check_timeout, constants.PORT_CHECK_TIMEOUT)
        except ValueError as err:
            raise InvalidConfigValueException("Invalid configuration for Cartridge Agent", err)

    @staticmethod
    def validate_url_list(urls, field_name):
        """
        host1:port1,host2:port2

        :param urls:
        :param field_name:
        :return:
        """
        url_list = str(urls).split(",")
        if len(url_list) < 1:
            raise ValueError("Invalid value [field] \"%s\"" % field_name)

        for single_url in url_list:
            try:
                url_ip, url_port = single_url.split(":")
            except ValueError:
                raise ValueError("Invalid host or port number value for [field] %s", field_name)

    @staticmethod
    def validate_int(int_value, field_name):
        """
        valid integer value

        :param int_value:
        :param field_name:
        :return:
        """
        try:
            int(int_value)
        except ValueError:
            raise ValueError("Invalid int value for [field] %s " % field_name)

    @staticmethod
    def initialize_plugins():
        """ Find, load, activate and group plugins for Python CA
        :return: a tuple of (PluginManager, plugins, artifact management plugins)
        """
        Config.log.info("Collecting and loading plugins")

        try:
            # TODO: change plugin descriptor ext, plugin_manager.setPluginInfoExtension(AGENT_PLUGIN_EXT)
            plugins_dir = Config.read_property(constants.PLUGINS_DIR)
            category_filter = {Config.CARTRIDGE_AGENT_PLUGIN: ICartridgeAgentPlugin,
                               Config.ARTIFACT_CHECKOUT_PLUGIN: IArtifactCheckoutPlugin,
                               Config.ARTIFACT_COMMIT_PLUGIN: IArtifactCommitPlugin,
                               Config.HEALTH_STAT_PLUGIN: IHealthStatReaderPlugin}

            plugin_manager = Config.create_plugin_manager(category_filter, plugins_dir)

            # activate cartridge agent plugins
            plugins = plugin_manager.getPluginsOfCategory(Config.CARTRIDGE_AGENT_PLUGIN)
            grouped_ca_plugins = {}
            for plugin_info in plugins:
                Config.log.debug("Found plugin [%s] at [%s]" % (plugin_info.name, plugin_info.path))
                plugin_manager.activatePluginByName(plugin_info.name)
                Config.log.info("Activated plugin [%s]" % plugin_info.name)

                mapped_events = plugin_info.description.split(",")
                for mapped_event in mapped_events:
                    if mapped_event.strip() != "":
                        if grouped_ca_plugins.get(mapped_event) is None:
                            grouped_ca_plugins[mapped_event] = []

                        grouped_ca_plugins[mapped_event].append(plugin_info)
            Config.plugins = grouped_ca_plugins

            # activate artifact management plugins
            artifact_checkout_plugins = plugin_manager.getPluginsOfCategory(Config.ARTIFACT_CHECKOUT_PLUGIN)
            for plugin_info in artifact_checkout_plugins:
                Config.log.debug("Found artifact checkout plugin [%s] at [%s]" % (plugin_info.name, plugin_info.path))
            # if multiple artifact management plugins are registered, halt agent execution. This is to avoid any
            # undesired outcome due to errors made in deployment
            if Config.is_checkout_enabled:
                if len(artifact_checkout_plugins) == 0:
                    Config.log.exception(
                        "No plugins registered for artifact checkout extension. Stratos agent failed to start")
                    sys.exit(1)
                elif len(artifact_checkout_plugins) == 1:
                    plugin_info = artifact_checkout_plugins[0]
                    Config.log.debug("Found artifact checkout plugin [%s] at [%s]" %
                                     (plugin_info.name, plugin_info.path))
                    plugin_manager.activatePluginByName(plugin_info.name)
                    Config.log.info("Activated artifact checkout plugin [%s]" % plugin_info.name)
                    Config.artifact_checkout_plugin = plugin_info
                elif len(artifact_checkout_plugins) > 1:
                    Config.log.exception(
                        "Multiple plugins registered for artifact checkout. Stratos agent failed to start.")
                    sys.exit(1)

            artifact_commit_plugins = plugin_manager.getPluginsOfCategory(Config.ARTIFACT_COMMIT_PLUGIN)
            for plugin_info in artifact_commit_plugins:
                Config.log.debug("Found artifact commit plugin [%s] at [%s]" % (plugin_info.name, plugin_info.path))
            if Config.is_commits_enabled:
                if len(artifact_commit_plugins) == 0:
                    Config.log.exception(
                        "No plugins registered for artifact commit extension. Stratos agent failed to start")
                    sys.exit(1)
                elif len(artifact_commit_plugins) == 1:
                    plugin_info = artifact_commit_plugins[0]
                    Config.log.debug("Found artifact commit plugin [%s] at [%s]" %
                                     (plugin_info.name, plugin_info.path))
                    plugin_manager.activatePluginByName(plugin_info.name)
                    Config.log.info("Activated artifact commit plugin [%s]" % plugin_info.name)
                    Config.artifact_commit_plugin = plugin_info
                elif len(artifact_commit_plugins) > 1:
                    Config.log.exception(
                        "Multiple plugins registered for artifact checkout. Stratos agent failed to start.")
                    sys.exit(1)

            health_stat_plugins = plugin_manager.getPluginsOfCategory(Config.HEALTH_STAT_PLUGIN)
            for plugin_info in health_stat_plugins:
                Config.log.debug("Found health stats reader plugin [%s] at [%s]" % (plugin_info.name, plugin_info.path))
            # If multiple health stat reader plugins are registered, halt agent execution. This is to avoid any
            # undesired outcome due to errors made in deployment
            if len(health_stat_plugins) == 0:
                Config.log.exception(
                    "No plugins registered for health statistics reader. Stratos agent failed to start.")
                sys.exit(1)
            elif len(health_stat_plugins) == 1:
                plugin_info = health_stat_plugins[0]
                Config.log.debug("Found health statistics reader plugin [%s] at [%s]" %
                                 (plugin_info.name, plugin_info.path))
                plugin_manager.activatePluginByName(plugin_info.name)
                Config.log.info("Activated health statistics reader plugin [%s]" % plugin_info.name)
                Config.health_stat_plugin = plugin_info
            elif len(health_stat_plugins) > 1:
                Config.log.exception(
                    "Multiple plugins registered for health statistics reader. Stratos agent failed to start.")
                sys.exit(1)
        except ParameterNotFoundException as e:
            Config.log.exception("Could not load plugins. Plugins directory not set: %s" % e)
            Config.log.error("Stratos agent failed to start")
            sys.exit(1)
        except Exception as e:
            Config.log.exception("Error while loading plugins: %s" % e)
            Config.log.error("Stratos agent failed to start")
            sys.exit(1)

    @staticmethod
    def initialize_extensions():
        """ Find, load and activate extension scripts for Python CA. The extensions are mapped to the event by the
        name used in the plugin descriptor.
        :return:a tuple of (PluginManager, extensions)
        """
        Config.log.info("Collecting and loading extensions")

        try:
            extensions_dir = Config.read_property(constants.EXTENSIONS_DIR)
            category_filter = {Config.CARTRIDGE_AGENT_PLUGIN: ICartridgeAgentPlugin}

            extension_manager = Config.create_plugin_manager(category_filter, extensions_dir)

            all_extensions = extension_manager.getPluginsOfCategory(Config.CARTRIDGE_AGENT_PLUGIN)
            for plugin_info in all_extensions:
                try:
                    Config.log.debug("Found extension executor [%s] at [%s]" % (plugin_info.name, plugin_info.path))
                    extension_manager.activatePluginByName(plugin_info.name)
                    extension_executor = plugin_info
                    Config.log.info("Activated extension executor [%s]" % plugin_info.name)
                    # extension executor found. break loop and return
                    return extension_executor
                except Exception as ignored:
                    pass

            # no extension executor plugin could be loaded or activated
            raise RuntimeError("Couldn't activated any ExtensionExecutor plugin")
        except ParameterNotFoundException as e:
            Config.log.exception("Could not load extensions. Extensions directory not set: %s" % e)
            return None
        except Exception as e:
            Config.log.exception("Error while loading extension: %s" % e)
            return None

    @staticmethod
    def create_plugin_manager(category_filter, plugin_place):
        """ Creates a PluginManager object from the given folder according to the given filter
        :param category_filter:
        :param plugin_place:
        :return:
        :rtype: PluginManager
        """
        plugin_manager = PluginManager()
        plugin_manager.setCategoriesFilter(category_filter)
        plugin_manager.setPluginPlaces([plugin_place])

        plugin_manager.collectPlugins()

        return plugin_manager
