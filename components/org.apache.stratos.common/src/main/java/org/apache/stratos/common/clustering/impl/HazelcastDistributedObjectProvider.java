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
import com.hazelcast.core.ILock;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.clustering.DistributedObjectProvider;
import org.apache.stratos.common.internal.ServiceReferenceHolder;
import org.wso2.carbon.caching.impl.MapEntryListener;
import org.wso2.carbon.core.clustering.hazelcast.HazelcastDistributedMapProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides objects to be managed in distributed and non-distributed environments.
 */
public class HazelcastDistributedObjectProvider implements DistributedObjectProvider {
    private static final Log log = LogFactory.getLog(HazelcastDistributedObjectProvider.class);

    private HazelcastDistributedMapProvider mapProvider;
    private HazelcastDistributedListProvider listProvider;
    private Map<Object, Lock> locksMap;

    public HazelcastDistributedObjectProvider() {
        HazelcastInstance hazelcastInstance = ServiceReferenceHolder.getInstance().getHazelcastInstance();
        mapProvider = new HazelcastDistributedMapProvider(hazelcastInstance);
        listProvider = new HazelcastDistributedListProvider(hazelcastInstance);
        locksMap = new HashMap<Object, Lock>();
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
            return mapProvider.getMap(key, new MapEntryListener() {
                @Override
                public <X> void entryAdded(X key) {
                    if(log.isDebugEnabled()) {
                        log.debug(String.format("Entry added to distributed map: [key] %s", key));
                    }
                }

                @Override
                public <X> void entryRemoved(X key) {
                    if(log.isDebugEnabled()) {
                        log.debug(String.format("Entry removed from distributed map: [key] %s", key));
                    }
                }

                @Override
                public <X> void entryUpdated(X key) {
                    if(log.isDebugEnabled()) {
                        log.debug(String.format("Entry updated in distributed map: [key] %s", key));
                    }
                }
            });
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
            return listProvider.getList(name, new ListEntryListener() {
                @Override
                public void itemAdded(Object item) {
                    if(log.isDebugEnabled()) {
                        log.debug("Item added to distributed list: " + item);
                    }
                }

                @Override
                public void itemRemoved(Object item) {
                    if(log.isDebugEnabled()) {
                        log.debug("Item removed from distributed list: " + item);
                    }
                }
            });
        } else {
            return new ArrayList();
        }
    }

    @Override
    public Lock acquireLock(Object object) {
        if(isClustered()) {
            return acquireDistributedLock(object);
        } else {
            Lock lock = locksMap.get(object);
            if(lock == null) {
                synchronized (object) {
                    if(lock == null) {
                        lock = new ReentrantLock();
                        locksMap.put(object, lock);
                    }
                }
            }
            lock.lock();
            return lock;
        }
    }

    @Override
    public void releaseLock(Lock lock) {
         if(isClustered()) {
             releaseDistributedLock((ILock)lock);
         } else {
             lock.unlock();
         }
    }

    private boolean isClustered() {
        AxisConfiguration axisConfiguration = ServiceReferenceHolder.getInstance().getAxisConfiguration();
        return ((axisConfiguration != null) && (axisConfiguration.getClusteringAgent() != null)
                && (getHazelcastInstance() != null));
    }

    private HazelcastInstance getHazelcastInstance() {
        return ServiceReferenceHolder.getInstance().getHazelcastInstance();
    }

    protected com.hazelcast.core.ILock acquireDistributedLock(Object object) {
        if(object == null) {
            if(log.isWarnEnabled()) {
                log.warn("Could not acquire distributed lock, object is null");
            }
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

    protected void releaseDistributedLock(ILock lock) {
        if(lock == null) {
            if(log.isWarnEnabled()) {
                log.warn("Could not release distributed lock, lock is null");
            }
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
}
