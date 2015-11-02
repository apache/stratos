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
                        // Adding member to hostname map against specific port
                        // that should contain this particular member
                        List<String> ipPortMapping = existingHostNameToServerMap.get(hostname);
                        String server = member.getHostName() + ":" + selectedPort.getValue();

                        if(!ipPortMapping.contains(server)) {
                            ipPortMapping.add(server);
                        }

                    }
                }
            }
        }
    }

}
