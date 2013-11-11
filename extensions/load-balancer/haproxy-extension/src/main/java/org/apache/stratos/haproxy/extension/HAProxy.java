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

import java.io.*;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;

/**
 * HAProxy load balancer life-cycle implementation.
 *
 * Thanks to Vaadin for HAProxyController implementation:
 * https://vaadin.com/license
 * http://dev.vaadin.com/browser/svn/incubator/Arvue/ArvueMaster/src/org/vaadin/arvue/arvuemaster/HAProxyController.java
 */
public class HAProxy implements LoadBalancer {
    private static final Log log = LogFactory.getLog(HAProxy.class);
    private static final String NEW_LINE = System.getProperty("line.separator");

    private String executableFilePath;
    private String processIdFilePath;
    private String templatePath;
    private String templateName;
    private String confFilePath;

    public HAProxy(String executableFilePath, String templatePath, String templateName, String confFilePath) {
        this.executableFilePath = executableFilePath;
        this.templatePath = templatePath;
        this.templateName = templateName;
        this.confFilePath = confFilePath;
        this.processIdFilePath = confFilePath.replace(".cfg", ".pid");
    }

    public void reload(Topology topology) {
        configure(topology);
        reloadConfiguration();
    }

    public void configure(Topology topology) {
        if(log.isInfoEnabled()) {
            log.info("Configuring haproxy instance...");
        }

        HAProxyConfigWriter writer = new HAProxyConfigWriter(templatePath, templateName, confFilePath);
        writer.write(topology);

        if(log.isInfoEnabled()) {
            log.info("Configuration done");
        }
    }

    private void executeCommand(String command) throws IOException {
        String line;
        Runtime r = Runtime.getRuntime();
        if (log.isDebugEnabled()) {
            log.debug("command = " + command);
        }
        Process p = r.exec(command);
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((line = in.readLine()) != null) {
            if(log.isInfoEnabled()) {
                log.info(line);
            }
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        while ((line = error.readLine()) != null) {
            if(log.isInfoEnabled()) {
                log.info(line);
                sb.append(line + NEW_LINE);
            }
        }
        if(sb.length() > 0) {
            throw new RuntimeException("Command execution failed: " + NEW_LINE + sb.toString());
        }
    }

    private void reloadConfiguration() {

        if(log.isInfoEnabled()) {
            log.info("Reloading configuration...");
        }
        BufferedReader input = null;

        try {
            // Read pid
            String pid = "";
            BufferedReader reader = new BufferedReader(new FileReader(processIdFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                pid += line + " ";
            }

            // Execute hot configuration deployment
            String command = executableFilePath + " -f " + confFilePath + " -p " + processIdFilePath + " -sf " + pid;
            executeCommand(command);
            if(log.isInfoEnabled()) {
                log.info("Configuration done");
            }
        } catch (Exception e) {
            if(log.isErrorEnabled()) {
                log.error("Reconfiguration failed", e);
            }
        }
    }

    public void start() {

        // Check for configuration file
        File conf;
        conf = new File(confFilePath);
        if (!conf.exists()) {
            throw new RuntimeException("Could not find haproxy configuration file");
        }

        // Start haproxy and write pid to processIdFilePath
        try {
            String command = executableFilePath + " -f " + confFilePath +" -p " + processIdFilePath;
            executeCommand(command);
            if(log.isInfoEnabled()) {
                log.info("haproxy started");
            }
        } catch (Exception e) {
            if(log.isErrorEnabled()) {
                log.error("Could not start haproxy", e);
            }
        }
    }

    public void stop() {

        // Read the PID's
        Vector<String> pids = new Vector<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(processIdFilePath));
            String pid = null;
            while ((pid = reader.readLine()) != null) {
                pids.add(pid);
            }
        } catch (Exception e) {
            if(log.isErrorEnabled()) {
                log.error(e);
            }
            return;
        }

        // Kill all haproxy processes
        for (String pid : pids) {
            try {
                String command = "kill -s 9" + pid;
                executeCommand(command);
                if(log.isInfoEnabled()) {
                    log.info("haproxy stopped");
                }
            } catch (Exception e) {
                if(log.isErrorEnabled()) {
                    log.error(e);
                }
                return;
            }
        }
    }
}
