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

package org.apache.stratos.integration.tests.group;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.cartridge.CartridgeGroupBean;
import org.apache.stratos.integration.tests.RestConstants;
import org.apache.stratos.integration.tests.StratosTestServerManager;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test to handle Cartridge group CRUD operations
 */
public class CartridgeGroupTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(CartridgeGroupTest.class);
    private static final String RESOURCES_PATH = "/cartridge-group-test";

    @Test
    public void testCartridgeGroup() {
        try {
            log.info("-------------------------Started Cartridge group test case-------------------------");

            boolean addedC1 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH +
                            "/" + "c4-cartridge-group-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(String.format("Cartridge did not added: [cartridge-name] %s",
                    "c4-cartridge-group-test"), addedC1, true);

            boolean addedC2 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH +
                            "/" + "c5-cartridge-group-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(String.format("Cartridge did not added: [cartridge-name] %s",
                    "c5-cartridge-group-test"), addedC2, true);

            boolean addedC3 = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGES_PATH +
                            "/" + "c6-cartridge-group-test.json",
                    RestConstants.CARTRIDGES, RestConstants.CARTRIDGES_NAME);
            assertEquals(String.format("Cartridge did not added: [cartridge-name] %s",
                    "c6-cartridge-group-test"), addedC3, true);

            boolean added = restClient.addEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                            "/" + "g4-g5-g6-cartridge-group-test.json", RestConstants.CARTRIDGE_GROUPS,
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Cartridge Group did not added: [cartridge-group-name] %s",
                    "g4-g5-g6-cartridge-group-test"), added, true);

            CartridgeGroupBean bean = (CartridgeGroupBean) restClient.
                    getEntity(RestConstants.CARTRIDGE_GROUPS, "G4-cartridge-group-test",
                            CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Cartridge Group name did not match: [cartridge-group-name] %s",
                    "g4-g5-g6-cartridge-group-test.json"), bean.getName(), "G4-cartridge-group-test");

            boolean updated = restClient.updateEntity(RESOURCES_PATH + RestConstants.CARTRIDGE_GROUPS_PATH +
                            "/" + "g4-g5-g6-cartridge-group-test-v1.json",
                    RestConstants.CARTRIDGE_GROUPS, RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Cartridge Group did not updated: [cartridge-group-name] %s",
                    "g4-g5-g6-cartridge-group-test"), updated, true);

            CartridgeGroupBean updatedBean = (CartridgeGroupBean) restClient.
                    getEntity(RestConstants.CARTRIDGE_GROUPS, "G4-cartridge-group-test",
                            CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Updated Cartridge Group didn't match: [cartridge-group-name] %s",
                    "g4-g5-g6-cartridge-group-test"), updatedBean.getName(), "G4-cartridge-group-test");

            boolean removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "c4-cartridge-group-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Cartridge can be removed while it is used in " +
                    "cartridge group: [cartridge-name] %s", "c4-cartridge-group-test"), removedC1, false);

            boolean removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES, "c5-cartridge-group-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Cartridge can be removed while it is used in " +
                            "cartridge group: [cartridge-name] %s",
                    "c5-cartridge-group-test"), removedC2, false);

            boolean removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, "c6-cartridge-group-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Cartridge can be removed while it is used in " +
                            "cartridge group: [cartridge-name] %s",
                    "c6-cartridge-group-test"), removedC3, false);

            boolean removed = restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, "G4-cartridge-group-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Cartridge Group did not removed: [cartridge-group-name] %s",
                    "g4-g5-g6-cartridge-group-test"), removed, true);

            CartridgeGroupBean beanRemoved = (CartridgeGroupBean) restClient.
                    getEntity(RestConstants.CARTRIDGE_GROUPS, "G4-cartridge-group-test",
                            CartridgeGroupBean.class, RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Cartridge Group did not removed completely: " +
                            "[cartridge-group-name] %s",
                    "g4-g5-g6-cartridge-group-test"), beanRemoved, null);

            removedC1 = restClient.removeEntity(RestConstants.CARTRIDGES, "c4-cartridge-group-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Cartridge can not be removed : [cartridge-name] %s",
                    "c4-cartridge-group-test"), removedC1, true);

            removedC2 = restClient.removeEntity(RestConstants.CARTRIDGES, "c5-cartridge-group-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Cartridge can not be removed : [cartridge-name] %s",
                    "c5-cartridge-group-test"), removedC2, true);

            removedC3 = restClient.removeEntity(RestConstants.CARTRIDGES, "c6-cartridge-group-test",
                    RestConstants.CARTRIDGE_GROUPS_NAME);
            assertEquals(String.format("Cartridge can not be removed : [cartridge-name] %s",
                    "c6-cartridge-group-test"), removedC3, true);

            log.info("-------------------------Ended Cartridge group test case-------------------------");

        } catch (Exception e) {
            log.error("An error occurred while handling Cartridge group test case", e);
            assertTrue("An error occurred while handling Cartridge group test case", false);
        }
    }
}
