import logging

logging.basicConfig(level=logging.DEBUG)
log = logging.getLogger(__name__)

def execute_copy_artifact_extension(source, destination):
    raise NotImplementedError

def execute_instance_started_extention(env_params):
    raise NotImplementedError