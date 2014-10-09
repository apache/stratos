import logging
import logging.config
import os


class LogFactory(object):
    """
    Singleton implementation for handling logging in CartridgeAgent
    """
    class __LogFactory:
        def __init__(self):
            self.logs = {}
            logging_conf = os.path.abspath(os.path.dirname(__file__)).split("modules")[0] + "logging.ini"
            logging.config.fileConfig(logging_conf)

        def get_log(self, name):
            if name not in self.logs:
                self.logs[name] = logging.getLogger(name)

            return self.logs[name]

    instance = None

    def __new__(cls, *args, **kwargs):
        if not LogFactory.instance:
            LogFactory.instance = LogFactory.__LogFactory()

        return LogFactory.instance

    def get_log(self, name):
        """
        Returns a logger class with the specified channel name. Creates a new logger if one doesn't exists for the
        specified channel
        :param str name: Channel name
        :return: The logger class
        :rtype: RootLogger
        """
        return self.instance.get_log(name)