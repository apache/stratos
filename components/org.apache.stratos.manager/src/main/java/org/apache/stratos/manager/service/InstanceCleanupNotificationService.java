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
package org.apache.stratos.manager.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.publisher.InstanceNotificationPublisher;
import org.wso2.carbon.core.AbstractAdmin;

/**
 * This service will get invoked by Autoscaler when it trying to scale down an instance
 * in order for the instance to perform clean up task before the actual termination.
 */
public class InstanceCleanupNotificationService extends AbstractAdmin {
    private static final Log log = LogFactory.getLog(InstanceCleanupNotificationService.class);

    public void sendInstanceCleanupNotificationOnTermination(String memberId) {
        //sending the notification event to the instance
        new InstanceNotificationPublisher().sendInstanceCleanupEvent(memberId);
    }
}
