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
package org.apache.stratos.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import org.apache.stratos.cli.utils.RowMapper;
import org.apache.stratos.cli.utils.CommandLineUtils;

public class RestCommandLineService {

	private static final Logger logger = LoggerFactory.getLogger(RestCommandLineService.class);

    RestClient restClientService = new RestClient();

	RestCommandLineService() {
	}

	private static class SingletonHolder {
		private final static RestCommandLineService INSTANCE = new RestCommandLineService();
	}

	public static RestCommandLineService getInstance() {
		return SingletonHolder.INSTANCE;
	}

    public void listAvailableCartridges() throws CommandException {
        try {

            String stratosURL = System.getenv(CliConstants.STRATOS_URL_ENV_PROPERTY);
            String username = System.getenv(CliConstants.STRATOS_USERNAME_ENV_PROPERTY);
            String password = System.getenv(CliConstants.STRATOS_PASSWORD_ENV_PROPERTY);

            System.out.println(stratosURL + " : " + username + " : " + password);
            String resultString = restClientService.doGet("http://ec2-54-254-71-178.ap-southeast-1.compute.amazonaws.com:9765/stratos/admin/cartridge/list", "admin@manula.org", "admin123");
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            CartridgeList cartridgeList = gson.fromJson(resultString, CartridgeList.class);

            if (cartridgeList == null) {
                System.out.println("Object is null");
            }

            CartridgeList multiTelentCartridgeList = new CartridgeList();
            CartridgeList singleTeneCartridgetList = new CartridgeList();

            ArrayList<Cartridge> multiTenetCartridge = new ArrayList<Cartridge>();
            ArrayList<Cartridge> singleTentCartridge = new ArrayList<Cartridge>();

            for (Cartridge cartridge : cartridgeList.getCartridge()) {
                if (cartridge.isMultiTenant()) {
                    multiTenetCartridge.add(cartridge);
                }
                else {
                    singleTentCartridge.add(cartridge);
                }
            }

            multiTelentCartridgeList.setCartridge(multiTenetCartridge);
            singleTeneCartridgetList.setCartridge(singleTentCartridge);

            RowMapper<Cartridge> cartridgeMapper = new RowMapper<Cartridge>() {

                @Override
                public String[] getData(Cartridge cartridge) {
                    String[] data = new String[3];
                    data[0] = cartridge.getCartridgeType();
                    data[1] = cartridge.getDisplayName();
                    data[2] = cartridge.getVersion();
                    return data;
                }
            };

            if (multiTenetCartridge.size() == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No multi-tenant cartridges available");
                }
                System.out.println("There are no multi-tenant cartridges available");
            }
            else {
                Cartridge[] cartridges = new Cartridge[multiTelentCartridgeList.getCartridge().size()];
                cartridges = multiTelentCartridgeList.getCartridge().toArray(cartridges);

                System.out.println("Available Multi-Tenant Cartridges:");
                CommandLineUtils.printTable(cartridges, cartridgeMapper, "Type", "Name", "Version");
                System.out.println();
            }

            if (singleTentCartridge.size() == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No single-tenant cartridges available");
                }
                System.out.println("There are no single-tenant cartridges available");
            }
            else {
                Cartridge[] cartridges1 = new Cartridge[singleTeneCartridgetList.getCartridge().size()];
                cartridges1 = singleTeneCartridgetList.getCartridge().toArray(cartridges1   );

                System.out.println("Available Single-Tenant Cartridges:");
                CommandLineUtils.printTable(cartridges1, cartridgeMapper, "Type", "Name", "Version");
                System.out.println();
            }

        } catch (Exception e) {
                e.printStackTrace();
        }
    }

    private class CartridgeList  {
        private ArrayList<Cartridge> cartridge;

        public ArrayList<Cartridge> getCartridge() {
            return cartridge;
        }

        public void setCartridge(ArrayList<Cartridge> cartridge) {
            this.cartridge = cartridge;
        }

        CartridgeList() {
            cartridge = new ArrayList<Cartridge>();
        }
    }
}
