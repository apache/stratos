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

package org.apache.stratos.lvs.extension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.util.CommandUtils;
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;
import org.apache.stratos.load.balancer.common.domain.Topology;

import java.io.File;

/**
 * Lvs load balancer life-cycle implementation.
 */
public class LVS implements LoadBalancer {

    private static final Log log = LogFactory.getLog(LVS.class);

    private String executableFilePath;
    private String processIdFilePath;
    private String templatePath;
    private String templateName;
    private String confFilePath;
    private String statsSocketFilePath;
	private String virtualIPsForServices;
	private String keepAlivedStartCommand;
	private String serverState;
	private String scheduleAlgo;
	private boolean isKeepAlivedUsed;

    public LVS() {
        this.executableFilePath = LVSContext.getInstance().getExecutableFilePath();
        this.templatePath = LVSContext.getInstance().getTemplatePath();
        this.templateName = LVSContext.getInstance().getTemplateName();
        this.confFilePath = LVSContext.getInstance().getConfFilePath();
        this.processIdFilePath = confFilePath.replace(".cfg", ".pid");
        this.statsSocketFilePath = LVSContext.getInstance().getStatsSocketFilePath();
	    this.virtualIPsForServices= LVSContext.getInstance().getVirtualIPsForServices();
	    this.keepAlivedStartCommand=LVSContext.getInstance().getKeepAlivedStartCommand();
	    this.serverState=LVSContext.getInstance().getServerState();
	    this.scheduleAlgo=LVSContext.getInstance().getLvsScheduleAlgo();
	    this.isKeepAlivedUsed=LVSContext.getInstance().getIsKeepAlivedUsed();

    }

    /**
     * Configure lvs instance according to topology given
     * @param topology
     * @throws LoadBalancerExtensionException
     */
    public boolean configure(Topology topology) throws LoadBalancerExtensionException {
        try {
            log.info("Generating lvs configuration...");
            LVSConfigWriter writer = new LVSConfigWriter(templatePath, templateName, confFilePath, statsSocketFilePath,
                                                         virtualIPsForServices,serverState,scheduleAlgo);
            if(writer.write(topology)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Could not generate lvs configuration");
            throw new LoadBalancerExtensionException(e);
        }
    }

    /**
     * Start lvs instance
     * @throws LoadBalancerExtensionException
     */
    public void start() throws LoadBalancerExtensionException {
        log.info("Starting lvs instance...");
        // Check for configuration file
        File conf = new File(confFilePath);
        if (!conf.exists()) {
            throw new LoadBalancerExtensionException("Could not find lvs configuration file");
        }

        // Start nginx and write pid to processIdFilePath
        try {
	        if(isKeepAlivedUsed) {
		        String command = keepAlivedStartCommand;
		        CommandUtils.executeCommand(command);
	        }
            log.info("lvs instance started");
        } catch (Exception e) {
            log.error("Could not start lvs instance");
            throw new LoadBalancerExtensionException(e);
        }
    }

    /**
     * Reload lvs instance according to the configuration written in configure() method.
     * @throws LoadBalancerExtensionException
     */
    public void reload() throws LoadBalancerExtensionException {
        try {
            log.info("Reloading configuration...");

            if(isKeepAlivedUsed) {
	            // Execute hot configuration deployment
		        String command = "service keepalived restart";
		        CommandUtils.executeCommand(command);
	        }
            if (log.isInfoEnabled()) {
                log.info("Configuration done");
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Reconfiguration failed");
            }
            throw new LoadBalancerExtensionException(e);
        }
    }

    /**
     * Stop lvs instance
     * @throws LoadBalancerExtensionException
     */
    public void stop() throws LoadBalancerExtensionException {

        try {
            log.info("Stopping lvs...");

	        if(isKeepAlivedUsed) {
		        // Execute hot configuration deployment
		        String command = "service keepalived stop";
		        CommandUtils.executeCommand(command);
	        }
            if (log.isInfoEnabled()) {
                log.info("LVS stopped");
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not stop lvs");
            }
            throw new LoadBalancerExtensionException(e);
        }
    }
}
