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

package org.apache.stratos.cartridge.agent.phase.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.executor.ExtensionExecutor;
import org.apache.stratos.cartridge.agent.phase.Phase;

/**
 * Working phase of Cartridge Agent. After sending out member activated event,
 * agent goes to the working phase.
 */
public class WorkingPhase extends Phase {

	private static final Log log = LogFactory.getLog(WorkingPhase.class);
			
	public WorkingPhase() {
		super(WorkingPhase.class.getName());
	}
	
	public WorkingPhase(String id) {
		super(id);
	}

	@Override
	public void execute() {
		
		log.info("Currently Executing Phase: "+super.getId());
		
		// execute all the extensions of this phase in order.
		for (ExtensionExecutor extensionExecutor : super.getExtensions()) {
			extensionExecutor.execute();
		}
		
		log.info("Finished Executing Phase: "+super.getId());

	}

}
