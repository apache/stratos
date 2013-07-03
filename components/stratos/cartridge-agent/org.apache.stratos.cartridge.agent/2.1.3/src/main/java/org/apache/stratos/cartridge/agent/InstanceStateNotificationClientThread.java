/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.stratos.cartridge.agent;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.registrant.Registrant;
import org.wso2.carbon.adc.instanceinfo.mgt.stub.InstanceInformationManagementServiceStub;

import java.rmi.RemoteException;

public class InstanceStateNotificationClientThread implements Runnable {

	private Registrant registrant;
	private String state;

	private static final Log log = LogFactory
			.getLog(InstanceStateNotificationClientThread.class);

	public InstanceStateNotificationClientThread(Registrant registrant,
			String state) {
		this.registrant = registrant;
		this.state = state;
	}

	public void run() {
		try {
			log.info("Instance State is updated to " + state + " "
					+ registrant.getRemoteHost());
			String serviceURL = "https://" + System.getProperty("adc.host")
					+ ":" + System.getProperty("adc.port")
					+ "/services/InstanceInformationManagementService";
			InstanceInformationManagementServiceStub stub = new InstanceInformationManagementServiceStub(
					serviceURL);
			stub.updateInstanceState(registrant.getRemoteHost(), 123,
					registrant.retrieveClusterDomain(), "__$default",
					registrant.getService(), state);
		} catch (AxisFault e) {
            log.warn("Error notifying state " + state + " of registrant " + registrant + " to ADC", e);
		} catch (RemoteException e) {
            log.warn("Error notifying state " + state + " of registrant " + registrant + " to ADC", e);
		}

	}
}
