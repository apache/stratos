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
package org.apache.stratos.cloud.controller.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.wso2.carbon.ntask.core.Task;

import java.util.Map;

public class TopologySynchronizerTask implements Task{
    private static final Log log = LogFactory.getLog(TopologySynchronizerTask.class);

    @Override
    public void execute() {
        if (log.isDebugEnabled()) {
            log.debug("Executing topology synchronization task");
        }

        if(FasterLookUpDataHolder.getInstance().isTopologySyncRunning() ||
        		// this is a temporary fix to avoid task execution - limitation with ntask
                (!FasterLookUpDataHolder.getInstance().getEnableTopologySync())){
            if(log.isWarnEnabled()) {
                log.warn("Topology synchronization is disabled.");
            }
            return;
        }
        
    	// publish to the topic 
        if (TopologyManager.getTopology() != null) {
            TopologyEventSender.sendCompleteTopologyEvent(TopologyManager.getTopology());
        }
    }
    
    @Override
    public void init() {

    	// this is a temporary fix to avoid task execution - limitation with ntask
		if(!FasterLookUpDataHolder.getInstance().getEnableTopologySync()){
            if(log.isWarnEnabled()) {
                log.warn("Topology synchronization is disabled.");
            }
			return;
		}
    }

    @Override
    public void setProperties(Map<String, String> arg0) {}
    
}
