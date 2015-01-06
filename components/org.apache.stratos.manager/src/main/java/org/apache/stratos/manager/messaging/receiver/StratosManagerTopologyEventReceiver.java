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

package org.apache.stratos.manager.messaging.receiver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.concurrent.ExecutorService;

public class StratosManagerTopologyEventReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(StratosManagerTopologyEventReceiver.class);

    private TopologyEventReceiver topologyEventReceiver;
    private boolean terminated;
	private ExecutorService executorService;

    public StratosManagerTopologyEventReceiver() {
        this.terminated = false;
        this.topologyEventReceiver = new TopologyEventReceiver();
        addEventListeners();
    }

    private void addEventListeners() {
    }


    @Override
    public void run() {

	    topologyEventReceiver.setExecutorService(executorService);
	    topologyEventReceiver.execute();

        log.info("Stratos manager topology event receiver thread started");
    }

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}
}
