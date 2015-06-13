import logging
import os
import Configs


if not os.path.exists(Configs.stratos_dir_path):
    try:
        os.makedirs(Configs.stratos_dir_path)
        logging.info("Created directory: "+Configs.stratos_dir_path)
    except OSError:
        logging.warning("Failed to create directory: "+Configs.stratos_dir_path)

logging.basicConfig(filename=Configs.log_file_path, level=logging.DEBUG)
