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
import org.apache.stratos.common.beans.cartridge.CartridgeGroupBean;
import org.apache.stratos.integration.tests.rest.RestClient;

/**
 * Test to handle Cartridge group CRUD operations
 */
public class CartridgeGroupTest {
    private static final Log log = LogFactory.getLog(CartridgeGroupTest.class);
    private static final String cartridgeGroups = "/cartridges-groups/";
    private static final String cartridgeGroupsUpdate = "/cartridges-groups/update/";
    private static final String entityName = "cartridgeGroup";

    public boolean addCartridgeGroup(String groupName, RestClient restClient) {
        return restClient.addEntity(cartridgeGroups + "/" + groupName,
                RestConstants.CARTRIDGE_GROUPS, entityName);
    }

    public CartridgeGroupBean getCartridgeGroup(String groupName, RestClient restClient) {
        CartridgeGroupBean bean = (CartridgeGroupBean) restClient.
                getEntity(RestConstants.CARTRIDGE_GROUPS, groupName,
                        CartridgeGroupBean.class, entityName);
        return bean;
    }

    public boolean updateCartridgeGroup(String groupName, RestClient restClient) {
        return restClient.updateEntity(cartridgeGroupsUpdate + "/" + groupName,
                RestConstants.CARTRIDGE_GROUPS, entityName);
    }

    public boolean removeCartridgeGroup(String groupName, RestClient restClient) {
        return restClient.removeEntity(RestConstants.CARTRIDGE_GROUPS, groupName, entityName);

    }
}
