/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.stratos.cloud.controller.util;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.wso2.carbon.lb.common.conf.structure.Node;
import org.wso2.carbon.lb.common.conf.structure.NodeBuilder;
import org.wso2.carbon.lb.common.conf.util.Constants;

/**
 * We keep information regarding a service (i.e. a cartridge instance)
 * in this object.
 */
public class ServiceContext implements Serializable{

    private static final long serialVersionUID = -6740964802890082678L;
    private File file;
	private String domainName;
    private String subDomainName = Constants.DEFAULT_SUB_DOMAIN;
    private String tenantRange;
    private String hostName;
    private String payloadFilePath;
    private String cartridgeType;
    private Cartridge cartridge;
    private byte[] payload;
    /**
     * Key - Value pair.
     */
    private Map<String, String> properties = new HashMap<String, String>();
    /**
     * Key - IaaS Type
     * Value - {@link IaasContext} object
     */
    private Map<String, IaasContext> iaasCtxts = new HashMap<String, IaasContext>();
	
    public Map<String, IaasContext> getIaasCtxts() {
    	return iaasCtxts;
    }

	public String getDomainName() {
        return domainName;
    }
    
    public boolean setDomainName(String domainName) {
        if (!"".equals(domainName)) {
            this.domainName = domainName;
            return true;
        }
        
        return false;
    }
    
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
    
    public String getProperty(String key) {
        
        if(properties.containsKey(key)){
            return properties.get(key);
        }
        
        return "";
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public String getSubDomainName() {
        return subDomainName;
    }

    public void setSubDomainName(String subDomainName) {
        if(subDomainName == null || "".equals(subDomainName)){
            return;
        }
        this.subDomainName = subDomainName;
    }

    public Cartridge getCartridge() {
        return cartridge;
    }

    public void setCartridge(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

	public String getTenantRange() {
	    return tenantRange;
    }

	public void setTenantRange(String tenantRange) {
	    this.tenantRange = tenantRange;
    }
	
	public IaasContext addIaasContext(String iaasType){
		IaasContext ctxt = new IaasContext(iaasType);
		iaasCtxts.put(iaasType, ctxt);
		return ctxt;
	}
	
	public IaasContext getIaasContext(String type){
		return iaasCtxts.get(type);
	}
	
	public void setIaasContextMap(Map<String, IaasContext> map){
		iaasCtxts = map;
	}
	
//	public byte[] getPayload() {
//    	return payload;
//    }
//
//	public void setPayload(byte[] payload) {
//    	this.payload = payload;
//    }


	public String getPayloadFile() {
	    return payloadFilePath;
    }

	public void setPayloadFile(String payloadFile) {
	    this.payloadFilePath = payloadFile;
    }

	public String getHostName() {
		if(cartridge != null && (hostName == null || hostName.isEmpty())){
			return cartridge.getHostName();
		}
	    return hostName;
    }

	public void setHostName(String hostName) {
	    this.hostName = hostName;
    }

	public String getCartridgeType() {
	    return cartridgeType;
    }

	public void setCartridgeType(String cartridgeType) {
	    this.cartridgeType = cartridgeType;
    }

	public byte[] getPayload() {
	    return payload;
    }

	public void setPayload(byte[] payload) {
	    this.payload = payload;
    }
	
	public String toXml() {
		String str =
				payloadFilePath == null ? "<service domain=\"" + domainName +
				                        "\" subDomain=\"" + subDomainName +
				                        "\" tenantRange=\"" + tenantRange + "\">\n" +
				                        "\t<cartridge type=\"" + cartridgeType +
				                        "\"/>\n" + "\t<host>" + hostName +
				                        "</host>\n" + "</service>"
				                        
		                                    : "<service domain=\"" + domainName +
		                                    "\" subDomain=\"" + subDomainName +
		                                    "\" tenantRange=\"" + tenantRange + "\">\n" +
		                                    "\t<cartridge type=\"" + cartridgeType +
		                                    "\"/>\n"  + "\t<host>" + hostName +
		                                    "</host>\n" + "\t<payload>" + payloadFilePath +
		                                    "</payload>\n" +
		                                    propertiesToXml() +
		                                    "</service>";
		return str;
	}
	
	public Node toNode() {
		Node node = new Node();
		node.setName(cartridgeType);
		String sbrace = Constants.NGINX_NODE_START_BRACE;
		String ebrace = Constants.NGINX_NODE_END_BRACE;
		String delimiter = Constants.NGINX_LINE_DELIMITER;
		String newLine = "\n";
		String nginx = 
				Constants.DOMAIN_ELEMENT+sbrace+newLine+
				domainName+sbrace+newLine+
				Constants.HOSTS_ELEMENT+" "+hostName+delimiter+newLine+
				Constants.SUB_DOMAIN_ELEMENT+" "+subDomainName+delimiter+newLine+
				Constants.TENANT_RANGE_ELEMENT+" "+tenantRange+delimiter+newLine+
				propertiesToNginx()+
				ebrace+newLine+
				ebrace+newLine;
		
		return NodeBuilder.buildNode(node, nginx);
		
	}
	
	/**
	 * Had to made this public in order to access from a test case.
	 * @return
	 */
	public String propertiesToNginx() {
		StringBuilder builder = new StringBuilder("");
		for (Iterator<Entry<String, String>> iterator = getProperties().entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, String> prop = (Map.Entry<String, String>) iterator.next();

			String key = prop.getKey();
			String value = prop.getValue();
			if (key != null) {
				builder.append(key + " " + (value == null ? "" : value) +
				               Constants.NGINX_LINE_DELIMITER + "\n");
			}

		}

		return builder.toString();
	}
	
	public String propertiesToXml() {
		StringBuilder builder = new StringBuilder("");
		for (Iterator<Entry<String, String>> iterator = getProperties().entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, String> prop = (Map.Entry<String, String>) iterator.next();

			String key = prop.getKey();
			String value = prop.getValue();
			if (key != null) {
				builder.append("\t<property name=\""+key +"\" value=\"" + (value == null ? "" : value) +"\"/>\n");
			}

		}

		return builder.toString();
	}
	
	public File getFile() {
		return file;
	}
	
	public void setFile(File file) {
		this.file = file;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof ServiceContext) {
			return this.domainName.equals(((ServiceContext) obj).getDomainName()) &&
			       this.subDomainName.equals(((ServiceContext) obj).getSubDomainName());
		}
		return false;
	}
    
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
            append(domainName).
            append(subDomainName).
            toHashCode();
    }

}
