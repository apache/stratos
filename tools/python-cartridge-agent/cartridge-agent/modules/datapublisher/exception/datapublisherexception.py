class DataPublisherException(Exception):

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
