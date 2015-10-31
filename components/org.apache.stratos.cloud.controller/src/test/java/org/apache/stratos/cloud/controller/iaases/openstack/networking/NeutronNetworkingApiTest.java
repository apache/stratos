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
package org.apache.stratos.cloud.controller.iaases.openstack.networking;

import junit.framework.TestCase;
import org.apache.stratos.cloud.controller.domain.FloatingNetwork;
import org.apache.stratos.cloud.controller.domain.FloatingNetworks;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.NetworkInterface;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class NeutronNetworkingApiTest extends TestCase {

    private IaasProvider iaasProvider;
    private NeutronNetworkingApi neutronNetworkingApi;
    private NetworkInterface[] networkInterfaces;
    private String validationError;

    @Before
    public void setUp() {
        iaasProvider = new IaasProvider();
        neutronNetworkingApi = new NeutronNetworkingApi(iaasProvider);
        validationError = "Neutron Networking Api validation failed";

        FloatingNetwork floatingNetwork_1 = new FloatingNetwork();
        floatingNetwork_1.setName("externalOne");
        floatingNetwork_1.setNetworkUuid("ba667f72-7ba8-4b24-b360-b74a0211c83c");

        FloatingNetwork floatingNetwork_2 = new FloatingNetwork();
        floatingNetwork_2.setName("externalTwo");
        floatingNetwork_2.setFloatingIP("192.168.16.59");

        FloatingNetwork floatingNetwork_3 = new FloatingNetwork();
        floatingNetwork_3.setName("externalThree");
        floatingNetwork_3.setNetworkUuid("er667f72-7ba8-4b24-b360-b74a0211c83c");
        floatingNetwork_3.setFloatingIP("192.165.198.12");

        FloatingNetwork floatingNetwork_4 = new FloatingNetwork();
        floatingNetwork_4.setName("externalFour");
        floatingNetwork_4.setNetworkUuid("b3607ba8-7ba8-4b24-b360-b74a0211c83c");

        FloatingNetworks floatingNetworks_1 = new FloatingNetworks();
        floatingNetworks_1.setFloatingNetworks(new FloatingNetwork[]{floatingNetwork_1});

        FloatingNetworks floatingNetworks_2 = new FloatingNetworks();
        floatingNetworks_2.setFloatingNetworks(new FloatingNetwork[]{floatingNetwork_2, floatingNetwork_4});

        FloatingNetworks floatingNetworks_3 = new FloatingNetworks();
        floatingNetworks_3.setFloatingNetworks(new FloatingNetwork[]{floatingNetwork_1, floatingNetwork_4});

        FloatingNetworks floatingNetworks_4 = new FloatingNetworks();
        floatingNetworks_4.setFloatingNetworks(new FloatingNetwork[]{floatingNetwork_1, floatingNetwork_3,
                floatingNetwork_4});

        NetworkInterface networkInterface_1 = new NetworkInterface();
        networkInterface_1.setNetworkUuid("512e1f54-1e85-4dac-b2e6-f0b30fc552cf");
        networkInterface_1.setFloatingNetworks(floatingNetworks_1);

        NetworkInterface networkInterface_2 = new NetworkInterface();
        networkInterface_2.setNetworkUuid("68aab21d-fc9a-4c2f-8d15-b1e41f6f7bb8");
        networkInterface_2.setFloatingNetworks(floatingNetworks_2);

        NetworkInterface networkInterface_3 = new NetworkInterface();
        networkInterface_3.setNetworkUuid("b55f009a-1cc6-4b17-924f-4ae0ee18db5e");
        networkInterface_3.setFloatingNetworks(floatingNetworks_3);

        NetworkInterface networkInterface_4 = new NetworkInterface();
        networkInterface_4.setPortUuid("d343d343-1cc6-4b17-924f-4ae0ee18db5e");
        networkInterface_4.setFixedIp("10.5.62.3");
        networkInterface_4.setFloatingNetworks(floatingNetworks_4);

        networkInterfaces = new NetworkInterface[]{networkInterface_1, networkInterface_2, networkInterface_3,
                networkInterface_4};
    }

    @Test
    public void testGetAllPredefinedFloatingIPs() throws Exception {
        List<String> allPredefinedFloatingIPs = neutronNetworkingApi.getAllPredefinedFloatingIPs(networkInterfaces);
        Assert.assertEquals(String.format("%s. Predefined floating IP count not valid", validationError), 1,
                allPredefinedFloatingIPs.size());
        Assert.assertTrue(String.format("%s. Predefined floating IP not returned", validationError),
                allPredefinedFloatingIPs.contains("192.168.16.59"));
    }

    @Test
    public void testGetNetworkUuidToFloatingNetworksMap() throws Exception {
        Map<String, List<FloatingNetwork>> networkInterfaceToFloatingNetworksMap =
                neutronNetworkingApi.getNetworkUuidToFloatingNetworksMap(networkInterfaces);

        Assert.assertEquals(String.format("%s. Network interfaces count not valid", validationError), 3,
                networkInterfaceToFloatingNetworksMap.size());
    }

    @Test
    public void testGetFixedIPToFloatingNetworksMap() throws Exception {
        Map<String, List<FloatingNetwork>> fixedIPToFloatingNetworksMap =
                neutronNetworkingApi.getFixedIPToFloatingNetworksMap(networkInterfaces);

        Assert.assertEquals(String.format("%s. Network interfaces count not valid", validationError), 1,
                fixedIPToFloatingNetworksMap.size());
    }
}