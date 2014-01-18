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

package org.apache.stratos.manager.test;

import junit.framework.TestCase;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.PersistenceManagerException;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.factory.CartridgeSubscriptionFactory;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionMultiTenantBehaviour;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionSingleTenantBehaviour;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collection;

public class LookupDataHolderTest extends TestCase  {

    Collection<CartridgeSubscription> cartridgeSubscriptions ;
    DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager;

    @Before
    public void setUp () {
        cartridgeSubscriptions = new ArrayList<CartridgeSubscription>();
        dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();

        setUpCsrtridgeSubscriptions();
    }

    private void setUpCsrtridgeSubscriptions() {

        CartridgeInfo cartridgeInfo1 = getCartridgeInfo("carbon", "esb", true);
        CartridgeSubscription cartridgeSubscription1 = getCartridgeInstance(cartridgeInfo1);
        assertNotNull(cartridgeSubscription1);
        Subscriber subscriber1 = getSubscriber("admin", 1, "a.com");
        cartridgeSubscription1.setSubscriber(subscriber1);
        cartridgeSubscription1.setClusterDomain("esb.domain");
        cartridgeSubscription1.setAlias("esba");
        cartridgeSubscriptions.add(cartridgeSubscription1);

        CartridgeInfo cartridgeInfo2 = getCartridgeInfo("carbon", "esb", true);
        CartridgeSubscription cartridgeSubscription2 = getCartridgeInstance(cartridgeInfo2);
        assertNotNull(cartridgeSubscription2);
        Subscriber subscriber2 = getSubscriber("admin", 2, "b.com");
        cartridgeSubscription2.setSubscriber(subscriber2);
        cartridgeSubscription2.setClusterDomain("esb.domain");
        cartridgeSubscription2.setAlias("esbb");
        cartridgeSubscriptions.add(cartridgeSubscription2);

        CartridgeInfo cartridgeInfo3 = getCartridgeInfo("carbon", "esb.privatejet", false);
        CartridgeSubscription cartridgeSubscription3 = getCartridgeInstance(cartridgeInfo3);
        assertNotNull(cartridgeSubscription3);
        Subscriber subscriber3 = getSubscriber("admin", 1, "a.com");
        cartridgeSubscription3.setSubscriber(subscriber3);
        cartridgeSubscription3.setClusterDomain("a.esb.domain");
        cartridgeSubscription3.setAlias("esba1");
        cartridgeSubscriptions.add(cartridgeSubscription3);

        CartridgeInfo cartridgeInfo4 = getCartridgeInfo("php-provider", "php", false);
        CartridgeSubscription cartridgeSubscription4 = getCartridgeInstance(cartridgeInfo4);
        assertNotNull(cartridgeSubscription4);
        Subscriber subscriber4 = getSubscriber("admin", 3, "c.com");
        cartridgeSubscription4.setSubscriber(subscriber4);
        cartridgeSubscription4.setClusterDomain("a.php.domain");
        cartridgeSubscription4.setAlias("phpa");
        cartridgeSubscriptions.add(cartridgeSubscription4);

        CartridgeInfo cartridgeInfo5 = getCartridgeInfo("mysql-provider", "mysql", false);
        CartridgeSubscription cartridgeSubscription5 = getCartridgeInstance(cartridgeInfo5);
        assertNotNull(cartridgeSubscription5);
        Subscriber subscriber5 = getSubscriber("admin", 3, "c.com");
        cartridgeSubscription5.setSubscriber(subscriber5);
        cartridgeSubscription5.setClusterDomain("a.mysql.domain");
        cartridgeSubscription5.setAlias("mysqla");
        cartridgeSubscriptions.add(cartridgeSubscription5);

        dataInsertionAndRetrievalManager.cacheSubscriptionsWithoutPersisting(cartridgeSubscriptions);
    }

    private CartridgeInfo getCartridgeInfo (String provider, String type, boolean multitenant) {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setProvider(provider);
        cartridgeInfo.setType(type);
        cartridgeInfo.setMultiTenant(multitenant);
        return cartridgeInfo;
    }

    private Subscriber getSubscriber (String adminUser, int tenantId, String tenantDomain) {
        return new Subscriber(adminUser, tenantId, tenantDomain);
    }

    private CartridgeSubscription getCartridgeInstance (CartridgeInfo cartridgeInfo) {

        SubscriptionTenancyBehaviour tenancyBehaviour;
        if(cartridgeInfo.getMultiTenant()) {
            tenancyBehaviour = new SubscriptionMultiTenantBehaviour();
        } else {
            tenancyBehaviour = new SubscriptionSingleTenantBehaviour();
        }

        try {
            return CartridgeSubscriptionFactory.getCartridgeSubscriptionInstance(cartridgeInfo, tenancyBehaviour);

        } catch (ADCException e) {
            throw new RuntimeException(e);
        }
    }

