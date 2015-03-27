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

package org.apache.stratos.cloud.controller.services.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.iaases.Iaas;

import java.util.concurrent.locks.Lock;

/**
 * Instance terminator runnable.
 */
public class InstanceTerminator implements Runnable {

    private static final Log log = LogFactory.getLog(InstanceTerminator.class);

    private Iaas iaas;
    private MemberContext memberContext;

    public InstanceTerminator(MemberContext memberContext) {
        String provider = memberContext.getPartition().getProvider();
        IaasProvider iaasProvider = CloudControllerContext.getInstance()
                .getIaasProvider(memberContext.getCartridgeType(), provider);
        this.iaas = iaasProvider.getIaas();
        this.memberContext = memberContext;
    }

    @Override
    public void run() {
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireMemberContextWriteLock();
            // Terminate the instance
            iaas.terminateInstance(memberContext);

            // Execute member termination post process
            CloudControllerServiceUtil.executeMemberTerminationPostProcess(memberContext);
        } catch (Exception e) {
            String msg = "Instance termination failed! " + memberContext.toString();
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        } finally {
            if(lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }
}