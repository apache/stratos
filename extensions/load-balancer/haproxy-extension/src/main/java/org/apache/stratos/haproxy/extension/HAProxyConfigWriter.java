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
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

/**
 * HAProxy load balancer configuration writer.
 */
public class HAProxyConfigWriter {
    private static final Log log = LogFactory.getLog(Main.class);
    private static final String NEW_LINE = System.getProperty("line.separator");

    private String templatePath;
    private String templateName;
    private String confFilePath;
    private String statsSocketFilePath;

    public HAProxyConfigWriter(String templatePath, String templateName, String confFilePath, String statsSocketFilePath) {
        this.templatePath = templatePath;
        this.templateName = templateName;
        this.confFilePath = confFilePath;
        this.statsSocketFilePath = statsSocketFilePath;
    }

    public void write(Topology topology) {
        // Prepare global parameters
        StringBuilder globalParameters = new StringBuilder();
        globalParameters.append("stats socket ");
        globalParameters.append(statsSocketFilePath);

        // Prepare frontend http collection
        StringBuilder frontEndHttp = new StringBuilder();
        // Prepare frontend https collection
        StringBuilder frontEndHttps = new StringBuilder();
        // Prepare backend http collection
        StringBuilder backEndHttp = new StringBuilder();
        // Prepare backend https collection
        StringBuilder backEndHttps = new StringBuilder();

        String frontEndHttpId = "http_frontend";
        String frontEndHttpsId = "https_frontend";
        boolean frontEndHttpAdded = false;
        boolean frontEndHttpsAdded = false;

        for (Service service : topology.getServices()) {
            for (Cluster cluster : service.getClusters()) {

                if(cluster.getServiceName().equals("haproxy"))
                    continue;

                if ((service.getPorts() == null) || (service.getPorts().size() == 0)) {
                    throw new RuntimeException(String.format("No ports found in service: %s", service.getServiceName()));
                }

                for (Port port : service.getPorts()) {
                    if (port.getProtocol().equals("http")){
                        if (!frontEndHttpAdded) {
                            frontEndHttp.append("frontend ").append(frontEndHttpId).append(NEW_LINE);
                            frontEndHttp.append("\tbind ").append(HAProxyContext.getInstance().getHAProxyPrivateIp()).append(":").append(port.getProxy()).append(NEW_LINE);
                            frontEndHttp.append("\tmode ").append(port.getProtocol()).append(NEW_LINE);
                            frontEndHttpAdded = true;
                        }

                        for(String hostname : cluster.getHostNames()) {
                            frontEndHttp.append("\tacl ").append("is_").append(hostname).append(" hdr_beg(host) -i ").append(hostname).append(NEW_LINE);
                            frontEndHttp.append("\tuse_backend ").append(hostname).append("-http-members if is_").append(hostname).append(NEW_LINE);

                            // Backend block
                            backEndHttp.append("backend ").append(hostname).append("-http-members").append(NEW_LINE);
                            backEndHttp.append("\tmode ").append("http").append(NEW_LINE);
                            for (Member member : cluster.getMembers()) {
                                backEndHttp.append("\tserver ").append(member.getMemberId()).append(" ")
                                        .append(member.getMemberIp()).append(":").append(port.getValue()).append(NEW_LINE);
                            }
                            backEndHttp.append(NEW_LINE);
                        }
                    } else if (port.getProtocol().equals("https")){
                        if (!frontEndHttpsAdded) {
                            frontEndHttp.append("frontend ").append(frontEndHttpsId).append(NEW_LINE);
                            frontEndHttp.append("\tbind ").append(HAProxyContext.getInstance().getHAProxyPrivateIp()).append(":").append(port.getProxy()).append(NEW_LINE);
                            frontEndHttp.append("\tmode ").append("http").append(NEW_LINE);
                            frontEndHttpsAdded = true;
                        }

                        for(String hostname : cluster.getHostNames()) {
                            frontEndHttps.append("\tacl ").append("is_").append(hostname).append(" hdr_beg(host) -i ").append(hostname).append(NEW_LINE);
                            frontEndHttps.append("\tuse_backend ").append(hostname).append("-https-members if is_").append(hostname).append(NEW_LINE);

                            // Backend block
                            backEndHttps.append("backend ").append(hostname).append("-http-members").append(NEW_LINE);
                            backEndHttps.append("\tmode ").append("https").append(NEW_LINE);
                            for (Member member : cluster.getMembers()) {
                                backEndHttps.append("\tserver ").append(member.getMemberId()).append(" ")
                                        .append(member.getMemberIp()).append(":").append(port.getValue()).append(NEW_LINE);
                            }
                            backEndHttps.append(NEW_LINE);
                        }
                    }
                }
            }
        }

        // Start velocity engine
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templatePath);
        ve.init();

        // Open the template
        Template t = ve.getTemplate(templateName);

        // Insert strings into the template
        VelocityContext context = new VelocityContext();
        context.put("global_parameters", globalParameters.toString());
        context.put("frontend_http_collection", frontEndHttp.toString());
        context.put("frontend_https_collection", frontEndHttps.toString());
        context.put("backend_http_collection", backEndHttp.toString());
        context.put("backend_https_collection", backEndHttps.toString());

        // Create a new string from the template
        StringWriter stringWriter = new StringWriter();
        t.merge(context, stringWriter);
        String configuration = stringWriter.toString();

        // Write configuration file
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(confFilePath));
            writer.write(configuration);
            writer.close();

            if (log.isInfoEnabled()) {
                log.info(String.format("Configuration written to file: %s", confFilePath));
            }
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not write configuration file: %s", confFilePath));
            }
            throw new RuntimeException(e);
        }
    }
}
