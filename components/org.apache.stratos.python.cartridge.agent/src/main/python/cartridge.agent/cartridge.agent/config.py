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

from modules.util.log import LogFactory
from exception import ParameterNotFoundException
import constants


class Config:
    """
    Handles the configuration information of the particular Cartridge Agent
    """

    # set log level
    log = LogFactory().get_log(__name__)

    payload_params = {}
    properties = None
    """ :type : ConfigParser.SafeConfigParser """

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

    @staticmethod
    def read_conf_file():
        """
        Reads and stores the agent's configuration file
        :return: properties object
        :rtype: ConfigParser.SafeConfigParser()
        """

        conf_file_path = os.path.abspath(os.path.dirname(__file__)).split("modules")[0] + "/agent.conf"
        Config.log.debug("Config file path : %r" % conf_file_path)

        properties = ConfigParser.SafeConfigParser()
        properties.read(conf_file_path)

        # set calculated values
        param_file = os.path.abspath(os.path.dirname(__file__)).split("modules")[0] + "/payload/launch-params"
        properties.set("agent", constants.PARAM_FILE_PATH, param_file)
        plugins_dir = os.path.abspath(os.path.dirname(__file__)).split("modules")[0] + "/plugins"
        properties.set("agent", constants.PLUGINS_DIR, plugins_dir)
        plugins_dir = os.path.abspath(os.path.dirname(__file__)).split("modules")[0] + "/extensions/py"
        properties.set("agent", constants.EXTENSIONS_DIR, plugins_dir)

        return properties

    @staticmethod
    def read_payload_file(param_file_path):
        """
        Reads the payload file of the cartridge and stores the values in a dictionary
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
        :param str property_key: the name of the property to be read
        :return: Value of the property
        :exception: ParameterNotFoundException if the provided property cannot be found
        """

        if Config.properties is None or Config.payload_params == {}:
            Config.initialize_config()

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
            Config.ports = Config.read_property(constants.PORTS).split("|")
            Config.is_multiTenant = Config.read_property(constants.MULTITENANT)
            Config.tenant_id = Config.read_property(constants.TENANT_ID)

            try:
                Config.is_clustered = Config.read_property(constants.CLUSTERING)
            except ParameterNotFoundException:
                Config.is_clustered = False

            try:
                Config.is_commits_enabled = Config.read_property(constants.COMMIT_ENABLED)
            except ParameterNotFoundException:
                try:
                    Config.is_commits_enabled = Config.read_property(constants.AUTO_COMMIT)
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
            # Config.deployment = Config.read_property(constants.DEPLOYMENT, False)
            #
            # # Setting worker-manager setup - manager service name
            # if Config.deployment is None:
            #     Config.manager_service_name = None
            #
            # if str(Config.deployment).lower() == constants.DEPLOYMENT_MANAGER.lower():
            #     Config.manager_service_name = Config.service_name
            # elif str(Config.deployment).lower() == constants.DEPLOYMENT_WORKER.lower():
            #     Config.deployment = Config.read_property(constants.MANAGER_SERVICE_TYPE)
            # elif str(Config.deployment).lower() == constants.DEPLOYMENT_DEFAULT.lower():
            #     Config.deployment = None
            # else:
            #     Config.deployment = None
            #
            # # Setting worker-manager setup - worker service name
            # if Config.deployment is None:
            #     Config.worker_service_name = None
            #
            # if str(Config.deployment).lower() == constants.DEPLOYMENT_WORKER.lower():
            #     Config.manager_service_name = Config.service_name
            # elif str(Config.deployment).lower() == constants.DEPLOYMENT_MANAGER.lower():
            #     Config.deployment = Config.read_property(constants.WORKER_SERVICE_TYPE)
            # elif str(Config.deployment).lower() == constants.DEPLOYMENT_DEFAULT.lower():
            #     Config.deployment = None
            # else:
            #     Config.deployment = None

            Config.is_primary = Config.read_property(constants.CLUSTERING_PRIMARY_KEY, False)

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
