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

package org.apache.stratos.common.clustering.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.clustering.DistributedObjectProvider;
import org.apache.stratos.common.internal.ServiceReferenceHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides objects to be managed in distributed and non-distributed environments.
 */
public class HazelcastDistributedObjectProvider implements DistributedObjectProvider {
    private static final Log log = LogFactory.getLog(HazelcastDistributedObjectProvider.class);

    public HazelcastDistributedObjectProvider() {
    }

    private boolean isClustered() {
        AxisConfiguration axisConfiguration = ServiceReferenceHolder.getInstance().getAxisConfiguration();
        return ((axisConfiguration != null) && (axisConfiguration.getClusteringAgent() != null)
                && (getHazelcastInstance() != null));
    }

    private HazelcastInstance getHazelcastInstance() {
        return ServiceReferenceHolder.getInstance().getHazelcastInstance();
    }

    private com.hazelcast.core.ILock acquireDistributedLock(Object object) {
        if((!isClustered()) || (object == null)) {
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Acquiring distributed lock for %s...", object.getClass().getSimpleName()));
        }
        ILock lock = getHazelcastInstance().getLock(object);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Distributed lock acquired for %s", object.getClass().getSimpleName()));
        }
        return lock;
    }

    private void releaseDistributedLock(ILock lock) {
        if(lock == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Releasing distributed lock for %s...", lock.getKey()));
        }
        lock.forceUnlock();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Distributed lock released for %s", lock.getKey()));
        }
    }

    /**
     * If clustering is enabled returns a distributed map object, otherwise returns a
     * concurrent local map object.
     * @param key
     * @return
     */
    @Override
    public Map getMap(String key) {
        if(isClustered()) {
            return getHazelcastInstance().getMap(key);
        } else {
            return new ConcurrentHashMap<Object, Object>();
        }
    }

    /**
     * If clustering is enabled returns a distributed list, otherwise returns
     * a local array list.
     * @param name
     * @return
     */
    @Override
    public List getList(String name) {
        if(isClustered()) {
            return getHazelcastInstance().getList(name);
        } else {
            return new ArrayList();
        }
    }

    /**
     * Put a key value pair to a map, if clustered use a distributed lock.
     * @param map
     * @param key
     * @param value
     */
    @Override
    public void putToMap(Map map, Object key, Object value) {
         if(isClustered()) {
             ILock lock = null;
             try {
                 IMap imap = (IMap) map;
                 lock = acquireDistributedLock(imap.getName());
                 imap.set(key, value);
             } finally {
                 releaseDistributedLock(lock);
             }
         } else {
            map.put(key, value);
         }
    }

    /**
     * Remove an object from a map, if clustered use a distributed lock.
     * @param map
     * @param key
     */
    @Override
    public void removeFromMap(Map map, Object key) {
        if(isClustered()) {
            ILock lock = null;
            try {
                IMap imap = (IMap) map;
                lock = acquireDistributedLock(imap.getName());
                imap.delete(key);
            } finally {
                releaseDistributedLock(lock);
            }
        } else {
            map.remove(key);
        }
    }

    /**
     * Add an object to a list, if clustered use a distributed lock.
     * @param list
     * @param value
     */
    @Override
    public void addToList(List list, Object value) {
        if(isClustered()) {
            ILock lock = null;
            try {
                IList ilist = (IList) list;
                lock = acquireDistributedLock(ilist.getName());
                ilist.add(value);
            } finally {
                releaseDistributedLock(lock);
            }
        } else {
            list.add(value);
        }
    }

    /**
     * Remove an object from a list, if clustered use a distributed lock.
     * @param list
     * @param value
     */
    @Override
    public void removeFromList(List list, Object value) {
        if(isClustered()) {
            ILock lock = null;
            try {
                IList ilist = (IList) list;
                lock = acquireDistributedLock(ilist.getName());
                ilist.remove(value);
            } finally {
                releaseDistributedLock(lock);
            }
        } else {
            list.remove(value);
        }
    }
}
