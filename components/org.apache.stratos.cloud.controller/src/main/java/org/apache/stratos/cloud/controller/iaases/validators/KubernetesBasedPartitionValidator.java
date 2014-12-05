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
package org.apache.stratos.cloud.controller.iaases.validators;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.exception.NonExistingKubernetesGroupException;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.kubernetes.KubernetesGroup;

/**
 * Kubernetes Partition Validator
 */
public class KubernetesBasedPartitionValidator implements PartitionValidator {
    
    private static final Log log = LogFactory.getLog(KubernetesBasedPartitionValidator.class);

    /**
     * Validate the given properties for its existent in this partition.
     * 
     * @param partitionId partition id.
     * @param properties set of properties to be validated.
     * @return cloned and modified {@link IaasProvider} which maps to the given partition.
     * @throws InvalidPartitionException if at least one property is evaluated to be invalid.
     */
    public KubernetesGroup validate(String partitionId, Properties properties) throws InvalidPartitionException {

        if (properties.containsKey(StratosConstants.KUBERNETES_CLUSTER_ID)) {
            String kubernetesClusterId = properties.getProperty(StratosConstants.KUBERNETES_CLUSTER_ID);
            try {
                KubernetesGroup kubGroup = CloudControllerContext.getInstance().getKubernetesGroup(kubernetesClusterId);
                return kubGroup;
            } catch (NonExistingKubernetesGroupException e) {
                String msg = "Invalid Partition Detected : " + partitionId + ". Cause: " + e.getMessage();
                log.error(msg, e);
                throw new InvalidPartitionException(msg, e);
            }
        }

        String msg =
                "Invalid Partition Detected : " + partitionId + ". Cause: Essential "
                        + StratosConstants.KUBERNETES_CLUSTER_ID + " property not found.";
        log.error(msg);
        throw new InvalidPartitionException(msg);

    }
}
