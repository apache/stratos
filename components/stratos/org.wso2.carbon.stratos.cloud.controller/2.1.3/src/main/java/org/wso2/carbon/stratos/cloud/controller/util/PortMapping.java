package org.wso2.carbon.stratos.cloud.controller.util;

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

}
