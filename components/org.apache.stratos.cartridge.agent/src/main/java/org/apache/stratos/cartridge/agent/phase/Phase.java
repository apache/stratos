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

package org.apache.stratos.cartridge.agent.phase;

import java.util.ArrayList;
import java.util.List;

import org.apache.stratos.cartridge.agent.executor.ExtensionExecutor;

/**
 * A phase corresponds to a period between two life cycle states
 * of agent.
 * eg: phase between started state to activated state would be
 * activating phase.
 */
public abstract class Phase {
	
	/**
	 * A unique id for this phase.
	 */
	private String id;
	
	private List<ExtensionExecutor> extensions;

	public Phase(String id) {
		this.setId(id);
	}
	
	public Phase() {
	}
	
	/**
	 * Returns the id of this phase.
	 * @return id
	 */
	public String getId() {
		return this.id;
	}
	
	/**
	 * Returns a list of registered extension executors.
	 * @return 
	 */
	public List<ExtensionExecutor> getExtensions() {
		return extensions;
	}
	
	/**
	 * Set a list of extension executors.
	 * @param executors list of extension executors.
	 */
	public void setExtensions(List<ExtensionExecutor> executors) {
		this.extensions = executors;
	}
	
	/**
	 * Add an {@link ExtensionExecutor} to this phase.
	 * @param extension
	 */
	public void addExtension(ExtensionExecutor extension) {
		if(this.extensions == null) {
			this.extensions = new ArrayList<ExtensionExecutor>();
		} 
		this.extensions.add(extension);
	}
	
	/**
	 * Sets the id of this phase
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Carry out the work that this phase suppose to do.
	 */
	public abstract void execute();
	
	/**
	 * Clean up work that this phase needs to perform.
	 */
	public void cleanUp() {
		// ask all extensions to clean up by themselves.
		for (ExtensionExecutor extensionExecutor : extensions) {
			extensionExecutor.cleanUp();
		}
	}
	
	
	
}
