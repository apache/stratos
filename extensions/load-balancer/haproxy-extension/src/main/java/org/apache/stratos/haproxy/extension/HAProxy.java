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
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;
import org.apache.stratos.messaging.domain.topology.Topology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

/**
 * HAProxy load balancer life-cycle implementation.
 */
public class HAProxy implements LoadBalancer {
    private static final Log log = LogFactory.getLog(HAProxy.class);
    private static final String NEW_LINE = System.getProperty("line.separator");

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

    private void reloadConfiguration() throws LoadBalancerExtensionException {

        try {
            if (log.isInfoEnabled()) {
                log.info("Reloading configuration...");
            }

            // Read pid
            String pid = "";
            BufferedReader reader = new BufferedReader(new FileReader(processIdFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                pid += line + " ";
            }

            // Execute hot configuration deployment
            String command = executableFilePath + " -f " + confFilePath + " -p " + processIdFilePath + " -sf " + pid;
            CommandUtil.executeCommand(command);
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

    public void reload(Topology topology) throws LoadBalancerExtensionException {
        configure(topology);
        reloadConfiguration();
    }

    public void configure(Topology topology) throws LoadBalancerExtensionException {

        try {
            if (log.isInfoEnabled()) {
                log.info("Configuring haproxy instance...");
            }

            HAProxyConfigWriter writer = new HAProxyConfigWriter(templatePath, templateName, confFilePath, statsSocketFilePath);
            writer.write(topology);

            if (log.isInfoEnabled()) {
                log.info("Configuration done");
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not configure haproxy");
            }
            throw new LoadBalancerExtensionException(e);
        }
    }

    public void start() throws LoadBalancerExtensionException {

        // Check for configuration file
        File conf = new File(confFilePath);
        if (!conf.exists()) {
            throw new LoadBalancerExtensionException("Could not find haproxy configuration file");
        }

        // Start haproxy and write pid to processIdFilePath
        try {
            String command = executableFilePath + " -f " + confFilePath + " -p " + processIdFilePath;
            CommandUtil.executeCommand(command);
            if (log.isInfoEnabled()) {
                log.info("haproxy started");
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not start haproxy");
            }
            throw new LoadBalancerExtensionException(e);
        }
    }

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
                CommandUtil.executeCommand(command);
                if (log.isInfoEnabled()) {
                    log.info(String.format("haproxy stopped [pid] %s", pid));
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not stop haproxy");
            }
            throw new LoadBalancerExtensionException(e);
        }
    }
}
