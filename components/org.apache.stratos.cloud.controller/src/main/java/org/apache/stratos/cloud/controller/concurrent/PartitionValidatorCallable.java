/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.concurrent;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.services.impl.CloudControllerServiceUtil;

public class PartitionValidatorCallable implements Callable<IaasProvider> {

    private static final Log log = LogFactory.getLog(PartitionValidatorCallable.class);
    private Partition partition;
    private Cartridge cartridge;

    public PartitionValidatorCallable(Partition partition, Cartridge cartridge) {
        this.partition = partition;
        this.cartridge = cartridge;
    }

    @Override
    public IaasProvider call() throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("Partition validation started for " + partition + " of " + cartridge);
        }
        String provider = partition.getProvider();
        IaasProvider iaasProvider = CloudControllerContext.getInstance().getIaasProvider(cartridge.getType(), provider);

        IaasProvider updatedIaasProvider =
                CloudControllerServiceUtil.validatePartitionAndGetIaasProvider(partition, iaasProvider);

        if (log.isDebugEnabled()) {
            log.debug("Partition " + partition.toString() + " is validated successfully " + "against the Cartridge: "
                    + cartridge.getType());
        }

        return updatedIaasProvider;
    }
}
