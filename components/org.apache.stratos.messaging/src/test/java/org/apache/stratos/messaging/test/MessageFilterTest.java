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

import org.apache.stratos.messaging.message.filter.MessageFilter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collection;

/**
 * Message filter tests.
 */
@RunWith(JUnit4.class)
public class MessageFilterTest {

    @Test
    public final void testFilterIncluded() {
        String filterName = "filter1";
        String validationError = "MessageFilter.included() method failed";
        System.setProperty(filterName,  "property1=value1,value2 | property2=value3,value4");
        MessageFilter messageFilter = new MessageFilter(filterName);
        Assert.assertTrue(validationError, messageFilter.included("property1", "value1"));
        Assert.assertTrue(validationError, messageFilter.included("property1", "value2"));
        Assert.assertTrue(validationError, messageFilter.included("property2", "value3"));
        Assert.assertTrue(validationError, messageFilter.included("property2", "value4"));
        System.setProperty(filterName, "");
    }

    @Test
    public final void testFilterExcluded() {
        String filterName = "filter2";
        String validationError = "MessageFilter.excluded() method failed";
        System.setProperty(filterName,  "property1=value1,value2 | property2=value3,value4");
        MessageFilter messageFilter = new MessageFilter(filterName);
        Assert.assertFalse(validationError, messageFilter.excluded("property1", "value1"));
        Assert.assertFalse(validationError, messageFilter.excluded("property1", "value2"));
        Assert.assertFalse(validationError, messageFilter.excluded("property2", "value3"));
        Assert.assertFalse(validationError, messageFilter.excluded("property2", "value4"));
        System.setProperty(filterName, "");
    }

    @Test
    public final void testFilterGetAllPropertyValues() {
        String filterName = "filter2";
        String validationError = "MessageFilter.getIncludedPropertyValues() method failed";
        System.setProperty(filterName,  "property1=value1,value2 | property2=value3,value4");
        MessageFilter messageFilter = new MessageFilter(filterName);

        Collection<String> property1Values = messageFilter.getIncludedPropertyValues("property1");
        Assert.assertTrue(validationError, property1Values.contains("value1"));
        Assert.assertTrue(validationError, property1Values.contains("value2"));

        Collection<String> property2Values = messageFilter.getIncludedPropertyValues("property2");
        Assert.assertTrue(validationError, property2Values.contains("value3"));
        Assert.assertTrue(validationError, property2Values.contains("value4"));
        System.setProperty(filterName, "");
    }
}
