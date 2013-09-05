package org.apache.stratos.cartridge.mgt.ui;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.reponotification.stub.RepoNotificationServiceException;
import org.apache.stratos.adc.reponotification.stub.RepoNotificationServiceStub;

import java.rmi.RemoteException;
import java.util.Locale;
import java.util.ResourceBundle;

public class RepoNotificationClient {

	private ResourceBundle bundle;
	public RepoNotificationServiceStub stub;
	public static final String BUNDLE = "org.apache.stratos.cartridge.mgt.ui.i18n.Resources";
	private static final Log log = LogFactory.getLog(RepoNotificationClient.class);

	public RepoNotificationClient(String cookie, String backendServerURL,
			ConfigurationContext configCtx, Locale locale) throws AxisFault {
		String serviceURL = backendServerURL + "RepoNotificationService";
		bundle = ResourceBundle.getBundle(BUNDLE, locale);

		stub = new RepoNotificationServiceStub(configCtx, serviceURL);
		ServiceClient client = stub._getServiceClient();
		Options option = client.getOptions();
		option.setManageSession(true);
		option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
		option.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
	}

	public void synchronize(String repositoryURL) throws AxisFault {
		try {
			stub.synchronize(repositoryURL);
		} catch (RemoteException e) {
			handleException("cannot.unsubscribe", e);
		} catch (RepoNotificationServiceException e) {
			handleException("cannot.unsubscribe", e);
		}
	}

	private void handleException(String msgKey, Exception e) throws AxisFault {
		String msg = bundle.getString(msgKey);
		log.error(msg, e);
		throw new AxisFault(msg, e);
	}
}
