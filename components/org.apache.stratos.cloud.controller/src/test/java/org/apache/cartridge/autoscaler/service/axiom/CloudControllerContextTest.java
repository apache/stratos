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
package org.apache.cartridge.autoscaler.service.axiom;

import junit.framework.TestCase;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.internal.ServiceReferenceHolder;
import org.apache.stratos.common.clustering.impl.HazelcastDistributedObjectProvider;

public class CloudControllerContextTest extends TestCase {

    public CloudControllerContextTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public final void testMemberContextOperations() throws Exception {
        AxisConfiguration axisConfiguration = new AxisConfiguration();
        axisConfiguration.setClusteringAgent(null);

        ServiceReferenceHolder.getInstance().setDistributedObjectProvider(new HazelcastDistributedObjectProvider());
        ServiceReferenceHolder.getInstance().setAxisConfiguration(axisConfiguration);

        CloudControllerContext.unitTest = true;
        CloudControllerContext cloudControllerContext = CloudControllerContext.getInstance();

        Thread t1 = new Thread(new MemberAdder(cloudControllerContext));
        t1.start();
        t1.join();
        assertEquals(2, cloudControllerContext.getMemberContextsOfClusterId("cluster-1").size());

        Thread t2 = new Thread(new MemberRemover(cloudControllerContext));
        t2.start();
        t2.join();
        assertEquals(1, cloudControllerContext.getMemberContextsOfClusterId("cluster-1").size());
    }

    class MemberAdder implements Runnable {

        private CloudControllerContext dataHolder;

        public MemberAdder(CloudControllerContext data) {
            this.dataHolder = data;
        }

        @Override
        public void run() {
            MemberContext ctxt1 = new MemberContext("application-1", "cartridge-1", "cluster-1", "member-1");
            MemberContext ctxt2 = new MemberContext("application-1", "cartridge-1", "cluster-1", "member-2");
            MemberContext ctxt3 = new MemberContext("application-1", "cartridge-1", "cluster-2", "member-3");
            dataHolder.addMemberContext(ctxt1);
            dataHolder.addMemberContext(ctxt2);
            dataHolder.addMemberContext(ctxt3);
        }
    }

    class MemberRemover implements Runnable {

        private CloudControllerContext dataHolder;

        public MemberRemover(CloudControllerContext cloudControllerContext) {
            this.dataHolder = cloudControllerContext;
        }

        @Override
        public void run() {
            dataHolder.removeMemberContext("cluster-1", "member-1");
        }
    }
}
