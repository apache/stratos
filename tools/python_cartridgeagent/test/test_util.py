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
import socket
from threading import Thread

ASYNC_WRITE_FILE = "asynctest.txt"


def test_async_task():
    with open(ASYNC_WRITE_FILE, "r") as f:
        init_context = f.read()

    test_task = TestTask()
    astask = ScheduledExecutor(1, test_task)
    start_time = time.time() * 1000
    astask.start()
    contents_changed = False
    timeout = 10  #seconds

    # wait till file content is written
    while not contents_changed and (time.time() * 1000 - start_time) < (10 * 1000):
        time.sleep(2)
        with open(ASYNC_WRITE_FILE, "r") as f:
            now_content = f.read()

        if init_context != now_content:
            contents_changed = True

    astask.terminate()
    f = open(ASYNC_WRITE_FILE, "r")
    end_time = float(f.read())
    assert (end_time - start_time) >= 1 * 1000, "Task was executed before specified delay"


class TestTask(AbstractAsyncScheduledTask):

    def execute_task(self):
        with open(ASYNC_WRITE_FILE, "w") as f:
            f.seek(0)
            f.truncate()
            f.write("%1.4f" % (time.time()*1000))


def test_decrypt_password_success():
    # def mockgetlog(path):
    #     return mocklog
    #
    # monkeypatch.delattr("LogFactory().get_log")
    # TODO: enable logging in cartridgeagentutils

    plain_password = "plaintext"
    secret_key = "tvnw63ufg9gh5111"
    encrypted_password= "jP1lZ5xMlpLzu8MbY2Porg=="

    decrypted_password = cartridgeagentutils.decrypt_password(encrypted_password, secret_key)
    #print decrypted_password

    assert decrypted_password == plain_password, "Password decryption failed"


def test_decrypt_password_failure():
    plain_password = "plaintext"
    secret_key = "notsecretkeyhere"
    encrypted_password= "jP1lZ5xMlpLzu8MbY2Porg=="
    assert cartridgeagentutils.decrypt_password(encrypted_password, secret_key) != plain_password, "Password decrypted for wrong key"


def test_create_dir_normal():
    assert True


def test_create_dir_system_path():
    assert True


def test_create_dir_existing_dir():
    assert True


def test_wait_for_ports_activity_normal():
    portnumber = 12345
    listener = PortListener(portnumber)
    listener.start()

    assert cartridgeagentutils.check_ports_active(socket.gethostbyname(socket.gethostname()), [str(portnumber)])


class PortListener(Thread):

    def __init__(self, portnumber):
        Thread.__init__(self)
        self.portnumber = portnumber
        self.terminated = False

    def run(self):
        s = socket.socket()
        host = socket.gethostname()

        s.bind((host, self.portnumber))
        s.listen(5)

        #while not self.terminated:
        c, addr = s.accept()     # Establish connection with client.
        #print 'Got connection from', addr
        c.send('Thank you for connecting')
        c.close()

        s.close()

    def terminate(self):
        self.terminated = True


def test_wait_for_ports_activity_non_existent():
    assert cartridgeagentutils.check_ports_active(socket.gethostbyname(socket.gethostname()), [str(34565)]) == False
