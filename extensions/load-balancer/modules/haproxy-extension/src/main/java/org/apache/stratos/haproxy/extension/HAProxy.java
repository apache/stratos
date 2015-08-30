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

package org.apache.stratos.haproxy.extension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.util.CommandUtils;
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;
import org.apache.stratos.load.balancer.common.domain.Topology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

/**
 * HAProxy load balancer life-cycle implementation.
 */
public class HAProxy implements LoadBalancer {

    private static final Log log = LogFactory.getLog(HAProxy.class);

    private String executableFilePath;
    private String processIdFilePath;
    private String templatePath;
    private String templateName;
    private String confFilePath;
    private String statsSocketFilePath;

    public HAProxy() {
        this.executableFilePath = HAProxyContext.getInstance().getExecutableFilePath();
        this.templatePath = HAProxyContext.getInstance().getTemplatePath();
        this.templateName = HAProxyContext.getInstance().getTemplateName();
        this.confFilePath = HAProxyContext.getInstance().getConfFilePath();
        this.processIdFilePath = confFilePath.replace(".cfg", ".pid");
        this.statsSocketFilePath = HAProxyContext.getInstance().getStatsSocketFilePath();
    }

    /**
     * Configure haproxy instance according to topology given
     * @param topology
     * @throws LoadBalancerExtensionException
     */
    public boolean configure(Topology topology) throws LoadBalancerExtensionException {
        try {
            log.info("Generating haproxy configuration...");
            HAProxyConfigWriter writer = new HAProxyConfigWriter(templatePath, templateName, confFilePath, statsSocketFilePath);
            if(writer.write(topology)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Could not generate haproxy configuration");
            throw new LoadBalancerExtensionException(e);
        }
    }

    /**
     * Start haproxy instance
     * @throws LoadBalancerExtensionException
     */
    public void start() throws LoadBalancerExtensionException {
        log.info("Starting haproxy instance...");
        // Check for configuration file
        File conf = new File(confFilePath);
        if (!conf.exists()) {
            throw new LoadBalancerExtensionException("Could not find haproxy configuration file");
        }

        // Start haproxy and write pid to processIdFilePath
        try {
            String command = executableFilePath + " -f " + confFilePath + " -p " + processIdFilePath;
            CommandUtils.executeCommand(command);
            log.info("haproxy instance started");
        } catch (Exception e) {
            log.error("Could not start haproxy instance");
            throw new LoadBalancerExtensionException(e);
        }
    }

    /**
     * Reload haproxy instance according to the configuration written in configure() method.
     * @throws LoadBalancerExtensionException
     */
    public void reload() throws LoadBalancerExtensionException {
        try {
            log.info("Reloading configuration...");

            // Read pid
            String pid = "";
            BufferedReader reader = new BufferedReader(new FileReader(processIdFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                pid += line + " ";
            }

            // Execute hot configuration deployment
            String command = executableFilePath + " -f " + confFilePath + " -p " + processIdFilePath + " -sf " + pid;
            CommandUtils.executeCommand(command);
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
     * Stop haproxy instance
     * @throws LoadBalancerExtensionException
     */
    public void stop() throws LoadBalancerExtensionException {

        try {
            // Read the PIDs
            Vector<String> pids = new Vector<String>();
            BufferedReader reader = new BufferedReader(new FileReader(processIdFilePath));
            String pid_;
            while ((pid_ = reader.readLine()) != null) {
                pids.add(pid_);
            }

            // Kill all haproxy processes
            for (String pid : pids) {
                String command = "kill -s 9 " + pid;
                CommandUtils.executeCommand(command);
                if (log.isInfoEnabled()) {
                    log.info(String.format("haproxy instance stopped [pid] %s", pid));
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not stop haproxy instance", e);
            }
        }
    }
}
