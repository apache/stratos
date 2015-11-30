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

import org.apache.stratos.common.statistics.publisher.ThriftClientConfig;
import org.apache.stratos.common.statistics.publisher.ThriftStatisticsPublisher;
import org.junit.Before;
import org.junit.Test;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;

import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * ThriftStatisticsPublisherTest
 */
public class ThriftStatisticsPublisherTest {
    private StreamDefinition streamDefinition;
    private ThriftStatisticsPublisher thriftStatisticsPublisher;

    @Before
    public void setUp() throws MalformedStreamDefinitionException {
        URL configFileUrl = ThriftClientConfigParserTest.class.getResource("/thrift-client-config.xml");
        System.setProperty(ThriftClientConfig.THRIFT_CLIENT_CONFIG_FILE_PATH, configFileUrl.getPath());
        streamDefinition = new StreamDefinition("Test", "1.0.0");
    }

    /**
     * Checking whether LoadBalancingDataPublisher is created for cep according to thrift-client-config.xml
     */
    @Test
    public void createCEPLoadBalancingDataPublisher() {
        thriftStatisticsPublisher = new ThriftStatisticsPublisher(streamDefinition,
                ThriftClientConfig.CEP_THRIFT_CLIENT_NAME);
        assertEquals("CEP stats publisher is not enabled", true, thriftStatisticsPublisher.isEnabled());
        assertEquals("No of CEP nodes enabled for stats publishing is not equal to two", 2,
                thriftStatisticsPublisher.getDataPublisherHolders().size());
    }

    /**
     * Checking whether LoadBalancingDataPublisher is created for das according to thrift-client-config.xml
     */
    @Test
    public void createDASLoadBalancingDataPublisher() {
        thriftStatisticsPublisher = new ThriftStatisticsPublisher(streamDefinition,
                ThriftClientConfig.DAS_THRIFT_CLIENT_NAME);
        assertEquals("DAS stats publisher is not enabled", true, thriftStatisticsPublisher.isEnabled());
        assertEquals("More than one DAS node is enabled for stats publishing", 1,
                thriftStatisticsPublisher.getDataPublisherHolders().size());
    }
}
