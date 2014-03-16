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

package org.apache.stratos.cartridge.agent.executor.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.executor.ExtensionExecutor;
import org.apache.stratos.cartridge.agent.runtime.DataHolder;

/**
 * This extension is responsible for keeping the agent alive.
 * 
 */
public class KeepAgentLiveExtensionExecutor extends ExtensionExecutor {
	
	private static final Log log = LogFactory.getLog(KeepAgentLiveExtensionExecutor.class);

	public KeepAgentLiveExtensionExecutor() {
		super(KeepAgentLiveExtensionExecutor.class.getName());
	}
	
	public KeepAgentLiveExtensionExecutor(List<String> fileNames) {
		super.setFileNamesToBeExecuted(fileNames);
	}

	@Override
	public void execute() {
		try {
            if(log.isDebugEnabled()) {
                log.debug("Executing Extension: "+super.getId());
            }
            while (!DataHolder.getInstance().isTerminated()) {
            	try {
    				Thread.sleep(1000);
    			} catch (InterruptedException ignore) {
    			}
            }
        }
		catch (Exception e) {
            log.error("Could not execute extension: "+super.getId() , e);
        }
	}

	@Override
	public void cleanUp() {
	}
	
	
}
