package org.apache.stratos.common.concurrent.locks;

/**
 * Lock metadata.
 */
public class LockMetadata {

    private final String lockName;
    private final LockType lockType;
    private final long threadId;
    private final String threadName;
    private final StackTraceElement[] stackTrace;
    private final long createdTime;

    public LockMetadata(String lockName, LockType lockType, long threadId, String threadName,
                        StackTraceElement[] stackTrace, long createdTime) {
        this.lockName = lockName;
        this.lockType = lockType;
        this.threadId = threadId;
        this.threadName = threadName;
        this.stackTrace = stackTrace;
        this.createdTime = createdTime;
    }

    public String getLockName() {
        return lockName;
    }

    public LockType getLockType() {
        return lockType;
    }

    public long getThreadId() {
        return threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    public long getCreatedTime() {
        return createdTime;
    }
}
