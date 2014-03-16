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

package org.apache.stratos.cartridge.agent.executor;

import java.util.List;

import org.apache.stratos.cartridge.agent.phase.Phase;

/**
 * An extension executor can be used to extend the features
 * of a {@link Phase}.
 * 
 */
public abstract class ExtensionExecutor {
	
	/**
	 * A unique id for this {@link ExtensionExecutor}
	 */
	private String id;
	
	private List<String> fileNamesToBeExecuted;
	
	public ExtensionExecutor(String id) {
		this.setId(id);
	}
	
	public ExtensionExecutor(List<String> fileNames) {
		this.setFileNamesToBeExecuted(fileNames);
	}
	
	public ExtensionExecutor() {
	}
	
	/**
	 * Sets the id of this {@link ExtensionExecutor}
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Returns the id of this {@link ExtensionExecutor}
	 * @return id
	 */
	public String getId() {
		return this.id;
	}
	
	public List<String> getFileNamesToBeExecuted() {
		return fileNamesToBeExecuted;
	}

	public void setFileNamesToBeExecuted(List<String> fileNamesToBeExecuted) {
		this.fileNamesToBeExecuted = fileNamesToBeExecuted;
	}

	/**
	 * Execution of this {@link ExtensionExecutor}
	 */
	public abstract void execute();
	
	/**
	 * Clean up this extension.
	 */
	public abstract void cleanUp();

	
}
