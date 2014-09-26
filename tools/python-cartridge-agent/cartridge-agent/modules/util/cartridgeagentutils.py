from Crypto.Cipher import AES
import base64
import logging
import os
import time
import socket
import shutil

from .. config.cartridgeagentconfiguration import CartridgeAgentConfiguration
import cartridgeagentconstants

unpad = lambda s: s[0:-ord(s[-1])]
logging.basicConfig(level=logging.DEBUG)
log = logging.getLogger(__name__)

current_milli_time = lambda: int(round(time.time() * 1000))


def decrypt_password(pass_str, secret):
    if pass_str is None:
        return pass_str

    dec_pass = ""

    try:
        log.debug("Decrypting password")
        bdecoded_pass = base64.b64decode(pass_str)
        #secret length should be 16
        cipher = AES.new(secret, AES.MODE_ECB)
        dec_pass = unpad(cipher.decrypt(bdecoded_pass))
    except:
        log.exception("Exception occurred while decrypting password")

    log.debug("Decrypted PWD: [%r]" % dec_pass)
    return dec_pass


def create_dir(path):
    """
    mkdir the provided path
    :param path: The path to the directory to be made
    :return: True if mkdir was successful, False if dir already exists
    """
    try:
        os.mkdir(path)
        log.info("Successfully created directory [%r]" % path)
        return True
    except OSError:
        log.exception("Directory creating failed in [%r]. Directory already exists. " % path)

    return False


def delete_folder_tree(path):
    try:
        shutil.rmtree(path)
        log.debug("Directory [%r] deleted." % path)
    except OSError:
        log.exception("Deletion of folder path %r failed." % path)


def wait_until_ports_active(ip_address, ports):
    ports_check_timeout = CartridgeAgentConfiguration.read_property("port.check.timeout")
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
            return

        time.sleep(5)

    log.info("Ports activated: [ip] %r [ports] %r" % (ip_address, ports))


def check_ports_active(ip_address, ports):
    if len(ports) < 1:
        raise RuntimeError("No ports found")

    for port in ports:
        s = socket.socket()
        s.settimeout(5)
        try:
            s.connect(ip_address, port)
            log.debug("Port %r is active" % port)
            s.close()
        except socket.error:
            log.debug("Print %r is not active" % port)
            return False

    return True


def validate_tenant_range(tenant_range):
    valid = False
    if tenant_range == "*":
        valid = True
    else:
        arr = tenant_range.split(cartridgeagentconstants.TENANT_RANGE_DELIMITER)
        if len(arr) == 2:
            if arr[0].isdigit() and arr[1].isdigit():
                valid = True
            elif arr[0].isdigit() and arr[1] == "*":
                valid = True

    if not valid:
        raise RuntimeError("Tenant range %r is not valid" % tenant_range)