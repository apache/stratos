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

import junit.framework.TestCase;

import org.apache.stratos.common.statistics.publisher.ThriftClientConfig;
import org.apache.stratos.common.statistics.publisher.ThriftClientInfo;
import org.junit.Test;

import java.net.URL;
import java.util.List;


/**
 * Thrift Client configuration.
 */
public class ThriftClientConfigParserTest extends TestCase {
    /**
     * Test ThriftClientConfigParser parse method to check whether it
     * reads the correct xml filed values.
     *
     * @throws Exception
     */
    @Test
    public void testThriftClientConfigParser() throws Exception {
        URL configFileUrl = ThriftClientConfigParserTest.class.getResource("/thrift-client-config.xml");
        System.setProperty(ThriftClientConfig.THRIFT_CLIENT_CONFIG_FILE_PATH, configFileUrl.getPath());
        ThriftClientConfig thriftClientConfig = ThriftClientConfig.getInstance();
        List <ThriftClientInfo> cepList = thriftClientConfig.getThriftClientInfo(
                ThriftClientConfig.CEP_THRIFT_CLIENT_NAME);
        List <ThriftClientInfo> dasList = thriftClientConfig.getThriftClientInfo(
                ThriftClientConfig.DAS_THRIFT_CLIENT_NAME);
        ThriftClientInfo cepNode1 = null;
        ThriftClientInfo cepNode2 = null;
        ThriftClientInfo dasNode1 = null;
        
        for (ThriftClientInfo cepNodeInfo : cepList) {
			if(cepNodeInfo.getId().equals("node-01")) {
				cepNode1 = cepNodeInfo;
			}else if(cepNodeInfo.getId().equals("node-02")) {
				cepNode2 = cepNodeInfo;
			}
		}
                
        for (ThriftClientInfo dasNodeInfo : dasList) {
			if(dasNodeInfo.getId().equals("node-01")) {
				dasNode1 = dasNodeInfo;
			}
		}

        // CEP-node1
        assertEquals("CEP Stats Publisher not enabled",true,cepNode1.isStatsPublisherEnabled());        
        assertEquals("Incorrect Username", "admincep1", cepNode1.getUsername());
        assertEquals("Incorrect Password", "1234cep1", cepNode1.getPassword());
        assertEquals("Incorrect IP", "192.168.10.10", cepNode1.getIp());
        assertEquals("Incorrect Port", "9300", cepNode1.getPort());
        
        // CEP-node2
        assertEquals("CEP Stats Publisher not enabled",true,cepNode2.isStatsPublisherEnabled());        
        assertEquals("Incorrect Username", "admincep2", cepNode2.getUsername());
        assertEquals("Incorrect Password", "1234cep2", cepNode2.getPassword());
        assertEquals("Incorrect IP", "192.168.10.20", cepNode2.getIp());
        assertEquals("Incorrect Port", "9300", cepNode2.getPort());

        // DAS node 1
        assertEquals("DAS Stats Publisher not enabled",true, dasNode1.isStatsPublisherEnabled());
        assertEquals("Incorrect Username", "admindas1", dasNode1.getUsername());
        assertEquals("Incorrect Password", "1234das1", dasNode1.getPassword());
        assertEquals("Incorrect IP", "192.168.10.11", dasNode1.getIp());
        assertEquals("Incorrect Port", "9301", dasNode1.getPort());
       
    }
}
