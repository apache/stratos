/*
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.stratos.adc.topology.mgt.service;

import org.apache.stratos.adc.topology.mgt.serviceobjects.DomainContext;

/**
 * This exposes details regarding up-to-date topology
 *
 */
public interface TopologyManagementService {

	/**
	 * Provide service cluster domains corresponds to the given information.
	 * @param cartridgeType cartridge type
	 * @param tenantId tenant id
	 * @return String array of service cluster domains
	 */
	String[] getDomains(String cartridgeType, int tenantId);

	/**
	 * Provide service cluster sub domains corresponds to the given information.
	 * @param cartridgeType cartridge type
	 * @param tenantId tenant id
	 * @return String array of service cluster sub domains
	 */
	String[] getSubDomains(String cartridgeType, int tenantId);

//	/**
//	 * Provide public IPs corresponds to the given information.
//	 * @param cartridgeType cartridge type
//	 * @param tenantId tenant id
//	 * @return String array of public IPs
//	 */
//	String[] getActiveIPs(String cartridgeType, int tenantId);
	
	/**
	 * Provide public IPs corresponds to the given information.
	 * @param cartridgeType cartridge type
	 * @param domain service cluster domain
	 * @param subDomain service cluster sub domain
	 * @return String array of public IPs
	 */
	String[] getActiveIPs(String cartridgeType, String domain, String subDomain);
	
	/**
	 * Provide domains and the relevant subdomains corresponding to the given information
	 * 
	 * @param cartridgeType cartridge type
	 * @param tenantId tenant id
	 * @return DomainContext instances array of domains and sub domains 
	 */
	DomainContext[] getDomainsAndSubdomains (String cartridgeType, int tenantId);
	
}
