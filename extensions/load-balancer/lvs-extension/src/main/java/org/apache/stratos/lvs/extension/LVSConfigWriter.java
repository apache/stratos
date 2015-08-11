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
import org.apache.stratos.load.balancer.common.domain.*;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;

/**
 * LVS load balancer configuration writer.
 */
public class LVSConfigWriter {

	private static final Log log = LogFactory.getLog(Main.class);
	private static final String NEW_LINE = System.getProperty("line.separator");
	private static final String TAB = "    ";

	private String templatePath;
	private String templateName;
	private String confFilePath;
	private String statsSocketFilePath;
	private String virtualIPsForServices;
	private String serverState;
	private String scheduleAlgo;
	private boolean isKeepAlived=false;

	public LVSConfigWriter(String templatePath, String templateName, String confFilePath,
	                       String statsSocketFilePath, String virtualIPsForServices, String serverState,
	                       String scheduleAlgo) {

		this.templatePath = templatePath;
		this.templateName = templateName;
		this.confFilePath = confFilePath;
		this.statsSocketFilePath = statsSocketFilePath;
		this.virtualIPsForServices = virtualIPsForServices;
		this.serverState = serverState;
		this.scheduleAlgo = scheduleAlgo;
	}

	public boolean write(Topology topology) {

		StringBuilder configurationBuilder = new StringBuilder();
		StringBuilder virtualIPBuilder = new StringBuilder();
		String state;
		for (Service service : topology.getServices()) {
			for (Cluster cluster : service.getClusters()) {
				if ((service.getPorts() == null) || (service.getPorts().size() == 0)) {
					throw new RuntimeException(
							String.format("No ports found in service: %s", service.getServiceName()));
				}
				generateConfigurationForCluster(cluster, service.getPorts(), configurationBuilder, virtualIPBuilder,
				                                virtualIPsForServices, scheduleAlgo,isKeepAlived);
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
		context.put("configuration", configurationBuilder.toString());
		context.put("virtualips", virtualIPBuilder.toString());
		context.put("state", serverState);

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

	/**
	 * Generate configuration for a cluster with the following format:
	 * <p/>
	 * virtual_server 10.10.10.10 80 {
	 * delay_loop 10
	 * lvs_sched wlc
	 * lvs_method DR
	 * persistence_timeout 5
	 * protocol TCP
	 * <p/>
	 * real_server 10.10.10.41 80 {
	 * weight 50
	 * TCP_CHECK {
	 * connect_timeout 3
	 * }
	 * }
	 * <p/>
	 * real_server 10.10.10.42 80 {
	 * weight 50
	 * TCP_CHECK {
	 * connect_timeout 3
	 * }
	 * }
	 * }
	 *
	 * @param cluster
	 * @param ports
	 * @param text
	 */
	private void generateConfigurationForCluster(Cluster cluster, Collection<Port> ports, StringBuilder text,
	                                             StringBuilder virtualIPs, String virtualIPsForServices,
	                                             String scheduleAlgo,boolean isKeepAlived) {
		String commandClear = "ipvsadm --clear";
		try {
			CommandUtils.executeCommand(commandClear);
		} catch (IOException e) {
			if (log.isErrorEnabled()) {
				log.error(String.format("Could not run the command: %s", commandClear));
			}
			throw new RuntimeException(e);
		}

		String[] virtualIPForServiceArray;
		if (virtualIPsForServices.contains(",")) {
			virtualIPForServiceArray = virtualIPsForServices.split(",");
		} else {
			virtualIPForServiceArray = new String[1];
			virtualIPForServiceArray[0] = virtualIPsForServices;
		}
		boolean isServiceAvailable = false;
		for (int i = 0; i < virtualIPForServiceArray.length; i++) {
			String[] virtualIpForService = virtualIPForServiceArray[i].split("\\|");
		    for (Port port : ports) {
			    String command = "ipvsadm -A -t " + virtualIpForService[1]+":" + port.getProxy()+ " -s " + scheduleAlgo;

			    try {
				    CommandUtils.executeCommand(command);
			    } catch (IOException e) {
				    if (log.isErrorEnabled()) {
					    log.error(String.format("Could not run the command: %s", command));
				    }
				    throw new RuntimeException(e);
			    }

			    for (String hostname : cluster.getHostNames()) {
				    if (virtualIpForService[0].equals(cluster.getServiceName())) {

					    text.append("virtual_server ").append(virtualIpForService[1]).append(" ")
					        .append(port.getProxy()).append(
							    " {").append(
							    NEW_LINE);
					    text.append(TAB).append("delay_loop 10").append(NEW_LINE);
					    text.append(TAB).append("lvs_sched ").append(scheduleAlgo).append(NEW_LINE);
					    text.append(TAB).append("lvs_method DR").append(NEW_LINE);
					    text.append(TAB).append("persistence_timeout 5").append(NEW_LINE);
					    text.append(TAB).append("protocol TCP").append(NEW_LINE).append(NEW_LINE);

					    //Start real servers block

					    for (Member member : cluster.getMembers()) {
						    // Start upstream server block
						    text.append(TAB).append("real_server ").append(member.getHostName()).append(" ")
						        .append(port.getValue()).append(" {")
						        .append(NEW_LINE);
						    text.append(TAB).append(TAB).append("weight 50").append(NEW_LINE);
						    text.append(TAB).append(TAB).append("TCP_CHECK {").append(NEW_LINE);
						    text.append(TAB).append(TAB).append(TAB).append("connect_timeout 3").append(NEW_LINE);
						    text.append(TAB).append(TAB).append("}").append(NEW_LINE);
						    text.append(TAB).append("}").append(NEW_LINE);
						    String commandMember =
								    "ipvsadm -a -t " + virtualIpForService[1] + ":" + port.getProxy() + " -r " +
								    member.getHostName() + ":" + port.getValue() + " -m";

						    try {
							    CommandUtils.executeCommand(commandMember);
						    } catch (IOException e) {
							    if (log.isErrorEnabled()) {
								    log.error(String.format("Could not run the command: %s", commandMember));
							    }
							    throw new RuntimeException(e);
						    }
					    }
					    text.append("}").append(NEW_LINE);
					    isServiceAvailable = true;
					    virtualIPs.append(TAB).append(TAB).append(virtualIpForService[1]).append(NEW_LINE);

				    }

			    }
		    }
			if (!isServiceAvailable) {
				log.warn(String.format("Given service is not available in the topology %s", virtualIpForService[0]));
			}
		}

	}
}
