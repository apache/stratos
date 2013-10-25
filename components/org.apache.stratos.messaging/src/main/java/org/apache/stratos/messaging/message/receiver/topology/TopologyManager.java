/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.message.receiver.topology;

import org.apache.stratos.messaging.domain.topology.Topology;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *  A singleton class for managing the topology data structure.
 *
 *  Usage:
 *  Acquire a relevant lock and invoke the getTopology() method inside a try block.
 *  Once processing is done release the lock using a finally block.
 */
public class TopologyManager {
    private static volatile Topology topology;
    private static volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static volatile ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private static volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public static void acquireReadLock() {
        readLock.lock();
    }

    public static void releaseReadLock() {
        readLock.unlock();
    }

    public static void acquireWriteLock() {
        writeLock.lock();
    }

    public static void releaseWriteLock() {
        writeLock.unlock();
    }

    public static synchronized Topology getTopology() {
        if (topology == null) {
            synchronized (TopologyManager.class){
                if (topology == null) {
                    topology = new Topology();
                }
            }
        }
        return topology;
    }
}
