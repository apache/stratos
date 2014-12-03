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
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Hazelcast distributed list provider.
 */
public class HazelcastDistributedListProvider {
    private static final Log log = LogFactory.getLog(HazelcastDistributedListProvider.class);

    private HazelcastInstance hazelcastInstance;
    private Map<String, DistList> listMap;

    public HazelcastDistributedListProvider(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public List getList(String name, ListEntryListener listEntryListener) {
        List list = listMap.get(name);
        if(list == null) {
            synchronized (HazelcastDistributedListProvider.class) {
                if(list == null) {
                    list = new DistList(name, listEntryListener);
                }
            }
        }
        return list;
    }

    public void removeList(String name) {
        DistList list = listMap.get(name);
        if(list != null) {
            IList ilist = (IList) list;
            ilist.removeItemListener(list.getListenerId());
            listMap.remove(list);
            ilist.destroy();
        }
    }

    private class DistList implements List {
        private IList list;
        private String listenerId;

        public DistList(String name, final ListEntryListener listEntryListener) {
            this.list = hazelcastInstance.getList(name);
            listenerId = list.addItemListener(new ItemListener() {
                @Override
                public void itemAdded(ItemEvent itemEvent) {
                    listEntryListener.itemAdded(itemEvent.getItem());
                }

                @Override
                public void itemRemoved(ItemEvent itemEvent) {
                    listEntryListener.itemRemoved(itemEvent.getItem());
                }
            }, false);
        }

        @Override
        public int size() {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.size();
            }
            return 0;
        }

        @Override
        public boolean isEmpty() {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.isEmpty();
            }
            return true;
        }

        @Override
        public boolean contains(Object object) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.contains(object);
            }
            return false;
        }

        @Override
        public Iterator iterator() {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.iterator();
            }
            return null;
        }

        @Override
        public Object[] toArray() {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.toArray();
            }
            return new Object[0];
        }

        @Override
        public boolean add(Object object) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.add(object);
            }
            return false;
        }

        @Override
        public boolean remove(Object object) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.remove(object);
            }
            return false;
        }

        @Override
        public boolean addAll(Collection collection) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.addAll(collection);
            }
            return false;
        }

        @Override
        public boolean addAll(int i, Collection collection) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.addAll(i, collection);
            }
            return false;
        }

        @Override
        public void clear() {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                list.clear();
            }
        }

        @Override
        public Object get(int i) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.get(i);
            }
            return null;
        }

        @Override
        public Object set(int i, Object o) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.set(i, o);
            }
            return null;
        }

        @Override
        public void add(int i, Object o) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                list.add(i, o);
            }
        }

        @Override
        public Object remove(int i) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.remove(i);
            }
            return null;
        }

        @Override
        public int indexOf(Object o) {
            return list.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.lastIndexOf(o);
            }
            return -1;
        }

        @Override
        public ListIterator listIterator() {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.listIterator();
            }
            return null;
        }

        @Override
        public ListIterator listIterator(int i) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.listIterator(i);
            }
            return null;
        }

        @Override
        public List subList(int i, int i2) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.subList(i, i2);
            }
            return null;
        }

        @Override
        public boolean retainAll(Collection collection) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.retainAll(collection);
            }
            return false;
        }

        @Override
        public boolean removeAll(Collection collection) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.removeAll(collection);
            }
            return false;
        }

        @Override
        public boolean containsAll(Collection collection) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.containsAll(collection);
            }
            return false;
        }

        @Override
        public Object[] toArray(Object[] objects) {
            if (hazelcastInstance.getLifecycleService().isRunning()) {
                return list.toArray(objects);
            }
            return null;
        }

        public String getListenerId() {
            return listenerId;
        }
    }
}
