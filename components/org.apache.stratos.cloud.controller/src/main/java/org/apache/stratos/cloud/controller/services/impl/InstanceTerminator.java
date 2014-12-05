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
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.exception.InvalidCartridgeTypeException;
import org.apache.stratos.cloud.controller.exception.InvalidMemberException;

import java.util.concurrent.locks.Lock;

/**
 * Instance terminator runnable.
 */
public class InstanceTerminator implements Runnable {

    private static final Log log = LogFactory.getLog(InstanceTerminator.class);

    private MemberContext ctxt;

    public InstanceTerminator(MemberContext ctxt) {
        this.ctxt = ctxt;
    }

    @Override
    public void run() {
        String memberId = ctxt.getMemberId();
        String clusterId = ctxt.getClusterId();
        String partitionId = ctxt.getPartition().getId();
        String cartridgeType = ctxt.getCartridgeType();
        String nodeId = ctxt.getNodeId();

        Lock lock = null;
        try {
            CloudControllerContext.getInstance().acquireMemberContextWriteLock();

            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
            log.info("Starting to terminate an instance with member id : " + memberId +
                    " in partition id: " + partitionId + " of cluster id: " + clusterId +
                    " and of cartridge type: " + cartridgeType);

            if (cartridge == null) {
                String msg = "Termination of Member Id: " + memberId + " failed. " +
                        "Cannot find a matching Cartridge for type: " +
                        cartridgeType;
                log.error(msg);
                throw new InvalidCartridgeTypeException(msg);
            }

            // if no matching node id can be found.
            if (nodeId == null) {
                String msg = "Termination failed. Cannot find a node id for Member Id: " + memberId;

                // log information
                CloudControllerServiceUtil.logTermination(ctxt);
                log.error(msg);
                throw new InvalidMemberException(msg);
            }

            IaasProvider iaasProvider = cartridge.getIaasProviderOfPartition(partitionId);

            // terminate it!
            CloudControllerServiceUtil.terminate(iaasProvider, nodeId, ctxt);

            // log information
            CloudControllerServiceUtil.logTermination(ctxt);
        } catch (Exception e) {
            String msg = "Instance termination failed. " + ctxt.toString();
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        } finally {
            if(lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }
}