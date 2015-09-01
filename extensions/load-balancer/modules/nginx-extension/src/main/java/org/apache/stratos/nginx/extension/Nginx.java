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

package org.apache.stratos.nginx.extension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.util.CommandUtils;
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;
import org.apache.stratos.load.balancer.common.domain.Topology;

import java.io.File;

/**
 * Nginx load balancer life-cycle implementation.
 */
public class Nginx implements LoadBalancer {

    private static final Log log = LogFactory.getLog(Nginx.class);

    private String executableFilePath;
    private String processIdFilePath;
    private String templatePath;
    private String templateName;
    private String confFilePath;
    private String statsSocketFilePath;

    public Nginx() {
        this.executableFilePath = NginxContext.getInstance().getExecutableFilePath();
        this.templatePath = NginxContext.getInstance().getTemplatePath();
        this.templateName = NginxContext.getInstance().getTemplateName();
        this.confFilePath = NginxContext.getInstance().getConfFilePath();
        this.processIdFilePath = confFilePath.replace(".cfg", ".pid");
        this.statsSocketFilePath = NginxContext.getInstance().getStatsSocketFilePath();
    }

    /**
     * Configure nginx instance according to topology given
     * @param topology
     * @throws LoadBalancerExtensionException
     */
    public boolean configure(Topology topology) throws LoadBalancerExtensionException {
        try {
            log.info("Generating nginx configuration...");
            NginxConfigWriter writer = new NginxConfigWriter(templatePath, templateName, confFilePath, statsSocketFilePath);
            if(writer.write(topology)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Could not generate nginx configuration");
            throw new LoadBalancerExtensionException(e);
        }
    }

    /**
     * Start nginx instance
     * @throws LoadBalancerExtensionException
     */
    public void start() throws LoadBalancerExtensionException {
        log.info("Starting nginx instance...");
        // Check for configuration file
        File conf = new File(confFilePath);
        if (!conf.exists()) {
            throw new LoadBalancerExtensionException("Could not find nginx configuration file");
        }

        // Start nginx and write pid to processIdFilePath
        try {
            String command = executableFilePath + " -c " + confFilePath;
            CommandUtils.executeCommand(command);
            log.info("nginx instance started");
        } catch (Exception e) {
            log.error("Could not start nginx instance");
            throw new LoadBalancerExtensionException(e);
        }
    }

    /**
     * Reload nginx instance according to the configuration written in configure() method.
     * @throws LoadBalancerExtensionException
     */
    public void reload() throws LoadBalancerExtensionException {
        try {
            log.info("Reloading configuration...");

            // Execute hot configuration deployment
            String command = executableFilePath + " -c " + confFilePath + " -s reload";
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
     * Stop nginx instance
     * @throws LoadBalancerExtensionException
     */
    public void stop() throws LoadBalancerExtensionException {

        try {
            log.info("Stopping nginx...");

            // Execute hot configuration deployment
            String command = executableFilePath + " -s stop";
            CommandUtils.executeCommand(command);
            if (log.isInfoEnabled()) {
                log.info("Nginx stopped");
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not stop nginx");
            }
            throw new LoadBalancerExtensionException(e);
        }
    }
}
