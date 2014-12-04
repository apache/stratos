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

package org.apache.stratos.common.test;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.stratos.common.clustering.impl.HazelcastDistributedObjectProvider;
import org.apache.stratos.common.internal.ServiceReferenceHolder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Distributed object provider unit tests.
 */
public class DistributedObjectProviderTest {

    public static final String MAP_1 = "MAP1";
    public static final String MAP_1_WRITE_LOCK = "MAP1_WRITE_LOCK";
    public static final String LIST_1 = "LIST1";
    public static final String LIST_1_WRITE_LOCK = "LIST1_WRITE_LOCK";
    private static HazelcastInstance hazelcastInstance;

    @BeforeClass
    public static void setUpClass() {
        hazelcastInstance = Hazelcast.newHazelcastInstance();
    }

    @Test
    public void testPutToMapLocal() {
        ServiceReferenceHolder.getInstance().setHazelcastInstance(null);
        HazelcastDistributedObjectProvider provider = new HazelcastDistributedObjectProvider();
        testPutToMap(provider);
        testPutToMap(provider);
    }

    @Test
    public void testPutToMapDistributed() {
        ServiceReferenceHolder.getInstance().setHazelcastInstance(hazelcastInstance);
        HazelcastDistributedObjectProvider provider = new HazelcastDistributedObjectProvider();
        testPutToMap(provider);
        testPutToMap(provider);
    }

    private void testPutToMap(HazelcastDistributedObjectProvider provider) {
        Map<String, String> map = provider.getMap(MAP_1);
        Lock lock = null;
        try {
            lock = provider.acquireLock(MAP_1_WRITE_LOCK);
            map.put("key1", "value1");
            assertEquals(map.get("key1"), "value1");
        } finally {
            provider.releaseLock(lock);
            provider.removeMap(MAP_1);
        }
    }

    @Test
    public void testGetListLocal() {
        ServiceReferenceHolder.getInstance().setHazelcastInstance(null);
        HazelcastDistributedObjectProvider provider = new HazelcastDistributedObjectProvider();
        testAddToList(provider);
        testAddToList(provider);
    }

    @Test
    public void testGetListDistributed() {
        ServiceReferenceHolder.getInstance().setHazelcastInstance(hazelcastInstance);
        HazelcastDistributedObjectProvider provider = new HazelcastDistributedObjectProvider();
        testAddToList(provider);
        testAddToList(provider);
    }

    private void testAddToList(HazelcastDistributedObjectProvider provider) {
        List list = provider.getList(LIST_1);
        Lock lock = null;
        try {
            lock = provider.acquireLock(LIST_1_WRITE_LOCK);
            String value1 = "value1";
            list.add(value1);
            assertTrue(list.contains(value1));
        } finally {
            provider.releaseLock(lock);
            provider.removeList(LIST_1);
        }
    }
}
