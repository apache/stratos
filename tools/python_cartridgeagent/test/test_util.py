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

from ..cartridgeagent.modules.util.asyncscheduledtask import *
from ..cartridgeagent.modules.util import cartridgeagentutils
import time


def test_async_task():
    test_task = TestTask()
    astask = ScheduledExecutor(1, test_task)
    start_time = time.time() * 1000
    astask.start()
    time.sleep(2)
    astask.terminate()
    f = open("asynctest.txt", "r")
    end_time = float(f.read())
    assert (end_time - start_time) >= 1 * 1000, "Task was executed before specified delay"


class TestTask(AbstractAsyncScheduledTask):

    def execute_task(self):
        with open("asynctest.txt", "w") as f:
            f.seek(0)
            f.truncate()
            f.write("%1.4f" % (time.time()*1000))


def test_decrypt_password():
    # def mockgetlog(path):
    #     return mocklog
    #
    # monkeypatch.delattr("LogFactory().get_log")

    plain_password = "plaintext"
    secret_key = "secret"
    encrypted_password= "v5UltRskmSGbwTIbXym7jQ=="
    b64encoded_encrypted_password = "djVVbHRSc2ttU0did1RJYlh5bTdqUT09"

    assert cartridgeagentutils.decrypt_password(b64encoded_encrypted_password, secret_key) == plain_password, "Password decryption failed"