    public void testClusterIdToSubscription () throws PersistenceManagerException {

        // check for tenant 1
        Collection<CartridgeSubscription> cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions(1);
        assertEquals(2, cartridgeSubscriptions.size());

        // check for tenant 2
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions(2);
        assertEquals(1, cartridgeSubscriptions.size());

        // check for tenant 3
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions(3);
        assertEquals(2, cartridgeSubscriptions.size());

        // check for type 'esb'
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions("esb");
        assertEquals(2, cartridgeSubscriptions.size());

        // check for type 'esb.privatejet'
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions("esb.privatejet");
        assertEquals(1, cartridgeSubscriptions.size());

        // check for type 'php'
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions("php");
        assertEquals(1, cartridgeSubscriptions.size());

        // check for type 'mysql'
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions("mysql");
        assertEquals(1, cartridgeSubscriptions.size());

        // check for type 'esb' and tenant 1
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions(1, "esb");
        assertEquals(1, cartridgeSubscriptions.size());

        // check for type 'esb' and tenant 2
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions(2, "esb");
        assertEquals(1, cartridgeSubscriptions.size());

        // check for type 'esb.privatejet' and tenant 1
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions(1, "esb.privatejet");
        assertEquals(1, cartridgeSubscriptions.size());

        // check for type 'esb.privatejet' and tenant 2
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions(2, "esb.privatejet");
        assertNull(cartridgeSubscriptions);

        // check for type 'php' and tenant 1
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions(1, "php");
        assertNull(cartridgeSubscriptions);

        // check for type 'mysql' and tenant 3
        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions(3, "mysql");
        assertEquals(1, cartridgeSubscriptions.size());

        // check for tenant 1 and alias 'esba'
        CartridgeSubscription subscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(1, "esba");
        assertNotNull(subscription);

        // check for tenant 1 and alias 'esbb'
        subscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(1, "esbb");
        assertNull(subscription);

        // check for tenant 2 and alias 'esba'
        subscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(2, "esba");
        assertNull(subscription);

        // check for tenant 2 and alias 'esbb'
        subscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(2, "esbb");
        assertNotNull(subscription);

        // check for tenant 1 and alias 'esba1'
        subscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(1, "esba1");
        assertNotNull(subscription);

        // check for tenant 2 and alias 'esba1'
        subscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(2, "esba1");
        assertNull(subscription);

        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptionForCluster("esb.domain");
        assertNotNull(cartridgeSubscriptions);
        assertEquals(2, cartridgeSubscriptions.size());
        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
            assertTrue(cartridgeSubscription.getAlias().equals("esba") || cartridgeSubscription.getAlias().equals("esbb"));
        }

        dataInsertionAndRetrievalManager.removeSubscriptionFromCache(1, "esba");

        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptionForCluster("esb.domain");
        assertNotNull(cartridgeSubscriptions);
        assertEquals(1, cartridgeSubscriptions.size());
        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
            assertTrue(cartridgeSubscription.getAlias().equals("esbb"));
        }

        dataInsertionAndRetrievalManager.removeSubscriptionFromCache(2, "esbb");
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptionForCluster("esb.domain"));

        cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptionForCluster("a.esb.domain");
        assertNotNull(cartridgeSubscriptions);
        assertEquals(1, cartridgeSubscriptions.size());
        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
            assertTrue(cartridgeSubscription.getAlias().equals("esba1"));
        }

        dataInsertionAndRetrievalManager.removeSubscriptionFromCache(1, "esba1");
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptionForCluster("a.esb.domain"));

        // check for tenant 3 and alias 'phpa'
        subscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(3, "phpa");
        assertNotNull(subscription);
        assertTrue(subscription.getAlias().equals("phpa"));

        dataInsertionAndRetrievalManager.removeSubscriptionFromCache(3, "phpa");
        subscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(3, "phpa");
        assertNull(subscription);

        // check for tenant 3 and alias 'mysqla'
        subscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(3, "mysqla");
        assertNotNull(subscription);
        assertTrue(subscription.getAlias().equals("mysqla"));

        dataInsertionAndRetrievalManager.removeSubscriptionFromCache(3, "mysqla");
        subscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(3, "mysqla");
        assertNull(subscription);

        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptions(1));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptions(2));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptions(3));

        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptionForCluster("esb.domain"));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptionForCluster("a.esb.domain"));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptionForCluster("a.php.domain"));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptionForCluster("a.mysql.domain"));

        assertTrue(dataInsertionAndRetrievalManager.getCartridgeSubscriptions("esb").isEmpty());
        assertTrue(dataInsertionAndRetrievalManager.getCartridgeSubscriptions("esb.privatejet").isEmpty());
        assertTrue(dataInsertionAndRetrievalManager.getCartridgeSubscriptions("php").isEmpty());
        assertTrue(dataInsertionAndRetrievalManager.getCartridgeSubscriptions("mysql").isEmpty());

        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptions(1, "esb"));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptions(1, "esb.privatejet"));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptions(3, "php"));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscriptions(3, "mysql"));

        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscription(1, "esba"));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscription(2, "esba"));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscription(1, "esba1"));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscription(2, "esbb"));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscription(3, "phpa"));
        assertNull(dataInsertionAndRetrievalManager.getCartridgeSubscription(3, "mysqla"));
    }
}
