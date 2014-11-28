/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.domain;

import java.io.Serializable;

public class PortMapping implements Serializable{
	
    private static final long serialVersionUID = -5387564414633460306L;
	private String protocol;
	private String port;
	private String proxyPort;
	
	public PortMapping(){
		
	}
	
	public PortMapping(String protocol, String port, String proxyPort){
		this.protocol = protocol;
		this.port = port;
		this.proxyPort = proxyPort;
	}

	public String getProtocol() {
    	return protocol;
    }

	public void setProtocol(String protocol) {
    	this.protocol = protocol;
    }

	public String getPort() {
    	return port;
    }

	public void setPort(String port) {
    	this.port = port;
    }

	public String getProxyPort() {
    	return proxyPort;
    }

	public void setProxyPort(String proxyPort) {
    	this.proxyPort = proxyPort;
    }

    public String toString () {

        return "Protocol: " + protocol + ", Port: " + port + ", Proxy Port: " + proxyPort;
    }

}
