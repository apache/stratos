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
 * KIND, either express or implied.  TcSee the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.client;

import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.manager.cleanup.notification.stub.*;

import java.rmi.RemoteException;

public class InstanceNotificationClient {
    private static final Log log = LogFactory.getLog(InstanceNotificationClient.class);
    private static InstanceCleanupNotificationServiceStub stub;

    /* An instance of a InstanceNotificationClient is created when the class is loaded.
     * Since the class is loaded only once, it is guaranteed that an object of
     * InstanceNotificationClient is created only once. Hence it is singleton.
     */
    private static class InstanceHolder {
        private static final InstanceNotificationClient INSTANCE = new InstanceNotificationClient();
    }

    public static InstanceNotificationClient getInstance() {
    	return InstanceHolder.INSTANCE;
    }

    private InstanceNotificationClient(){
    	try {
            XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
            int port = conf.getInt(Constants.STRATOS_MANAGER_DEFAULT_PORT_ELEMENT,
                    Constants.STRATOS_MANAGER_DEFAULT_PORT);
            String hostname = conf.getString(Constants.STRATOS_MANAGER_HOSTNAME_ELEMENT, "localhost");
            String epr = "https://" + hostname + ":" + port + "/" + Constants.STRATOS_MANAGER_SERVICE_SFX;
            int instanceNotificationTimeOut = conf.getInt(Constants.STRATOS_MANAGER_CLIENT_TIMEOUT_ELEMENT, 180000);
            stub = new InstanceCleanupNotificationServiceStub(epr);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, instanceNotificationTimeOut);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, instanceNotificationTimeOut);
		} catch (Exception e) {
			log.error("Stub init error", e);
		}
    }

    public void sendMemberCleanupEvent(String memberId) {
        try {
            stub.sendInstanceCleanupNotificationForMember(memberId);
        } catch (RemoteException e)  {
            log.error("error while sending the cleanup notification event to SM", e);
        }

    }
}
