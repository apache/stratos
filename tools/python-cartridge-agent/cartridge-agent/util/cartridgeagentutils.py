from Crypto.Cipher import AES
import base64
import logging
import os

unpad = lambda s : s[0:-ord(s[-1])]
logging.basicConfig(level=logging.DEBUG)
log = logging.getLogger(__name__)


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