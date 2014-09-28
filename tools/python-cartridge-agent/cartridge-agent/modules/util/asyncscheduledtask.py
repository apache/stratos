import time
from threading import Thread


class AsyncScheduledTask(Thread):
    """
    Executes a given task with a given interval until being terminated
    """

    def __init__(self, delay, task):
        Thread.__init__(self)
        self.delay = delay
        """ :type : int  """
        self.task = task
        """ :type : Thread  """
        self.terminated = False
        """ :type : bool  """

    def run(self):
        """
        Start the scheuled task with a sleep time of delay in between
        :return:
        """
        while not self.terminated:
            time.sleep(self.delay)
            self.task.start()

    def terminate(self):
        """
        Terminate the scheduled task. Allow a maximum of 'delay' seconds to be terminated.
        :return: void
        """
        self.terminated = True