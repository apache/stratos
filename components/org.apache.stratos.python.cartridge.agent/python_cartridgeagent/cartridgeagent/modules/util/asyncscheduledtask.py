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

import time
from threading import Thread

class AbstractAsyncScheduledTask:
    """
    Exposes the contract to follow to implement a scheduled task to be executed by the ScheduledExecutor
    """

    def execute_task(self):
        """
        Override this method and implement the task to be executed by the ScheduledExecutor with a specified
        interval.
        """
        raise NotImplementedError


class ScheduledExecutor(Thread):
    """
    Executes a given task with a given interval until being terminated
    """

    def __init__(self, delay, task):
        """
        Creates a ScheduledExecutor thread to handle interval based repeated execution of a given task of type
        AbstractAsyncScheduledTask
        :param int delay: The interval to keep between executions
        :param AbstractAsyncScheduledTask task: The task to be implemented
        :return:
        """

        Thread.__init__(self)
        self.delay = delay
        """ :type : int  """
        self.task = task
        """ :type : AbstractAsyncScheduledTask  """
        self.terminated = False
        """ :type : bool  """

    def run(self):
        """
        Start the scheduled task with a sleep time of delay in between
        :return:
        """
        while not self.terminated:
            time.sleep(self.delay)
            task_thread = Thread(target=self.task.execute_task)
            task_thread.start()

    def terminate(self):
        """
        Terminate the scheduled task. Allow a maximum of 'delay' seconds to be terminated.
        :return: void
        """
        self.terminated = True