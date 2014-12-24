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
package org.apache.stratos.cloud.controller.iaases.kubernetes;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.Partition;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.exception.NonExistingKubernetesClusterException;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;

import java.util.Properties;

/**
 * Kubernetes partition validator
 */
public class KubernetesPartitionValidator implements PartitionValidator {

    private static final Log log = LogFactory.getLog(KubernetesPartitionValidator.class);

    private IaasProvider iaasProvider;

    @Override
    public void setIaasProvider(IaasProvider iaasProvider) {
        this.iaasProvider = iaasProvider;
    }

    /**
     * Validate the given properties for its existent in this partition.
     *
     * @param partition  partition.
     * @param properties set of properties to be validated.
     * @return cloned and modified {@link IaasProvider} which maps to the given partition.
     * @throws InvalidPartitionException if at least one property is evaluated to be invalid.
     */
    public IaasProvider validate(Partition partition, Properties properties) throws InvalidPartitionException {

        String kubernetesClusterId = partition.getKubernetesClusterId();
        if (StringUtils.isBlank(kubernetesClusterId)) {
            String message = "Kubernetes cluster id not found in partition: [partition-id] " + partition.getId();
            log.error(message);
            throw new InvalidPartitionException(message);
        }

        try {
            CloudControllerContext.getInstance().getKubernetesCluster(kubernetesClusterId);
            return iaasProvider;
        } catch (NonExistingKubernetesClusterException e) {
            String message = "Kubernetes partition is not valid: [partition-id] " + partition.getId();
            log.error(message, e);
            throw new InvalidPartitionException(message, e);
        }
    }
}
