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
package org.apache.stratos.cartridge.agent.config;

import java.util.List;

import org.apache.stratos.cartridge.agent.executor.impl.ScriptExtensionExecutor;
import org.apache.stratos.cartridge.agent.executor.impl.StartListenersExtensionExecutor;
import org.apache.stratos.cartridge.agent.phase.Phase;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentConstants;

import junit.framework.TestCase;

public class AgentFlowFileLoadTest extends TestCase {
    
    public AgentFlowFileLoadTest(String name) {
        super(name);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty(CartridgeAgentConstants.AGENT_FLOW_FILE_PATH, "src/test/resources/agent-flow.conf");
    }

    public final void testAgentFlowConfigParsing() throws Exception {
    	
    	List<Phase> phases = CartridgeAgentConfiguration.loadFlowConfig();
    	
    	assertEquals(2, phases.size());
    	
    	Phase phase = phases.get(0);
		assertEquals("Initializing", phase.getId());
    	
    	assertEquals(2, phase.getExtensions().size());
    	
    	// ensure the order has been maintained 
    	assertEquals(true, phase.getExtensions().get(0) instanceof StartListenersExtensionExecutor);	
    	assertEquals(true, phase.getExtensions().get(1) instanceof ScriptExtensionExecutor);
    	
    	// test loading scripts
    	assertEquals(2, phase.getExtensions().get(1).getFileNamesToBeExecuted().size());
    	
    	assertEquals("Starting", phases.get(1).getId());
    	
    	assertEquals(1, phases.get(1).getExtensions().get(0).getFileNamesToBeExecuted().size());
    }
    
}
