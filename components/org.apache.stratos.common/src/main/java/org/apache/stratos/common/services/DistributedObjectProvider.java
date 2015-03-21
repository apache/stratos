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

package org.apache.stratos.common.services;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Distributed object provider service interface.
 * Caution! When using distributed maps and lists, please note that changes done to an item in a map/list
 * after adding them to the map/list will not be replicated in the cluster. If a modification of an item
 * needs to be replicated, that item needs to be put() to the map or set() back in the list.
 */
public interface DistributedObjectProvider extends Serializable {
    /**
     * Returns a distributed map if clustering is enabled, else returns a local hash map.
     * @param name
     * @return
     */
    Map getMap(String name);

    /**
     * Removes a map from the object provider.
     * @param name
     */
    void removeMap(String name);

    /**
     * Returns a distributed list if clustering is enabled, else returns a local array list.
     * @param name
     * @return
     */
    List getList(String name);

    /**
     * Remove a list from the object provider.
     * @param name
     */
    void removeList(String name);

    /**
     * Acquires a distributed lock if clustering is enabled, else acquires a local reentrant lock and
     * returns the lock object.
     * @param object
     * @return
     */
    Lock acquireLock(Object object);

    /**
     * Releases a given distributed/local lock.
     * @param lock
     */
    void releaseLock(Lock lock);
}
