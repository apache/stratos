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

package org.apache.stratos.messaging.domain.topology.locking;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a lock in the Topology
 */

public class TopologyLock {

    private final ReentrantReadWriteLock lock;

    public TopologyLock () {
        lock = new ReentrantReadWriteLock(true);
    }

    /**
     * acquires write lock
     */
    public void acquireWriteLock() {
        lock.writeLock().lock();
    }

    /**
     * releases write lock
     */
    public void releaseWritelock() {
        lock.writeLock().unlock();
    }

    /**
     * acquires read lock
     */
    public void acquireReadLock() {
        lock.readLock().lock();
    }

    /**
     * releases read lock
     */
    public void releaseReadLock() {
        lock.readLock().unlock();
    }
}
