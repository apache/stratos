class AbstractHealthStatisticsReader:
    """
    TODO:
    """

    def stat_cartridge_health(self):
        """
        Abstract method that when implemented reads the memory usage and the load average
        of the instance running the agent and returns a CartridgeHealthStatistics object
        with the information

        :return: CartridgeHealthStatistics object with memory usage and load average values
        :rtype : CartridgeHealthStatistics
        """
        raise NotImplementedError


class CartridgeHealthStatistics:
    """
    Holds the memory usage and load average reading
    """

    def __init__(self):
        self.memory_usage = None
        """:type : float"""
        self.load_avg = None
        """:type : float"""


class CEPPublisherException(Exception):
    """
    Exception to be used during CEP publishing operations
    """

    def __init__(self, msg):
        super(self,  msg)
        self.message = msg

    def get_message(self):
        """
        The message provided when the exception is raised
        :return: message
        :rtype: str
        """
        return self.message
