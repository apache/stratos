class ParameterNotFoundException(Exception):
    __message = None

    def __init__(self, message):
        Exception.__init__(self, message)
        self.__message = message

    def get_message(self):
        return self.__message
