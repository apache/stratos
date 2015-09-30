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
import org.apache.stratos.load.balancer.common.domain.*;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Nginx load balancer configuration writer.
 */
public class NginxConfigWriter {

    private static final Log log = LogFactory.getLog(Main.class);
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String TAB = "    ";

    private String templatePath;
    private String templateName;
    private String confFilePath;
    private String statsSocketFilePath;

    public NginxConfigWriter(String templatePath, String templateName, String confFilePath,
                             String statsSocketFilePath) {

        this.templatePath = templatePath;
        this.templateName = templateName;
        this.confFilePath = confFilePath;
        this.statsSocketFilePath = statsSocketFilePath;
    }

    public boolean write(Topology topology) {
        List<Port> availablePorts = new ArrayList<Port>();
        for (Service service : topology.getServices()) {
            for (Cluster cluster : service.getClusters()) {
                if ((service.getPorts() == null) || (service.getPorts().size() == 0)) {
                    throw new RuntimeException(String.format("No ports found in service: %s", service.getServiceName()));
                }
                for (Member member : cluster.getMembers()) {
                    Collection<Port> ports = member.getPorts();
                    for(Port port : ports) {
                        boolean protocolFound = false;
                        for(Port availablePort : availablePorts) {
                            if ((availablePort.getProtocol().equals(port.getProtocol()))) {
                                protocolFound = true;
                                break;
                            }
                        }
                        if(!protocolFound) {
                            if (log.isDebugEnabled()) {
                                log.debug("Available protocols : " + port.getProtocol() + " proxy val: " +
                                        port.getProxy() + "\n");
                            }
                            availablePorts.add(port);
                        } else {
                            boolean proxyFound = false;
                            for(Port availablePort : availablePorts) {
                                if (availablePort.getProtocol().equals(port.getProtocol()) &&
                                        availablePort.getProxy() == port.getProxy()) {
                                    proxyFound = true;
                                    break;
                                }
                            }

                            if(!proxyFound) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Available protocols : " + port.getProtocol() + " proxy val: " +
                                            port.getProxy() + "\n");
                                }
                                availablePorts.add(port);
                            }
                        }
                    }
                }

            }
        }

        //Constructing the port list
        List<Map<String, String>> portList = new ArrayList<Map<String, String>>();
        Map<String, String> portMap;

        Map<String, Map<String, List>> hostnameToPortMap = new HashMap<String, Map<String, List>>();

        for (Port availPort : availablePorts) {
            portMap = new HashMap<String, String>();
            portMap.put("proxy", String.valueOf(availPort.getProxy()));
            portMap.put("protocol", availPort.getProtocol());
            portMap.put("value", String.valueOf(availPort.getValue()));
            portList.add(portMap);


            for (Service service : topology.getServices()) {
                for (Cluster cluster : service.getClusters()) {
                    Map<String, List> existingHostNameToServerMap = hostnameToPortMap.
                                                get(String.valueOf(availPort.getProxy()));
                    if(existingHostNameToServerMap == null) {
                        existingHostNameToServerMap = new HashMap<String, List>();
                    }
                    if ((service.getPorts() == null) || (service.getPorts().size() == 0)) {
                        throw new RuntimeException(String.format("No ports found in service: %s",
                                service.getServiceName()));
                    }
                    generateConfigurationForCluster(cluster, availPort, existingHostNameToServerMap);
                    hostnameToPortMap.put(String.valueOf(availPort.getProxy()), existingHostNameToServerMap);

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
        context.put("portlist", portList);
        context.put("servermap", hostnameToPortMap);

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
            return true;
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not write configuration file: %s", confFilePath));
            }
            throw new RuntimeException(e);
        }
    }

    private void generateConfigurationForCluster(Cluster cluster, Port availPort, Map<String, List> existingHostNameToServerMap) {
        for (String hostname : cluster.getHostNames()) {
            boolean memberFound = false;
            //Checking whether at-least one member is available to create
            // the upstream and server blocks
            for (Member member : cluster.getMembers()) {
                Collection<Port> ports = member.getPorts();
                for (Port port : ports) {
                    if ((port.getProtocol().equals(availPort.getProtocol())) &&
                            (port.getProxy() == availPort.getProxy())) {
                        memberFound = true;
                        break;
                    }
                }
                if(memberFound) {
                    break;
                }
            }
            if(memberFound) {
                for (Member member : cluster.getMembers()) {
                    Port selectedPort = null;
                    Collection<Port> ports = member.getPorts();
                    for (Port port : ports) {
                        if ((port.getProtocol().equals(availPort.getProtocol())) &&
                                (port.getProxy() == availPort.getProxy())) {
                            selectedPort = port;
                            break;
                        }
                    }

                    if (selectedPort != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("The selected Port for cluster: " + cluster.getClusterId()
                                    + " is " + selectedPort.getValue() + " " +
                                    selectedPort.getProtocol() + " " + selectedPort.getProxy());
                        }
                        if(existingHostNameToServerMap.get(hostname) == null) {
                            List<String> serverList = new ArrayList<String>();
                            existingHostNameToServerMap.put(hostname, serverList);
                        }
                        // Start upstream server block
                        existingHostNameToServerMap.get(hostname).add(member.getHostName() + ":" +
                                                        selectedPort.getValue());

                    }
                }
            }
        }
    }

    /**
     * Generate configuration for a cluster with the following format:
     * <p/>
     * <transport> {
     * upstream <cluster-hostname> {
     * server <hostname>:<port>;
     * server <hostname>:<port>;
     * }
     * server {
     * listen <proxy-port>;
     * server_name <cluster-hostname>;
     * location / {
     * proxy_pass    http://<cluster-hostname>
     * }
     * location /nginx_status {
     * stub_status on;
     * access_log off;
     * allow 127.0.0.1;
     * deny all;
     * }
     * }
     * }
     *
     * @param cluster
     * @param availPort
     * @param text
     */
    private void generateConfigurationForCluster(Cluster cluster, Port availPort, StringBuilder text) {

        for (String hostname : cluster.getHostNames()) {
            boolean memberFound = false;
            //Checking whether at-least one member is available to create
            // the upstream and server blocks
            for (Member member : cluster.getMembers()) {
                Collection<Port> ports = member.getPorts();
                for (Port port : ports) {
                    if ((port.getProtocol().equals(availPort.getProtocol())) &&
                            (port.getProxy() == availPort.getProxy())) {
                        memberFound = true;
                        break;
                    }
                }
                if(memberFound) {
                    break;
                }
            }
            if(memberFound) {
                // Start upstream block
                text.append(TAB).append("upstream ").append(hostname).append(" {").append(NEW_LINE);
                for (Member member : cluster.getMembers()) {
                    Port selectedPort = null;
                    Collection<Port> ports = member.getPorts();
                    for (Port port : ports) {
                        if ((port.getProtocol().equals(availPort.getProtocol())) &&
                                (port.getProxy() == availPort.getProxy())) {
                            selectedPort = port;
                            break;
                        }
                    }

                    if (selectedPort != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("The selected Port for cluster: " + cluster.getClusterId()
                                    + " is " + selectedPort.getValue() + " " +
                                    selectedPort.getProtocol() + " " + selectedPort.getProxy());
                        }
                        // Start upstream server block
                        text.append(TAB).append(TAB).append("server ").append(member.getHostName()).append(":")
                                .append(selectedPort.getValue()).append(";").append(NEW_LINE);
                        // End upstream server block
                    }
                }
                text.append(TAB).append("}").append(NEW_LINE);
                // End upstream block

                // Start server block
                text.append(NEW_LINE);
                text.append(TAB).append("server {").append(NEW_LINE);
                if (availPort.getProtocol().equals("https")) {
                    text.append(TAB).append(TAB).append("listen ").append(availPort.getProxy()).append(" ssl;").append(NEW_LINE);
                } else {
                    text.append(TAB).append(TAB).append("listen ").append(availPort.getProxy()).append(";").append(NEW_LINE);
                }
                text.append(TAB).append(TAB).append("server_name ").append(hostname).append(";").append(NEW_LINE);

                text.append(TAB).append(TAB).append("location / {").append(NEW_LINE);
                if (availPort.getProtocol().equals("https")) {
                    text.append(TAB).append(TAB).append(TAB).append("proxy_pass").append(TAB)
                            .append("https://").append(hostname).append(";").append(NEW_LINE);
                } else {
                    text.append(TAB).append(TAB).append(TAB).append("proxy_pass").append(TAB)
                            .append("http://").append(hostname).append(";").append(NEW_LINE);
                }
                text.append(TAB).append(TAB).append("}").append(NEW_LINE);

                text.append(TAB).append(TAB).append("location /nginx_status {").append(NEW_LINE);
                text.append(TAB).append(TAB).append(TAB).append("stub_status on;").append(NEW_LINE);
                text.append(TAB).append(TAB).append(TAB).append("access_log off;").append(NEW_LINE);
                text.append(TAB).append(TAB).append(TAB).append("allow 127.0.0.1;").append(NEW_LINE);
                text.append(TAB).append(TAB).append(TAB).append("deny all;").append(NEW_LINE);
                text.append(TAB).append(TAB).append("}").append(NEW_LINE);

                if (availPort.getProtocol().equals("https")) {
                    text.append(TAB).append(TAB).append("ssl on;").append(NEW_LINE);
                    text.append(TAB).append(TAB).append("ssl_certificate ").append(System.getProperty("nginx.cert.path")).append(";").append(NEW_LINE);
                    text.append(TAB).append(TAB).append("ssl_certificate_key ").append(System.getProperty("nginx.key.path")).append(";").append(NEW_LINE);
                }

                text.append(TAB).append("}").append(NEW_LINE);
                // End server block
            }
        }
    }
}
