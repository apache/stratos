from cartridgeagent.modules.util.asyncscheduledtask import AsyncScheduledTask
from threading import Thread
import time

def thread_worker():
    f = open("asynctest.txt", "w")
    f.write("%1.4f" % time.time() * 1000)
    f.close()

def test_async_task():
    astask = AsyncScheduledTask(2, Thread(thread_worker()))
    start_time = time.time() * 1000
    astask.run()
    time.sleep(3)
    astask.terminate()
    f = open("asynctest.txt")
    end_time = float(f.read())
    assert (end_time - start_time) >= 2 * 1000, "Task was executed before specified delay"


