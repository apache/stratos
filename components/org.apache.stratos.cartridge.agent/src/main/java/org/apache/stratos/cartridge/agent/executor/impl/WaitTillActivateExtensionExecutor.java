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

package org.apache.stratos.cartridge.agent.executor.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.executor.ExtensionExecutor;

/**
 * This extension is suppose to start all the event listeners that the Cartridge Agent
 * needs.
 * 
 */
public class WaitTillActivateExtensionExecutor extends ExtensionExecutor {
	
	private static final Log log = LogFactory.getLog(WaitTillActivateExtensionExecutor.class);

	public WaitTillActivateExtensionExecutor() {
		super(WaitTillActivateExtensionExecutor.class.getName());
	}
	
	public WaitTillActivateExtensionExecutor(List<String> fileNames) {
		super.setFileNamesToBeExecuted(fileNames);
	}

	@Override
	public void execute() {
		long portCheckTimeOut = 1000 * 60 * 10;
		String ipAddress = "localhost";
		List<Integer> ports = CartridgeAgentConfiguration.getInstance().getPorts();
		
        String portCheckTimeOutStr = System.getProperty("port.check.timeout");
        if (StringUtils.isNotBlank(portCheckTimeOutStr)) {
            portCheckTimeOut = Integer.parseInt(portCheckTimeOutStr);
        }
        if (log.isDebugEnabled()) {
            log.debug("Port check timeout: " + portCheckTimeOut);
        }

        long startTime = System.currentTimeMillis();
        boolean active = false;
        while (!active) {
            if(log.isInfoEnabled()) {
                log.info("Waiting for ports to be active");
            }
            active = checkPortsActive(ipAddress,  ports);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            if (duration > portCheckTimeOut) {
                return;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
	}
	
	
	private static boolean checkPortsActive(String ipAddress, List<Integer> ports) {
        //List<Integer> ports = CartridgeAgentConfiguration.getInstance().getPorts();
        if (ports.size() == 0) {
            throw new RuntimeException("No ports found");
        }
        for (int port : ports) {
            Socket socket = null;
            try {
                SocketAddress httpSockaddr = new InetSocketAddress(ipAddress, port);
                socket = new Socket();
                socket.connect(httpSockaddr, 5000);
                if (log.isInfoEnabled()) {
                    log.info(String.format("Port %s is active", port));
                }
            } catch (Exception e) {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Port %s is not active", port));
                }
                return false;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return true;
    }

	@Override
	public void cleanUp() {
	}
	
	
}
