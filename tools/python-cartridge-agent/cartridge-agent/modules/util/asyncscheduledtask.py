import time
from threading import Thread


class AsyncScheduledTask(Thread):
    """
    Executes a given task with a given interval until being terminated
    """

    def __init__(self, delay, task):
        Thread.__init__(self)
        self.delay = delay
        self.task = task
        self.terminated = False

    def run(self):
        while not self.terminated:
            time.sleep(self.delay)
            self.task.start()

    def terminate(self):
        self.terminated = True