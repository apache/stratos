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
import time
import socket
import string
import hashlib

from log import LogFactory

BS = 16

log = LogFactory().get_log(__name__)


def unpad(s): return s[0:-ord(s[-1])]


def current_milli_time(): return int(round(time.time() * 1000))


def pad(s): return s + (BS - len(s) % BS) * chr(BS - len(s) % BS)


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
        log.debug("Decrypting cipher text: %s" % pass_str)
        bdecoded_pass = base64.b64decode(pass_str.strip())
        # secret length should be 16
        cipher = AES.new(secret.strip(), AES.MODE_ECB)
        # dec_pass = unpad(cipher.decrypt(bdecoded_pass))
        dec_pass = cipher.decrypt(bdecoded_pass)
    except:
        log.exception("Exception occurred while decrypting password")

    # remove nonprintable characters that are padded in the decrypted password
    dec_pass = filter(lambda x: x in string.printable, dec_pass)
    # dec_pass_md5 = hashlib.md5(dec_pass.encode('utf-8')).hexdigest()
    # log.debug("Decrypted password md5sum: [%r]" % dec_pass_md5)
    return dec_pass


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

    log.debug("Port check timeout: %s" % ports_check_timeout)

    ports_left = ports
    start_time = current_milli_time()

    # check ports until all are active or timeout exceeds
    while True:
        log.info("Waiting for ports to be active: [ip] %s [ports] %s" % (ip_address, ports))

        # check each port for activity
        for checking_port in list(ports_left):
            port_active = check_port_active(ip_address, checking_port)
            if port_active:
                log.debug("Port %s on host %s active" % (checking_port, ip_address))
                ports_left.remove(checking_port)

        # if no ports are left to check for activity, return
        if len(ports_left) == 0:
            log.info("Ports activated: [ip] %r [ports] %r" % (ip_address, ports))
            return True

        # active = check_ports_active(ip_address, ports)
        end_time = current_milli_time()
        duration = end_time - start_time

        if duration > ports_check_timeout:
            log.info("Port check timeout reached: [ip] %s [ports] %s [timeout] %s"
                     % (ip_address, ports, ports_check_timeout))
            return False

        time.sleep(5)


def check_port_active(ip_address, port):
    """
    Checks the given port on the given host for activity
    :param str ip_address: Ip address of the member to be checked
    :param str port: The port to be checked
    :return: True if the ports are active, False if at least one is not active
    :rtype: bool
    """
    if port is None:
        raise RuntimeError("Cannot check invalid port for activity")

    try:
        port_int = int(port)
    except ValueError:
        raise RuntimeError("Cannot check invalid port for activity %s" % port)

    s = socket.socket()
    s.settimeout(5)
    try:
        s.connect((ip_address, port_int))
        log.debug("Port %s is active" % port)
        s.close()
        return True
    except socket.error:
        log.debug("Port %s is not active" % port)
        return False


class IncrementalCeilingListIterator(object):
    """
    Iterates through a given list and returns elements. At the end of the list if terminate_at_end is set to false,
    the last element will be returned repeatedly. If terminate_at_end is set to true, an IndexError will be thrown.
    """

    def __init__(self, intervals, terminate_at_end):
        self.__intervals = intervals
        self.__index = 0
        self.__terminate_at_end = terminate_at_end

    def get_next_retry_interval(self):
        """
        Retrieves the next element in the list.
        :return:
        :rtype: int
        """
        if self.__index < len(self.__intervals):
            next_interval = self.__intervals[self.__index]
            self.__index += 1
        else:
            if self.__terminate_at_end:
                raise IndexError("Reached the end of the list")
            else:
                next_interval = self.__intervals[-1]

        return next_interval
