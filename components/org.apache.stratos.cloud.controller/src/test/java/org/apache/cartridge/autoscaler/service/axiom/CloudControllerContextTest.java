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

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import junit.framework.TestCase;
import org.apache.stratos.cloud.controller.internal.ServiceReferenceHolder;

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
        ServiceReferenceHolder.getInstance().setAxisConfiguration(axisConfiguration);

    	CloudControllerContext context = CloudControllerContext.getInstance();
    	Thread t1 = new Thread(new MemberAdder(context));
    	t1.start();
    	t1.join();
    	assertEquals(2, context.getMemberContextsOfClusterId("123").size());
    	Thread t2 = new Thread(new MemberRemover(context));
    	t2.start();
    	t2.join();
    	assertEquals(1, context.getMemberContextsOfClusterId("123").size());
    	
    }
    class MemberAdder implements Runnable {
    	
    	private CloudControllerContext dataHolder;
    	public MemberAdder(CloudControllerContext data) {
    		this.dataHolder = data;
    	}
    	@Override
    	public void run() {
    		MemberContext ctxt1 = new MemberContext();
    		ctxt1.setMemberId("abc");
    		ctxt1.setClusterId("123");
    		MemberContext ctxt2 = new MemberContext();
    		ctxt2.setMemberId("def");
    		ctxt2.setClusterId("456");
    		MemberContext ctxt3 = new MemberContext();
    		ctxt3.setMemberId("ghi");
    		ctxt3.setClusterId("123");
    		dataHolder.addMemberContext(ctxt1);
    		dataHolder.addMemberContext(ctxt2);
    		dataHolder.addMemberContext(ctxt3);
    	}
    	
    }
    
    class MemberRemover implements Runnable {
    	
    	private CloudControllerContext dataHolder;
    	public MemberRemover(CloudControllerContext data) {
    		this.dataHolder = data;
    	}
    	@Override
    	public void run() {
    		dataHolder.removeMemberContext("ghi", "123");
    	}
    	
    }
    
}
