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

package org.apache.stratos.adc.mgt.test;

import junit.framework.TestCase;
import org.apache.stratos.adc.mgt.dto.Policy;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.payload.Payload;
import org.apache.stratos.adc.mgt.payload.PayloadArg;
import org.apache.stratos.adc.mgt.payload.PayloadFactory;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.cloud.controller.util.xsd.CartridgeInfo;
import org.apache.stratos.cloud.controller.util.xsd.PortMapping;

import java.math.BigDecimal;

public class PayloadTest extends TestCase {

    protected void setUp() throws java.lang.Exception {

        System.setProperty(CartridgeConstants.REPO_INFO_EPR, "https://sm.stratos.com:9445/repository_info_service");
        System.setProperty(CartridgeConstants.CARTRIDGE_AGENT_EPR, "https://sm.stratos.com:9445/cartridge_agent_service");
        System.setProperty(CartridgeConstants.BAM_IP, "10.10.10.10");
        System.setProperty(CartridgeConstants.BAM_PORT, "7714");
        System.setProperty("carbon.home", "/tmp/dummy-carbon-server");
    }

    private Payload createPayload (PayloadArg payloadArg, String provider, String cartridgeType, String tenantDomain,
                                   String cartridgeAlias) {

        Payload payload = null;
        try {
            payload = PayloadFactory.getPayloadInstance(provider, cartridgeType,
                    "/tmp/" + tenantDomain + "-" + cartridgeAlias + ".zip");

        } catch (ADCException e) {
            throw new RuntimeException(e);
        }
        payload.populatePayload(payloadArg);
        try {
            payload.createPayload();

        } catch (ADCException e) {
            throw new RuntimeException(e);
        }

        return payload;
    }

    private PayloadArg createBasePayloadArg (CartridgeInfo cartridgeInfo, boolean isMultitenant, String alias,
                                             String type) {

        PayloadArg payloadArg = new PayloadArg();
        payloadArg.setCartridgeInfo(cartridgeInfo);
        payloadArg.setMultitenant(isMultitenant);
        payloadArg.setTenantId(1);
        payloadArg.setTenantDomain("foo.com");
        payloadArg.setCartridgeAlias(alias);
        payloadArg.setServiceName(type);

        return payloadArg;
    }

    public void testMultitenantCarbonPayload() {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setMultiTenant(true);
        cartridgeInfo.setType("esb");
        PayloadArg payloadArg = createBasePayloadArg(cartridgeInfo, true, "carbon1", "esb");
        payloadArg.setDeployment("default");
        payloadArg.setHostName("esb.test.com");
        payloadArg.setServiceDomain("esb.domain");
        payloadArg.setServiceDomain("__$default");
        Payload payload = createPayload(payloadArg, "wso2", "esb", "foo.com", "carbon1");
        assertNotNull(payload);
        //assertTrue(payload.delete());
    }

    public void testSingleTenantCarbonPayload() {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setMultiTenant(false);
        cartridgeInfo.setType("as");
        PayloadArg payloadArg = createBasePayloadArg(cartridgeInfo, false, "carbon2", "as");
        payloadArg.setDeployment("default");
        payloadArg.setHostName("as.test.com");
        payloadArg.setServiceDomain("as.domain");
        payloadArg.setServiceDomain("__$default");
        Payload payload = createPayload(payloadArg, "wso2", "as", "foo.com", "carbon2");
        assertNotNull(payload);
        //assertTrue(payload.delete());
    }

    public void testNonCarbonPayload() {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setType("php");
        //http port mapping
        PortMapping httpPortMapping = new PortMapping();
        httpPortMapping.setProtocol("http");
        httpPortMapping.setPort("80");
        httpPortMapping.setProxyPort("8280");
        //https port mapping
        PortMapping httpsPortMapping = new PortMapping();
        httpsPortMapping.setProtocol("https");
        httpsPortMapping.setPort("443");
        httpsPortMapping.setProxyPort("8243");

        PortMapping[] portMappings = new PortMapping[]{httpPortMapping, httpsPortMapping};
        cartridgeInfo.setPortMappings(portMappings);

        //auto scaling parameters
        Policy autoScalePolicy = new Policy();
        autoScalePolicy.setMinAppInstances(1);
        autoScalePolicy.setMaxAppInstances(2);
        autoScalePolicy.setAlarmingLowerRate(BigDecimal.valueOf(0.2));
        autoScalePolicy.setAlarmingUpperRate(BigDecimal.valueOf(0.7));
        autoScalePolicy.setMaxRequestsPerSecond(5);
        autoScalePolicy.setScaleDownFactor(BigDecimal.valueOf(0.25));
        autoScalePolicy.setRoundsToAverage(2);

        PayloadArg payloadArg = createBasePayloadArg(cartridgeInfo, false, "php1", "php");
        payloadArg.setPolicy(autoScalePolicy);
        payloadArg.setHostName("php.test.com");
        payloadArg.setServiceDomain("php.domain");
        payloadArg.setServiceDomain("__$default");
        Payload payload = createPayload(payloadArg, "php-provider", "php", "foo.com", "php1");
        assertNotNull(payload);
        //assertTrue(payload.delete());
    }

    public void testDataPayload() {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setType("php");
        //http port mapping
        PortMapping httpPortMapping = new PortMapping();
        httpPortMapping.setProtocol("http");
        httpPortMapping.setPort("80");
        httpPortMapping.setProxyPort("8280");
        //https port mapping
        PortMapping httpsPortMapping = new PortMapping();
        httpsPortMapping.setProtocol("https");
        httpsPortMapping.setPort("443");
        httpsPortMapping.setProxyPort("8243");

        PortMapping[] portMappings = new PortMapping[]{httpPortMapping, httpsPortMapping};
        cartridgeInfo.setPortMappings(portMappings);

        //auto scaling parameters
        Policy autoScalePolicy = new Policy();
        autoScalePolicy.setMinAppInstances(1);
        autoScalePolicy.setMaxAppInstances(2);
        autoScalePolicy.setAlarmingLowerRate(BigDecimal.valueOf(0.2));
        autoScalePolicy.setAlarmingUpperRate(BigDecimal.valueOf(0.7));
        autoScalePolicy.setMaxRequestsPerSecond(5);
        autoScalePolicy.setScaleDownFactor(BigDecimal.valueOf(0.25));
        autoScalePolicy.setRoundsToAverage(2);

        PayloadArg payloadArg = createBasePayloadArg(cartridgeInfo, false, "mysql1", "mysql");
        payloadArg.setPolicy(autoScalePolicy);
        payloadArg.setHostName("mysql.test.com");
        payloadArg.setServiceDomain("mysql.domain");
        payloadArg.setServiceDomain("__$default");
        Payload payload = createPayload(payloadArg, "mysql-provider", "mysql", "foo.com", "mysql1");
        assertNotNull(payload);
        //assertTrue(payload.delete());
    }

    protected void tearDown() throws java.lang.Exception {

        System.clearProperty(CartridgeConstants.REPO_INFO_EPR);
        System.clearProperty(CartridgeConstants.CARTRIDGE_AGENT_EPR);
        System.clearProperty(CartridgeConstants.BAM_IP);
        System.clearProperty(CartridgeConstants.BAM_PORT);
        System.clearProperty("carbon.home");
    }
}
