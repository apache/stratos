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
import org.apache.stratos.common.clustering.DistributedObjectProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Distributed object provider unit tests.
 */
public class DistributedObjectProviderTest {

    private static HazelcastInstance hazelcastInstance;

    @BeforeClass
    public static void setUpClass() {
        hazelcastInstance = Hazelcast.newHazelcastInstance();
    }

    @Test
    public void testPutToMapLocal() {
        DistributedObjectProvider provider = new DistributedObjectProvider(false, null);
        testPutToMap(provider);
    }

    @Test
    public void testPutToMapDistributed() {
        DistributedObjectProvider provider = new DistributedObjectProvider(true, hazelcastInstance);
        testPutToMap(provider);
    }

    private void testPutToMap(DistributedObjectProvider provider) {
        Map<String, String> map = provider.getMap("MAP1");
        provider.putToMap(map, "key1", "value1");
        assertEquals(map.get("key1"), "value1");
    }

    @Test
    public void testRemoveFromMapLocal() {
        DistributedObjectProvider provider = new DistributedObjectProvider(false, null);
        testRemoveFromMap(provider);
    }

    @Test
    public void testRemoveFromMapDistributed() {
        DistributedObjectProvider provider = new DistributedObjectProvider(true, hazelcastInstance);
        testRemoveFromMap(provider);
    }

    private void testRemoveFromMap(DistributedObjectProvider provider) {
        Map<String, String> map = provider.getMap("MAP1");
        provider.putToMap(map, "key1", "value1");
        assertEquals(map.get("key1"), "value1");
        provider.removeFromMap(map, "key1");
        assertNull(map.get("key1"));
    }

    @Test
    public void testAddToListLocal() {
        DistributedObjectProvider provider = new DistributedObjectProvider(false, null);
        testAddToList(provider);
    }

    @Test
    public void testAddToListDistributed() {
        DistributedObjectProvider provider = new DistributedObjectProvider(true, hazelcastInstance);
        testAddToList(provider);
    }

    private void testAddToList(DistributedObjectProvider provider) {
        List list = provider.getList("LIST1");
        String value1 = "value1";
        provider.addToList(list, value1);
        assertTrue(list.contains(value1));
    }

    @Test
    public void testRemoveFromListLocal() {
        DistributedObjectProvider provider = new DistributedObjectProvider(false, null);
        testRemovalFromList(provider);
    }

    @Test
    public void testRemoveFromListDistributed() {
        DistributedObjectProvider provider = new DistributedObjectProvider(true, hazelcastInstance);
        testRemovalFromList(provider);
    }

    private void testRemovalFromList(DistributedObjectProvider provider) {
        List list = provider.getList("LIST1");
        String value1 = "value1";
        provider.addToList(list, value1);
        assertTrue(list.contains(value1));
        provider.removeFromList(list, value1);
        assertFalse(list.contains(value1));
    }
}
