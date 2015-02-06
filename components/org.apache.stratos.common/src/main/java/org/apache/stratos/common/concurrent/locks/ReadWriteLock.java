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
import org.apache.stratos.common.exception.InvalidLockRequestedException;
import org.apache.stratos.common.threading.StratosThreadPool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Read write lock implements read/write locks using java.util.concurrent.locks.ReentrantReadWriteLock and
 * provides logic for detecting following scenarios:
 * - A thread trying to acquire a write lock while having a read lock.
 * - Unreleased locks for a certain time period
 */
public class ReadWriteLock {

    private static final Log log = LogFactory.getLog(ReadWriteLock.class);

    private static final String READ_WRITE_LOCK_MONITOR_THREAD_POOL = "read.write.lock.monitor.thread.pool";
    private static final String READ_WRITE_LOCK_MONITOR_THREAD_POOL_SIZE_KEY = "read.write.lock.monitor.thread.pool.size";

    private final String name;
    private final ReentrantReadWriteLock lock;
    private final Map<Long, Map<LockType, LockMetadata>> threadToLockSetMap;
    private boolean readWriteLockMonitorEnabled;
    private int readWriteLockMonitorInterval;
    private int threadPoolSize;

    public ReadWriteLock(String name) {
        this.name = name;
        this.lock = new ReentrantReadWriteLock(true);
        this.threadToLockSetMap = new ConcurrentHashMap<Long, Map<LockType, LockMetadata>>();

        readWriteLockMonitorEnabled = Boolean.getBoolean("read.write.lock.monitor.enabled");
        if (readWriteLockMonitorEnabled) {
            // Schedule read write lock monitor
            readWriteLockMonitorInterval = Integer.getInteger("read.write.lock.monitor.interval", 30000);
            threadPoolSize = Integer.getInteger(READ_WRITE_LOCK_MONITOR_THREAD_POOL_SIZE_KEY, 10);

            ScheduledExecutorService scheduledExecutorService = StratosThreadPool.getScheduledExecutorService(
                    READ_WRITE_LOCK_MONITOR_THREAD_POOL, threadPoolSize);
            scheduledExecutorService.scheduleAtFixedRate(new ReadWriteLockMonitor(this),
                    readWriteLockMonitorInterval, readWriteLockMonitorInterval, TimeUnit.MILLISECONDS);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Lock monitor scheduled: [lock-name] %s [interval] %d seconds",
                        name, (readWriteLockMonitorInterval / 1000)));
            }
        }
    }

    public String getName() {
        return name;
    }

    Map<Long, Map<LockType, LockMetadata>> getThreadToLockSetMap() {
        return threadToLockSetMap;
    }

    private Map<LockType, LockMetadata> getLockTypeLongMap(long threadId) {
        Map<LockType, LockMetadata> lockTypeLongMap = threadToLockSetMap.get(threadId);
        if (lockTypeLongMap == null) {
            synchronized (ReadWriteLock.class) {
                if (lockTypeLongMap == null) {
                    lockTypeLongMap = new HashMap<LockType, LockMetadata>();
                    threadToLockSetMap.put(threadId, lockTypeLongMap);
                }
            }
        }
        return lockTypeLongMap;
    }

    /**
     * acquires write lock
     */
    public void acquireWriteLock() {
        Thread currentThread = Thread.currentThread();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Acquiring write lock: [lock-name] %s [thread-id] %d [thread-name] %s",
                    getName(), currentThread.getId(), currentThread.getName()));
        }

        if (readWriteLockMonitorEnabled) {
            // Check whether the thread has already taken a read lock before requesting a write lock
            Map<LockType, LockMetadata> lockTypeLongMap = getLockTypeLongMap(currentThread.getId());
            if (lockTypeLongMap.containsKey(LockType.Read)) {
                String message = String.format("System error, cannot acquire a write lock while having a " +
                                "read lock on the same thread: [lock-name] %s [thread-id] %d [thread-name] %s",
                        getName(), currentThread.getId(), currentThread.getName());
                InvalidLockRequestedException exception = new InvalidLockRequestedException(message);
                log.error(exception);
                throw exception;
            }
        }

        lock.writeLock().lock();

        if (readWriteLockMonitorEnabled) {
            LockMetadata lockMetadata = new LockMetadata(getName(), LockType.Write, currentThread.getId(),
                    currentThread.getName(), currentThread.getStackTrace(), System.currentTimeMillis());
            Map<LockType, LockMetadata> lockTypeLongMap = getLockTypeLongMap(currentThread.getId());
            lockTypeLongMap.put(lockMetadata.getLockType(), lockMetadata);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Write lock acquired: [lock-name] %s [thread-id] %d [thread-name] %s",
                    getName(), currentThread.getId(), currentThread.getName()));
        }
    }

    /**
     * releases write lock
     */
    public void releaseWriteLock() {
        Thread currentThread = Thread.currentThread();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Releasing write lock: [lock-name] %s [thread-id] %d [thread-name] %s",
                    getName(), currentThread.getId(), currentThread.getName()));
        }

        if(lock.writeLock().isHeldByCurrentThread()) {
            lock.writeLock().unlock();

            if (readWriteLockMonitorEnabled) {
                Map<LockType, LockMetadata> lockTypeLongMap = getLockTypeLongMap(currentThread.getId());
                lockTypeLongMap.remove(LockType.Write);
            }

            if (log.isDebugEnabled()) {
                log.debug(String.format("Write lock released: [lock-name] %s [thread-id] %d [thread-name] %s",
                        getName(), currentThread.getId(), currentThread.getName()));
            }
        } else {
            log.warn(String.format("System warning! A thread is trying to release a lock which has not been taken: " +
                            "[lock-name] %s [thread-id] %d [thread-name] %s",
                    getName(), currentThread.getId(), currentThread.getName()));
        }
    }

    /**
     * acquires read lock
     */
    public void acquireReadLock() {
        Thread currentThread = Thread.currentThread();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Acquiring read lock: [lock-name] %s [thread-id] %d [thread-name] %s",
                    getName(), currentThread.getId(), currentThread.getName()));
        }

        lock.readLock().lock();

        if (readWriteLockMonitorEnabled) {
            Map<LockType, LockMetadata> lockTypeLongMap = getLockTypeLongMap(currentThread.getId());
            LockMetadata lockMetadata = new LockMetadata(getName(), LockType.Read, currentThread.getId(),
                    currentThread.getName(), currentThread.getStackTrace(), System.currentTimeMillis());
            lockTypeLongMap.put(lockMetadata.getLockType(), lockMetadata);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Read lock acquired: [lock-name] %s [thread-id] %d [thread-name] %s",
                    getName(), currentThread.getId(), currentThread.getName()));
        }
    }

    /**
     * releases read lock
     */
    public void releaseReadLock() {
        Thread currentThread = Thread.currentThread();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Releasing read lock: [lock-name] %s [thread-id] %d [thread-name] %s",
                    getName(), currentThread.getId(), currentThread.getName()));
        }

        lock.readLock().unlock();

        if (readWriteLockMonitorEnabled) {
            Map<LockType, LockMetadata> lockTypeLongMap = getLockTypeLongMap(currentThread.getId());
            lockTypeLongMap.remove(LockType.Read);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Read lock released: [lock-name] %s [thread-id] %d [thread-name] %s",
                    getName(), currentThread.getId(), currentThread.getName()));
        }
    }
}