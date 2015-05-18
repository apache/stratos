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

import datetime
from threading import Thread, current_thread

from ..databridge.agent import *
from config import CartridgeAgentConfiguration
from ..util import cartridgeagentutils
from exception import DataPublisherException
import constants


class LogPublisher(Thread):

    def __init__(self, file_path, stream_definition, tenant_id, alias, date_time, member_id):
        Thread.__init__(self)

        self.log = LogFactory().get_log(__name__)

        self.file_path = file_path
        self.thrift_publisher = ThriftPublisher(
            DataPublisherConfiguration.get_instance().monitoring_server_ip,
            DataPublisherConfiguration.get_instance().monitoring_server_secure_port,
            DataPublisherConfiguration.get_instance().admin_username,
            DataPublisherConfiguration.get_instance().admin_password,
            stream_definition)
        self.tenant_id = tenant_id
        self.alias = alias
        self.date_time = date_time
        self.member_id = member_id

        self.terminated = False

    def run(self):
        if os.path.isfile(self.file_path) and os.access(self.file_path, os.R_OK):
            self.log.info("Starting log publisher for file: " + self.file_path + ", thread: " + str(current_thread()))
            # open file and keep reading for new entries
            # with open(self.file_path, "r") as read_file:
            read_file = open(self.file_path, "r")
            read_file.seek(os.stat(self.file_path)[6])  # go to the end of the file

            while not self.terminated:
                where = read_file.tell()  # where the seeker is in the file
                line = read_file.readline()   # read the current line
                if not line:
                    # no new line entered
                    self.log.debug("No new log entries detected to publish.")
                    time.sleep(1)
                    read_file.seek(where)  # set seeker
                else:
                    # new line detected, create event object
                    self.log.debug("Log entry/entries detected. Publishing to monitoring server.")
                    event = ThriftEvent()
                    event.metaData.append(self.member_id)
                    event.payloadData.append(self.tenant_id)
                    event.payloadData.append(self.alias)
                    event.payloadData.append("")
                    event.payloadData.append(self.date_time)
                    event.payloadData.append("")
                    event.payloadData.append(line)
                    event.payloadData.append("")
                    event.payloadData.append("")
                    event.payloadData.append(self.member_id)
                    event.payloadData.append("")

                    self.thrift_publisher.publish(event)
                    self.log.debug("Log event published.")

            self.thrift_publisher.disconnect()  # disconnect the publisher upon being terminated
            self.log.debug("Log publisher for path \"%s\" terminated" % self.file_path)
        else:
            raise DataPublisherException("Unable to read the file at path \"%s\"" % self.file_path)

    def terminate(self):
        """
        Allows the LogPublisher thread to be terminated to stop publishing to BAM/CEP. Allow a minimum of 1 second delay
        to take effect.
        """
        self.terminated = True


class LogPublisherManager(Thread):
    """
    A log publishing thread management thread which maintains a log publisher for each log file. Also defines a stream
    definition and the BAM/CEP server information for a single publishing context.
    """

    @staticmethod
    def define_stream(tenant_id, alias, date_time):
        """
        Creates a stream definition for Log Publishing
        :return: A StreamDefinition object with the required attributes added
        :rtype : StreamDefinition
        """
        # stream definition
        stream_definition = StreamDefinition()
        stream_name = "logs." + tenant_id + "." \
                      + alias + "." + date_time
        stream_version = "1.0.0"
        stream_nickname = "log entries from instance"
        stream_description = "Apache Stratos Instance Log Publisher"

        stream_definition.name = stream_name
        stream_definition.version = stream_version
        stream_definition.description = stream_description
        stream_definition.nickname = stream_nickname
        stream_definition.add_metadata_attribute("memberId", StreamDefinition.STRING)
        stream_definition.add_payloaddata_attribute("tenantID", StreamDefinition.STRING)
        stream_definition.add_payloaddata_attribute("serverName", StreamDefinition.STRING)
        stream_definition.add_payloaddata_attribute("appName", StreamDefinition.STRING)
        stream_definition.add_payloaddata_attribute("logTime", StreamDefinition.STRING)
        stream_definition.add_payloaddata_attribute("priority", StreamDefinition.STRING)
        stream_definition.add_payloaddata_attribute("message", StreamDefinition.STRING)
        stream_definition.add_payloaddata_attribute("logger", StreamDefinition.STRING)
        stream_definition.add_payloaddata_attribute("ip", StreamDefinition.STRING)
        stream_definition.add_payloaddata_attribute("instance", StreamDefinition.STRING)
        stream_definition.add_payloaddata_attribute("stacktrace", StreamDefinition.STRING)

        return stream_definition

    def __init__(self, logfile_paths):
        Thread.__init__(self)

        self.log = LogFactory().get_log(__name__)

        self.logfile_paths = logfile_paths
        self.publishers = {}
        self.ports = []
        self.ports.append(DataPublisherConfiguration.get_instance().monitoring_server_port)
        self.ports.append(DataPublisherConfiguration.get_instance().monitoring_server_secure_port)

        self.cartridge_agent_config = CartridgeAgentConfiguration()

        self.log.debug("Checking if Monitoring server is active.")
        ports_active = cartridgeagentutils.wait_until_ports_active(
            DataPublisherConfiguration.get_instance().monitoring_server_ip,
            self.ports,
            int(self.cartridge_agent_config.read_property("port.check.timeout", critical=False)))

        if not ports_active:
            self.log.debug("Monitoring server is not active")
            raise DataPublisherException("Monitoring server not active, data publishing is aborted")

        self.log.debug("Monitoring server is up and running. Log Publisher Manager started.")

        self.tenant_id = LogPublisherManager.get_valid_tenant_id(CartridgeAgentConfiguration().tenant_id)
        self.alias = LogPublisherManager.get_alias(CartridgeAgentConfiguration().cluster_id)
        self.date_time = LogPublisherManager.get_current_date()

        self.stream_definition = self.define_stream(self.tenant_id, self.alias, self.date_time)

    def run(self):
        if self.logfile_paths is not None and len(self.logfile_paths):
            for log_path in self.logfile_paths:
                # thread for each log file
                publisher = self.get_publisher(log_path)
                publisher.start()
                self.log.debug("Log publisher for path \"%s\" started." % log_path)

    def get_publisher(self, log_path):
        """
        Retrieve the publisher for the specified log file path. Creates a new LogPublisher if one is not available
        :return: The LogPublisher object
        :rtype : LogPublisher
        """
        if log_path not in self.publishers:
            self.log.debug("Creating a Log publisher for path \"%s\"" % log_path)
            self.publishers[log_path] = LogPublisher(
                log_path,
                self.stream_definition,
                self.tenant_id,
                self.alias,
                self.date_time,
                self.cartridge_agent_config.member_id)

        return self.publishers[log_path]

    def terminate_publisher(self, log_path):
        """
        Terminates the LogPublisher thread associated with the specified log file
        """
        if log_path in self.publishers:
            self.publishers[log_path].terminate()

    def terminate_all_publishers(self):
        """
        Terminates all LogPublisher threads
        """
        for publisher in self.publishers:
            publisher.terminate()

    @staticmethod
    def get_valid_tenant_id(tenant_id):
        if tenant_id == constants.INVALID_TENANT_ID or tenant_id == constants.SUPER_TENANT_ID:
            return "0"

        return tenant_id

    @staticmethod
    def get_alias(cluster_id):
        try:
            alias = cluster_id.split("\\.")[0]
        except:
            alias = cluster_id

        return alias

    @staticmethod
    def get_current_date():
        """
        Returns the current date formatted as yyyy-MM-dd
        :return: Formatted date string
        :rtype : str
        """
        return datetime.date.today().strftime(constants.DATE_FORMAT)


