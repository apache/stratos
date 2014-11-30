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

package org.apache.stratos.cloud.controller.clustering;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An object handler for managing objects in distributed and non-distributed environments.
 */
public class DistributedObjectHandler {
    private static final Log log = LogFactory.getLog(DistributedObjectHandler.class);

    private final boolean clustered;
    private final HazelcastInstance hazelcastInstance;

    public DistributedObjectHandler(boolean clustered, HazelcastInstance hazelcastInstance) {
        this.clustered = clustered;
        this.hazelcastInstance = hazelcastInstance;
    }

    private com.hazelcast.core.ILock acquireDistributedLock(Object object) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Acquiring distributed lock for %s...", object.getClass().getSimpleName()));
        }
        ILock lock = hazelcastInstance.getLock(object);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Distributed lock acquired for %s", object.getClass().getSimpleName()));
        }
        return lock;
    }

    private void releaseDistributedLock(ILock lock) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Releasing distributed lock for %s...", lock.getKey()));
        }
        lock.forceUnlock();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Distributed lock released for %s", lock.getKey()));
        }
    }

    public Map getMap(String key) {
        if(clustered) {
            return hazelcastInstance.getMap(key);
        } else {
            return new ConcurrentHashMap<Object, Object>();
        }
    }

    public List getList(String name) {
        if(clustered) {
            return hazelcastInstance.getList(name);
        } else {
            return new ArrayList();
        }
    }

    public void putToMap(Map map, Object key, Object value) {
         if(clustered) {
             ILock lock = null;
             try {
                 lock = acquireDistributedLock(map);
                 ((IMap)map).set(key, value);
             } finally {
                 releaseDistributedLock(lock);
             }
         } else {
            map.put(key, value);
         }
    }

    public void removeFromMap(Map map, Object key) {
        if(clustered) {
            ILock lock = null;
            try {
                lock = acquireDistributedLock(map);
                ((IMap)map).delete(key);
            } finally {
                releaseDistributedLock(lock);
            }
        } else {
            map.remove(key);
        }
    }

    public void addToList(List list, Object value) {
        if(clustered) {
            ILock lock = null;
            try {
                lock = acquireDistributedLock(list);
                ((IList)list).add(value);
            } finally {
                releaseDistributedLock(lock);
            }
        } else {
            list.add(value);
        }
    }

    public void removeFromList(List list, Object value) {
        if(clustered) {
            ILock lock = null;
            try {
                lock = acquireDistributedLock(list);
                ((IList)list).remove(value);
            } finally {
                releaseDistributedLock(lock);
            }
        } else {
            list.remove(value);
        }
    }
}
