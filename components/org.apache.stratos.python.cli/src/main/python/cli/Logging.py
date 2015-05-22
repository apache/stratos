import logging
import os
import Configs

stratos_dir_path = os.path.expanduser(Configs.stratos_dir)
log_file_path = stratos_dir_path+Configs.log_file_name


if not os.path.exists(stratos_dir_path):
    try:
        os.makedirs(stratos_dir_path)
        logging.info("Created directory: "+stratos_dir_path)
    except OSError:
        logging.warning("Failed to create directory: "+stratos_dir_path)


logging.basicConfig(filename=log_file_path, level=logging.DEBUG)
