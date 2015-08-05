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
import org.apache.stratos.common.beans.cartridge.CartridgeBean;
import org.apache.stratos.common.beans.cartridge.CartridgeGroupBean;
import org.apache.stratos.integration.tests.rest.RestClient;

/**
 * Test to handle Cartridge CRUD operations
 */
public class CartridgeTest {
    private static final Log log = LogFactory.getLog(CartridgeTest.class);
    private static final String cartridges = "/cartridges/mock/";
    private static final String cartridgesUpdate = "/cartridges/mock/update/";
    private static final String entityName = "cartridge";
    

    public boolean addCartridge(String cartridgeType, RestClient restClient) {
        return restClient.addEntity(cartridges + "/" + cartridgeType,
                RestConstants.CARTRIDGES, entityName);
    }

    public CartridgeBean getCartridge(String cartridgeType,
                                      RestClient restClient) {
        CartridgeBean bean = (CartridgeBean) restClient.
                getEntity(RestConstants.CARTRIDGES, cartridgeType,
                        CartridgeBean.class, entityName);
        return bean;
    }

    public boolean updateCartridge(String cartridgeType, RestClient restClient) {
        return restClient.updateEntity(cartridgesUpdate + "/" + cartridgeType,
                RestConstants.CARTRIDGES, entityName);
    }

    public boolean removeCartridge(String cartridgeType, RestClient restClient) {
        return restClient.removeEntity(RestConstants.CARTRIDGES, cartridgeType, entityName);
    }
}
