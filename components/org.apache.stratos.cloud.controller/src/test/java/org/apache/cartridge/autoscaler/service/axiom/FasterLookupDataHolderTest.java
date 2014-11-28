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

import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.context.FasterLookUpDataHolder;
import junit.framework.TestCase;

public class FasterLookupDataHolderTest extends TestCase {
    
    public FasterLookupDataHolderTest(String name) {
        super(name);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    public final void testMemberContextOperations() throws Exception {
    	
    	
    	FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();
    	Thread t1 = new Thread(new MemberAdder(dataHolder));
    	t1.start();
    	t1.join();
    	assertEquals(2, dataHolder.getMemberContextsOfClusterId("123").size());
    	Thread t2 = new Thread(new MemberRemover(dataHolder));
    	t2.start();
    	t2.join();
    	assertEquals(1, dataHolder.getMemberContextsOfClusterId("123").size());
    	
    }
    class MemberAdder implements Runnable {
    	
    	private FasterLookUpDataHolder dataHolder;
    	public MemberAdder(FasterLookUpDataHolder data) {
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
    	
    	private FasterLookUpDataHolder dataHolder;
    	public MemberRemover(FasterLookUpDataHolder data) {
    		this.dataHolder = data;
    	}
    	@Override
    	public void run() {
    		dataHolder.removeMemberContext("ghi", "123");
    	}
    	
    }
    
}
