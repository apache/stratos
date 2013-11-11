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
 *
 * Thanks to Vaadin for HAProxyController implementation:
 * https://vaadin.com/license
 * http://dev.vaadin.com/browser/svn/incubator/Arvue/ArvueMaster/src/org/vaadin/arvue/arvuemaster/HAProxyController.java

 */
public class HAProxyConfigWriter {
    private static final Log log = LogFactory.getLog(Main.class);
    private static final String NEW_LINE = System.getProperty("line.separator");

    private String templatePath;
    private String templateName;
    private String confFilePath;

    public HAProxyConfigWriter(String templatePath, String templateName, String confFilePath) {
        this.templatePath = templatePath;
        this.templateName = templateName;
        this.confFilePath = confFilePath;
    }

    public void write(Topology topology) {

        StringBuilder sb = new StringBuilder();

        for(Service service : topology.getServices()) {
            for(Cluster cluster : service.getClusters()) {
                for(Port port : service.getPorts()) {

                    String frontEndId = cluster.getClusterId() + "-proxy-" + port.getProxy();
                    String backEndId = frontEndId + "-members";

                    sb.append("frontend ").append(frontEndId).append(NEW_LINE);
                    sb.append("\tbind ").append(cluster.getHostName()).append(":").append(port.getProxy()).append(NEW_LINE);
                    sb.append("\tdefault_backend ").append(backEndId).append(NEW_LINE);
                    sb.append(NEW_LINE);
                    sb.append("backend ").append(backEndId).append(NEW_LINE);

                    for (Member member : cluster.getMembers()) {
                        sb.append("\tserver ").append(member.getMemberId()).append(" ")
                          .append(member.getMemberIp()).append(":").append(port.getValue()).append(NEW_LINE);
                    }
                    sb.append(NEW_LINE);
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
        context.put("frontend_backend_collection", sb.toString());

        // Create a new string from the template
        StringWriter stringWriter = new StringWriter();
        t.merge(context, stringWriter);
        String configuration = stringWriter.toString();

        // Write configuration file
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(confFilePath));
            writer.write(configuration);
            writer.close();

            if(log.isInfoEnabled()) {
                log.info(String.format("Configuration written to file: %s", confFilePath));
            }
        } catch (IOException e) {
            if(log.isErrorEnabled()) {
                log.error("Could not write configuration file", e);
            }
        }
    }
}
