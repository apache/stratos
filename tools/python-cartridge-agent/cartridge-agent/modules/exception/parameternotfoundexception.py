class ParameterNotFoundException(Exception):
    """
    Exception raised when a property is not present in the configuration or the payload
    of the cartridge agent
    """
    __message = None

    def __init__(self, message):
        Exception.__init__(self, message)
        self.__message = message

    def get_message(self):
        """
        The message provided when the exception is raised
        :return: message
        :rtype: str
        """
        return self.__message