class DataPublisherConfiguration:
    """
    A singleton implementation to access configuration information for data publishing to BAM/CEP
    TODO: perfect singleton impl ex: Borg
    """

    __instance = None
    log = LogFactory().get_log(__name__)

    @staticmethod
    def get_instance():
        """
        Singleton instance retriever
        :return: Instance
        :rtype : DataPublisherConfiguration
        """
        if DataPublisherConfiguration.__instance is None:
            DataPublisherConfiguration.__instance = DataPublisherConfiguration()

        return DataPublisherConfiguration.__instance

    def __init__(self):
        self.enabled = False
        self.monitoring_server_ip = None
        self.monitoring_server_port = None
        self.monitoring_server_secure_port = None
        self.admin_username = None
        self.admin_password = None
        self.cartridge_agent_config = CartridgeAgentConfiguration()

        self.read_config()

    def read_config(self):
        self.enabled = True if \
            self.cartridge_agent_config.read_property(constants.MONITORING_PUBLISHER_ENABLED, False).strip().lower() \
            == "true" \
            else False

        if not self.enabled:
            DataPublisherConfiguration.log.info("Data Publisher disabled")
            return

        DataPublisherConfiguration.log.info("Data Publisher enabled")

        self.monitoring_server_ip = self.cartridge_agent_config.read_property(constants.MONITORING_RECEIVER_IP, False)
        if self.monitoring_server_ip is None or self.monitoring_server_ip.strip() == "":
            raise RuntimeError("System property not found: " + constants.MONITORING_RECEIVER_IP)

        self.monitoring_server_port = self.cartridge_agent_config.read_property(
            constants.MONITORING_RECEIVER_PORT,
            False)

        if self.monitoring_server_port is None or self.monitoring_server_port.strip() == "":
            raise RuntimeError("System property not found: " + constants.MONITORING_RECEIVER_PORT)

        self.monitoring_server_secure_port = self.cartridge_agent_config.read_property(
            "monitoring.server.secure.port",
            False)

        if self.monitoring_server_secure_port is None or self.monitoring_server_secure_port.strip() == "":
            raise RuntimeError("System property not found: monitoring.server.secure.port")

        self.admin_username = self.cartridge_agent_config.read_property(
            constants.MONITORING_SERVER_ADMIN_USERNAME,
            False)

        if self.admin_username is None or self.admin_username.strip() == "":
            raise RuntimeError("System property not found: " + constants.MONITORING_SERVER_ADMIN_USERNAME)

        self.admin_password = self.cartridge_agent_config.read_property(
            constants.MONITORING_SERVER_ADMIN_PASSWORD,
            False)

        if self.admin_password is None or self.admin_password.strip() == "":
            raise RuntimeError("System property not found: " + constants.MONITORING_SERVER_ADMIN_PASSWORD)

        DataPublisherConfiguration.log.info("Data Publisher configuration initialized")
