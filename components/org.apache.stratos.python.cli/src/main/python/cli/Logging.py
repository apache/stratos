import logging
import os

stratos_dir = os.path.expanduser('~/.stratos')
log_file_path = stratos_dir+"stratos-cli.log"


if not os.path.exists(stratos_dir):
    try:
        os.makedirs(stratos_dir)
        logging.info("Created directory: "+stratos_dir)
    except OSError:
        logging.warning("Failed to create directory: "+stratos_dir)


logging.basicConfig(filename=log_file_path, level=logging.DEBUG)
