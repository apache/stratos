/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.messaging.test;

import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.message.filter.MessageFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyClusterFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyMemberFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyServiceFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Message filter tests.
 */
@RunWith(JUnit4.class)
public class MessageFilterTest {

    @Test
    public final void testFilterIncluded() {
        String filterName = "filter1";
        String validationError = "MessageFilter.included() method failed";
        System.setProperty(filterName,  "property1=value1,value2|property2=value3,value4");
        MessageFilter messageFilter = new MessageFilter(filterName);
        assertTrue(validationError, messageFilter.included("property1", "value1"));
        assertTrue(validationError, messageFilter.included("property1", "value2"));
        assertTrue(validationError, messageFilter.included("property2", "value3"));
        assertTrue(validationError, messageFilter.included("property2", "value4"));
        System.setProperty(filterName, "");
    }

    @Test
    public final void testFilterExcluded() {
        String filterName = "filter2";
        String validationError = "MessageFilter.excluded() method failed";
        System.setProperty(filterName,  "property1=value1,value2|property2=value3,value4");
        MessageFilter messageFilter = new MessageFilter(filterName);
        assertFalse(validationError, messageFilter.excluded("property1", "value1"));
        assertFalse(validationError, messageFilter.excluded("property1", "value2"));
        assertFalse(validationError, messageFilter.excluded("property2", "value3"));
        assertFalse(validationError, messageFilter.excluded("property2", "value4"));
        System.setProperty(filterName, "");
    }

    @Test
    public final void testFilterGetAllPropertyValues() {
        String filterName = "filter2";
        String validationError = "MessageFilter.getIncludedPropertyValues() method failed";
        System.setProperty(filterName,  "property1=value1,value2|property2=value3,value4");
        MessageFilter messageFilter = new MessageFilter(filterName);

        Collection<String> property1Values = messageFilter.getIncludedPropertyValues("property1");
        assertTrue(validationError, property1Values.contains("value1"));
        assertTrue(validationError, property1Values.contains("value2"));

        Collection<String> property2Values = messageFilter.getIncludedPropertyValues("property2");
        assertTrue(validationError, property2Values.contains("value3"));
        assertTrue(validationError, property2Values.contains("value4"));
        System.setProperty(filterName, "");
    }

    @Test
    public final void testServiceFilter() {
        System.setProperty(StratosConstants.TOPOLOGY_SERVICE_FILTER,
                TopologyServiceFilter.TOPOLOGY_SERVICE_FILTER_SERVICE_NAME + "=service1,service2");

        assertFalse(TopologyServiceFilter.apply("service1"));
        assertFalse(TopologyServiceFilter.apply("service2"));
        assertTrue(TopologyServiceFilter.apply("service3"));
    }

    @Test
    public final void testClusterFilter() {
        System.setProperty(StratosConstants.TOPOLOGY_CLUSTER_FILTER,
                TopologyClusterFilter.TOPOLOGY_CLUSTER_FILTER_CLUSTER_ID + "=cluster1,cluster2");

        assertFalse(TopologyClusterFilter.apply("cluster1"));
        assertFalse(TopologyClusterFilter.apply("cluster2"));
        assertTrue(TopologyClusterFilter.apply("cluster3"));
    }

    @Test
    public final void testMemberFilter() {
        System.setProperty(StratosConstants.TOPOLOGY_MEMBER_FILTER,
                TopologyMemberFilter.TOPOLOGY_MEMBER_FILTER_LB_CLUSTER_ID + "=lb-cluster1,lb-cluster2|" +
                        TopologyMemberFilter.TOPOLOGY_MEMBER_FILTER_NETWORK_PARTITION_ID + "=np1,np2");

        assertFalse(TopologyMemberFilter.apply("lb-cluster1", null));
        assertFalse(TopologyMemberFilter.apply("lb-cluster2", null));
        assertTrue(TopologyMemberFilter.apply("lb-cluster3", null));

        assertFalse(TopologyMemberFilter.apply(null, "np1"));
        assertFalse(TopologyMemberFilter.apply(null, "np2"));
        assertTrue(TopologyMemberFilter.apply(null, "np3"));

        assertFalse(TopologyMemberFilter.apply("lb-cluster1", "np1"));
        assertFalse(TopologyMemberFilter.apply("lb-cluster2", "np2"));
        assertTrue(TopologyMemberFilter.apply("lb-cluster3", "np3"));
    }
}
