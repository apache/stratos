/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.stratos.metadataservice.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TopologyListener implements ServletContextListener {

	private static final Log log = LogFactory.getLog(TopologyListener.class);

	private TopologyAgent topologyThread = null;
	private Thread thread = null;

    /**
     * Initialization of the context
     * @param arg0 ServletContextEvent
     */
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		log.info("Topology literner started....");
		if (topologyThread == null) {
			// load default agent
			topologyThread = new TopologyAgent();
			if (log.isDebugEnabled()) {
				log.debug("Loading default Cartridge Agent.");
			}
		}
		// start agent
		thread = new Thread(topologyThread);
		thread.start();

	}

    /**
     * Destroy the context
     * @param arg0 ServletContextEvent
     */
	@Override
	public void contextDestroyed(ServletContextEvent arg0)
    {
		thread.stop();
	}



}
