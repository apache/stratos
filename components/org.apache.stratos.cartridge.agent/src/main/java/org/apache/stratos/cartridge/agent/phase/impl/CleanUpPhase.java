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
import org.apache.stratos.cartridge.agent.runtime.DataHolder;

/**
 * Inception phase of Cartridge Agent. From initialization to sending instance
 * started event.
 */
public class CleanUpPhase extends Phase {
	
	private static final Log log = LogFactory.getLog(CleanUpPhase.class);

	public CleanUpPhase() {
		super(CleanUpPhase.class.getName());
	}
	
	public CleanUpPhase(String id) {
		super(id);
	}

	@Override
	public void execute() {

		log.info("Currently Executing Phase: " + super.getId());

		// execute all the extensions of this phase in order.
		for (ExtensionExecutor extensionExecutor : super.getExtensions()) {
			extensionExecutor.execute();
		}

		// clean up all the phases
		for (Phase phase : DataHolder.getInstance().getPhases()) {
			phase.cleanUp();
		}

		log.info("Finished Executing Phase: " + super.getId());
	}

}
