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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.exception.LockNotReleasedException;

import java.util.Map;

/**
 * Read write lock monitor.
 */
class ReadWriteLockMonitor implements Runnable {

    private static final Log log = LogFactory.getLog(ReadWriteLockMonitor.class);

    private static final long LOCK_TIMEOUT = Long.getLong("read.write.lock.timeout", 30000); // 30 seconds
    private ReadWriteLock readWriteLock;

    ReadWriteLockMonitor(ReadWriteLock readWriteLock) {
        this.readWriteLock = readWriteLock;
    }

    @Override
    public void run() {
        try {
            if(readWriteLock.getThreadToLockSetMap() != null) {
                for (Map.Entry<Long, Map<LockType, LockMetadata>> entry : readWriteLock.getThreadToLockSetMap().entrySet()) {
                    Map<LockType, LockMetadata> lockTypeLongMap = entry.getValue();
                    LockMetadata lockMetadata = lockTypeLongMap.get(LockType.Read);
                    if (lockMetadata != null) {
                        checkTimeout(lockMetadata);
                    }
                    lockMetadata = lockTypeLongMap.get(LockType.Write);
                    if (lockMetadata != null) {
                        checkTimeout(lockMetadata);
                    }
                }
            }
        } catch (Exception e) {
            String message = "Read write lock monitor failed";
            log.error(message, e);
        }
    }

    private void checkTimeout(LockMetadata lockMetadata) {
        if ((System.currentTimeMillis() - lockMetadata.getCreatedTime()) > LOCK_TIMEOUT) {
            String message = String.format("System error, lock has not released for %d seconds: " +
                            "[lock-name] %s [lock-type] %s [thread-id] %d [thread-name] %s [stack-trace] \n%s",
                    LOCK_TIMEOUT / (1000), lockMetadata.getLockName(), lockMetadata.getLockType(),
                    lockMetadata.getThreadId(), lockMetadata.getThreadName(), stackTraceToString(
                            lockMetadata.getStackTrace()));
            LockNotReleasedException exception = new LockNotReleasedException();
            log.error(message, exception);
        }
    }

    private String stackTraceToString(StackTraceElement[] stackTraceElements) {
        StringBuffer sb = new StringBuffer();
        if(stackTraceElements != null) {
            for(StackTraceElement element : stackTraceElements) {
                if(element != null) {
                    sb.append(element.toString()).append("\n");
                }
            }
        }
        return sb.toString();
    }
}
