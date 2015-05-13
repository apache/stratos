/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
