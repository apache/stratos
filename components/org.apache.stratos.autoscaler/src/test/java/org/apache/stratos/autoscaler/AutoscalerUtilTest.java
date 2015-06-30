/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler;

import org.apache.stratos.autoscaler.applications.pojo.*;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.common.clustering.impl.HazelcastDistributedObjectProvider;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Autoscaler util test.
 */
public class AutoscalerUtilTest {

    @Test
    public void testFindTenantIdRange() {
        int tenantId = -1234;
        String tenantPartitions = "1-10,11-20,20-*";
        String tenantRange = AutoscalerUtil.findTenantRange(tenantId, tenantPartitions);
        assertEquals(tenantRange, "*");

        tenantId = 1;
        tenantRange = AutoscalerUtil.findTenantRange(tenantId, tenantPartitions);
        assertEquals(tenantRange, "1-10");

        tenantId = 4;
        tenantRange = AutoscalerUtil.findTenantRange(tenantId, tenantPartitions);
        assertEquals(tenantRange, "1-10");

        tenantId = 10;
        tenantRange = AutoscalerUtil.findTenantRange(tenantId, tenantPartitions);
        assertEquals(tenantRange, "1-10");

        tenantId = 11;
        tenantRange = AutoscalerUtil.findTenantRange(tenantId, tenantPartitions);
        assertEquals(tenantRange, "11-20");

        tenantId = 14;
        tenantRange = AutoscalerUtil.findTenantRange(tenantId, tenantPartitions);
        assertEquals(tenantRange, "11-20");

        tenantId = 20;
        tenantRange = AutoscalerUtil.findTenantRange(tenantId, tenantPartitions);
        assertEquals(tenantRange, "11-20");

        tenantId = 25;
        tenantRange = AutoscalerUtil.findTenantRange(tenantId, tenantPartitions);
        assertEquals(tenantRange, "20-*");

        tenantId = 25;
        tenantRange = AutoscalerUtil.findTenantRange(tenantId, null);
        assertEquals(tenantRange, "*");
    }

    @Test
    public void testRemovalOfAutoscalingPolicy() {
        List<CartridgeContext> cartridgeContexts = new ArrayList<CartridgeContext>();
        for(int i = 0; i < 12; i++) {
            CartridgeContext cartridgeContext = new CartridgeContext();
            SubscribableInfoContext subscribableContext = new SubscribableInfoContext();
            subscribableContext.setAlias("cart-" + i);
            subscribableContext.setDeploymentPolicy("dep-" + i);
            subscribableContext.setAutoscalingPolicy("auto-" + i);
            cartridgeContext.setSubscribableInfoContext(subscribableContext);
            cartridgeContexts.add(cartridgeContext);
        }
        List<GroupContext> groupContextList = new ArrayList<GroupContext>();
        for(int i = 0; i < 5; i++) {
            GroupContext groupContext = new GroupContext();
            groupContext.setAlias("group-" + i);
            CartridgeContext[] cartridgeContexts1 = new CartridgeContext[2];
            cartridgeContexts1[0] = cartridgeContexts.get(i);
            cartridgeContexts1[1] = cartridgeContexts.get(i+5);

            groupContext.setCartridgeContexts(cartridgeContexts1);
            groupContextList.add(groupContext);
        }

        //Application-1
        ApplicationContext applicationContext = new ApplicationContext();
        applicationContext.setApplicationId("application-1");
        ComponentContext componentContext = new ComponentContext();
        CartridgeContext[] cartridgeContexts1 = new CartridgeContext[2];
        cartridgeContexts1[0] = cartridgeContexts.get(10);
        cartridgeContexts1[1] = cartridgeContexts.get(11);
        componentContext.setCartridgeContexts(cartridgeContexts1);
        GroupContext[] groupContexts = new GroupContext[groupContextList.size()];
        componentContext.setGroupContexts(groupContextList.toArray(groupContexts));
        applicationContext.setComponents(componentContext);

        ServiceReferenceHolder holder = ServiceReferenceHolder.getInstance();
        holder.setDistributedObjectProvider(new HazelcastDistributedObjectProvider());

        AutoscalerContext.getInstance().addApplicationContext(applicationContext);


        boolean canRemove;

        canRemove = AutoscalerUtil.removableAutoScalerPolicy("test");
        assertEquals(canRemove, true);

        for(int i = 0; i < 12; i ++) {
            canRemove = AutoscalerUtil.removableAutoScalerPolicy("auto-" + i);
            assertEquals(canRemove, false);

        }
    }

    public void testRemovalOfDeploymentPolicy() {

    }
}
