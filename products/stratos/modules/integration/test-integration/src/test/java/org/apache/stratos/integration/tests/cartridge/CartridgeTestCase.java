/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.integration.tests.cartridge;

import com.google.gson.reflect.TypeToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.PropertyBean;
import org.apache.stratos.common.beans.cartridge.CartridgeBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Test to handle Cartridge CRUD operations
 */
@Test(groups = { "cartridge" })
public class CartridgeTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(CartridgeTestCase.class);
    private static final String RESOURCES_PATH = "/cartridge-test";
    private long startTime;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT,
          priority = 1)
    public void testCartridge() throws Exception {
        log.info("Running CartridgeTestCase.testCartridge test method...");
        startTime = System.currentTimeMillis();

        String cartridgeType = "c0-cartridge-test";
        boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" +
                cartridgeType + ".json", RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(added);
        CartridgeBean bean = (CartridgeBean) restClient.
                getEntity(RestConstants.CARTRIDGES, cartridgeType, CartridgeBean.class, RestConstants.CARTRIDGES_NAME);
        assertEquals(bean.getCategory(), "Application");
        assertEquals(bean.getHost(), "qmog.cisco.com");
        for (PropertyBean property : bean.getProperty()) {
            if (property.getName().equals("payload_parameter.CEP_IP")) {
                assertEquals(property.getValue(), "octl.qmog.cisco.com");
            } else if (property.getName().equals("payload_parameter.CEP_ADMIN_PASSWORD")) {
                assertEquals(property.getValue(), "admin");
            } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_IP")) {
                assertEquals(property.getValue(), "octl.qmog.cisco.com");
            } else if (property.getName().equals("payload_parameter.QTCM_NETWORK_COUNT")) {
                assertEquals(property.getValue(), "1");
            } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_ADMIN_PASSWORD")) {
                assertEquals(property.getValue(), "admin");
            } else if (property.getName().equals("payload_parameter.QTCM_DNS_SEGMENT")) {
                assertEquals(property.getValue(), "test");
            } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_SECURE_PORT")) {
                assertEquals(property.getValue(), "7711");
            } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_PORT")) {
                assertEquals(property.getValue(), "7611");
            } else if (property.getName().equals("payload_parameter.CEP_PORT")) {
                assertEquals(property.getValue(), "7611");
            } else if (property.getName().equals("payload_parameter.MB_PORT")) {
                assertEquals(property.getValue(), "61616");
            }
        }

        boolean updated = restClient.updateEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" +
                cartridgeType + "-v1.json", RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(updated);
        CartridgeBean updatedBean = (CartridgeBean) restClient.
                getEntity(RestConstants.CARTRIDGES, cartridgeType, CartridgeBean.class, RestConstants.CARTRIDGES_NAME);
        assertEquals(updatedBean.getType(), "c0-cartridge-test");
        assertEquals(updatedBean.getCategory(), "Data");
        assertEquals(updatedBean.getHost(), "qmog.cisco.com12");
        for (PropertyBean property : updatedBean.getProperty()) {
            if (property.getName().equals("payload_parameter.CEP_IP")) {
                assertEquals(property.getValue(), "octl.qmog.cisco.com123");
            } else if (property.getName().equals("payload_parameter.CEP_ADMIN_PASSWORD")) {
                assertEquals(property.getValue(), "admin123");
            } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_IP")) {
                assertEquals(property.getValue(), "octl.qmog.cisco.com123");
            } else if (property.getName().equals("payload_parameter.QTCM_NETWORK_COUNT")) {
                assertEquals(property.getValue(), "3");
            } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_ADMIN_PASSWORD")) {
                assertEquals(property.getValue(), "admin123");
            } else if (property.getName().equals("payload_parameter.QTCM_DNS_SEGMENT")) {
                assertEquals(property.getValue(), "test123");
            } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_SECURE_PORT")) {
                assertEquals(property.getValue(), "7712");
            } else if (property.getName().equals("payload_parameter.MONITORING_SERVER_PORT")) {
                assertEquals(property.getValue(), "7612");
            } else if (property.getName().equals("payload_parameter.CEP_PORT")) {
                assertEquals(property.getValue(), "7612");
            } else if (property.getName().equals("payload_parameter.MB_PORT")) {
                assertEquals(property.getValue(), "61617");
            }
        }

        boolean removed = restClient
                .removeEntity(RestConstants.CARTRIDGES, cartridgeType, RestConstants.CARTRIDGES_NAME);
        assertTrue(removed);

        CartridgeBean beanRemoved = (CartridgeBean) restClient.
                getEntity(RestConstants.CARTRIDGES, cartridgeType, CartridgeBean.class, RestConstants.CARTRIDGES_NAME);
        assertNull(beanRemoved);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT,
          priority = 2)
    public void testCartridgeList() throws Exception {
        log.info("Running CartridgeTestCase.testCartridgeList test method...");

        String cartridgeType1 = "c1-cartridge-test";
        String cartridgeType2 = "c2-cartridge-test";
        boolean added1 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" +
                cartridgeType1 + ".json", RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(added1);

        boolean added2 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH + "/" +
                cartridgeType2 + ".json", RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
        assertTrue(added2);

        Type listType = new TypeToken<ArrayList<CartridgeBean>>() {
        }.getType();

        List<CartridgeBean> cartridgeList = (List<CartridgeBean>) restClient
                .listEntity(RestConstants.CARTRIDGES, listType, RestConstants.CARTRIDGES_NAME);
        assertTrue(cartridgeList.size() >= 2);

        CartridgeBean bean1 = null;
        for (CartridgeBean cartridgeBean : cartridgeList) {
            if (cartridgeBean.getType().equals(cartridgeType1)) {
                bean1 = cartridgeBean;
            }
        }
        assertNotNull(bean1);

        CartridgeBean bean2 = null;
        for (CartridgeBean cartridgeBean : cartridgeList) {
            if (cartridgeBean.getType().equals(cartridgeType1)) {
                bean2 = cartridgeBean;
            }
        }
        assertNotNull(bean2);

        boolean removed = restClient
                .removeEntity(RestConstants.CARTRIDGES, cartridgeType1, RestConstants.CARTRIDGES_NAME);
        assertTrue(removed);

        CartridgeBean beanRemoved = (CartridgeBean) restClient.
                getEntity(RestConstants.CARTRIDGES, cartridgeType1, CartridgeBean.class, RestConstants.CARTRIDGES_NAME);
        assertEquals(beanRemoved, null);

        removed = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeType2, RestConstants.CARTRIDGES_NAME);
        assertTrue(removed);

        beanRemoved = (CartridgeBean) restClient.
                getEntity(RestConstants.CARTRIDGES, cartridgeType2, CartridgeBean.class, RestConstants.CARTRIDGES_NAME);
        assertNull(beanRemoved);
        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("CartridgeTestCase completed in [duration] %s ms", duration));
    }
}
