# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
import os

from plugins.contracts import IHealthStatReaderPlugin
from modules.util.log import LogFactory
import multiprocessing
import psutil


class DefaultHealthStatisticsReader(IHealthStatReaderPlugin):
    """
    Default implementation for the health statistics reader
    """

    def __init__(self):
        super(DefaultHealthStatisticsReader, self).__init__()
        self.log = LogFactory().get_log(__name__)

    def stat_cartridge_health(self, ca_health_stat):
        ca_health_stat.memory_usage = DefaultHealthStatisticsReader.__read_mem_usage()
        ca_health_stat.load_avg = DefaultHealthStatisticsReader.__read_load_avg()

        self.log.debug("Memory read: %r, CPU read: %r" % (ca_health_stat.memory_usage, ca_health_stat.load_avg))
        return ca_health_stat

    @staticmethod
    def __read_mem_usage():
        return psutil.virtual_memory().percent

    @staticmethod
    def __read_load_avg():
        (one, five, fifteen) = os.getloadavg()
        cores = multiprocessing.cpu_count()

        return (one / cores) * 100
