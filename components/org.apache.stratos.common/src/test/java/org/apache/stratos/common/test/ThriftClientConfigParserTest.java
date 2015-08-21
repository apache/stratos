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
        ThriftClientInfo cepThriftClientInfo = thriftClientConfig.getThriftClientInfo(
                ThriftClientConfig.CEP_THRIFT_CLIENT_NAME);
        ThriftClientInfo dasThriftClientInfo = thriftClientConfig.getThriftClientInfo(
                ThriftClientConfig.DAS_THRIFT_CLIENT_NAME);

        assertEquals("Incorrect Username", "admin", cepThriftClientInfo.getUsername());
        assertEquals("Incorrect Password", "1234", cepThriftClientInfo.getPassword());
        assertEquals("Incorrect IP", "192.168.10.10", cepThriftClientInfo.getIp());
        assertEquals("Incorrect Port", "9300", cepThriftClientInfo.getPort());

        assertEquals("Incorrect Username", "admin1", dasThriftClientInfo.getUsername());
        assertEquals("Incorrect Password", "12345", dasThriftClientInfo.getPassword());
        assertEquals("Incorrect IP", "192.168.10.11", dasThriftClientInfo.getIp());
        assertEquals("Incorrect Port", "9301", dasThriftClientInfo.getPort());
    }
}
