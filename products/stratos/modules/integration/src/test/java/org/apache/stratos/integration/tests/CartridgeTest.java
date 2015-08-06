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

package org.apache.stratos.integration.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.PropertyBean;
import org.apache.stratos.common.beans.cartridge.CartridgeBean;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test to handle Cartridge CRUD operations
 */
public class CartridgeTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(CartridgeTest.class);
    private static final String TEST_PATH = "/cartridge-group-test";


    @Test
    public void testCartridge() {
        log.info("Started Cartridge test case**************************************");

        try {
            String cartridgeType = "c0";
            boolean added = restClient.addEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" +
                            cartridgeType + ".json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(added, true);
            CartridgeBean bean = (CartridgeBean) restClient.
                    getEntity(RestConstants.CARTRIDGES, cartridgeType,
                            CartridgeBean.class, RestConstants.CARTRIDGES_NAME);
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


            boolean updated = restClient.updateEntity(TEST_PATH + RestConstants.CARTRIDGES_PATH + "/" +
                            cartridgeType + "-v1.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(updated, true);
            CartridgeBean updatedBean = (CartridgeBean) restClient.
                    getEntity(RestConstants.CARTRIDGES, cartridgeType,
                            CartridgeBean.class, RestConstants.CARTRIDGES_NAME);
            assertEquals(updatedBean.getType(), "c0");
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

            boolean removed = restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeType,
                    RestConstants.CARTRIDGES_NAME);
            assertEquals(removed, true);

            CartridgeBean beanRemoved = (CartridgeBean) restClient.
                    getEntity(RestConstants.CARTRIDGES, cartridgeType,
                            CartridgeBean.class, RestConstants.CARTRIDGES_NAME);
            assertEquals(beanRemoved, null);

            log.info("Ended Cartridge test case**************************************");
        } catch (Exception e) {
            log.error("An error occurred while handling RESTConstants.CARTRIDGES_PATH", e);
            assertTrue("An error occurred while handling RESTConstants.CARTRIDGES_PATH", false);
        }
    }
}
