/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.stratos.manager.reponotification;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RepoNotificationServiceClient {

    private static final Log log = LogFactory.getLog(RepoNotificationServiceClient.class);
    private static volatile RepoNotificationServiceClient serviceClient;

    public static RepoNotificationServiceClient getServiceClient() throws AxisFault {
        if (serviceClient == null) {
            synchronized (RepoNotificationServiceClient.class) {
                if (serviceClient == null) {
                    serviceClient = new RepoNotificationServiceClient();
                }
            }
        }
        return serviceClient;
    }

    public void getPayload(String url) {
        System.out.println("Repo URL : " + url);
    }

}
