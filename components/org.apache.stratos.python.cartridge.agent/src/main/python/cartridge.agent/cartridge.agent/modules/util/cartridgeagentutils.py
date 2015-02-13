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

from Crypto.Cipher import AES
import base64
import os
import time
import socket

from log import LogFactory

log = LogFactory().get_log(__name__)

unpad = lambda s: s[0:-ord(s[-1])]
current_milli_time = lambda: int(round(time.time() * 1000))


def decrypt_password(pass_str, secret):
    """
    Decrypts the given password using the given secret. The encryption is assumed to be done
    without IV, in AES.
    :param str pass_str: Encrypted password string in Base64 encoding
    :param str secret: The secret string
    :return: The decrypted password
    :rtype: str
    """

    if pass_str is None or pass_str.strip() == "":
        return pass_str.strip()

    dec_pass = ""

    try:
        log.debug("Decrypting password")
        bdecoded_pass = base64.b64decode(pass_str.strip())
        # secret length should be 16
        cipher = AES.new(secret.strip(), AES.MODE_ECB)
        # dec_pass = unpad(cipher.decrypt(bdecoded_pass))
        dec_pass = cipher.decrypt(bdecoded_pass)
    except:
        log.exception("Exception occurred while decrypting password")

    log.debug("Decrypted PWD: [%r]" % dec_pass)
    return unicode(dec_pass, "utf-8")


def wait_until_ports_active(ip_address, ports, ports_check_timeout=600000):
    """
    Blocks until the given list of ports become active
    :param str ip_address: Ip address of the member to be checked
    :param list[str] ports: List of ports to be checked
    :param int ports_check_timeout: The timeout in milliseconds, defaults to 1000*60*10
    :return: void
    """
    if ports_check_timeout is None:
        ports_check_timeout = 1000 * 60 * 10

    log.debug("Port check timeout: %r" % ports_check_timeout)

    active = False
    start_time = current_milli_time()
    while not active:
        log.info("Waiting for ports to be active: [ip] %r [ports] %r" % (ip_address, ports))
        active = check_ports_active(ip_address, ports)
        end_time = current_milli_time()
        duration = end_time - start_time

        if duration > ports_check_timeout:
            log.info("Port check timeout reached: [ip] %r [ports] %r [timeout] %r" % (ip_address, ports, ports_check_timeout))
            return

        time.sleep(5)
    log.info("Ports activated: [ip] %r [ports] %r" % (ip_address, ports))


def check_ports_active(ip_address, ports):
    """
    Checks the given list of port addresses for active state
    :param str ip_address: Ip address of the member to be checked
    :param list[str] ports: The list of ports to be checked
    :return: True if the ports are active, False if at least one is not active
    :rtype: bool
    """
    if len(ports) < 1:
        raise RuntimeError("No ports found")

    for port in ports:
        s = socket.socket()
        s.settimeout(5)
        try:
            s.connect((ip_address, int(port)))
            log.debug("Port %r is active" % port)
            s.close()
        except socket.error:
            log.debug("Port %r is not active" % port)
            return False

    return True


def get_working_dir():
    """
    Returns the base directory of the cartridge agent.
    :return: Base working dir path
    :rtype : str
    """
    #"/path/to/cartridgeagent/modules/util/".split("modules") returns ["/path/to/cartridgeagent/", "/util"]
    return os.path.abspath(os.path.dirname(__file__)).split("modules")[0]