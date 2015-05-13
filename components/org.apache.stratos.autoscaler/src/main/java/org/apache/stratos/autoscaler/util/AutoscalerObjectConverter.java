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

package org.apache.stratos.autoscaler.util;

import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.partition.PartitionRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Autoscaler object converter.
 */
public class AutoscalerObjectConverter {

    /**
     * Convert autoscaler partitions to cloud controller stub partitions
     *
     * @param partitions
     * @return
     */
    public static org.apache.stratos.cloud.controller.stub.domain.Partition[]
    convertPartitionsToCCPartitions(PartitionRef[] partitions) {

        List<org.apache.stratos.cloud.controller.stub.domain.Partition> cloudControllerPartitions
                = new ArrayList<org.apache.stratos.cloud.controller.stub.domain.Partition>();
        for (PartitionRef partition : partitions) {
            org.apache.stratos.cloud.controller.stub.domain.Partition cloudControllerPartition = convertPartitionToCCPartition(partition);
            cloudControllerPartitions.add(cloudControllerPartition);
        }
        return cloudControllerPartitions.toArray(
                new org.apache.stratos.cloud.controller.stub.domain.Partition[cloudControllerPartitions.size()]);
    }

    /**
     * Convert autoscaler partition to cloud controller partition
     *
     * @param partition
     * @return
     */
    public static org.apache.stratos.cloud.controller.stub.domain.Partition
    convertPartitionToCCPartition(PartitionRef partition) {

        org.apache.stratos.cloud.controller.stub.domain.Partition cloudControllerPartition = new
                org.apache.stratos.cloud.controller.stub.domain.Partition();

        cloudControllerPartition.setId(partition.getId());
        cloudControllerPartition.setDescription(partition.getDescription());
        cloudControllerPartition.setKubernetesClusterId(partition.getKubernetesClusterId());
        cloudControllerPartition.setProperties(AutoscalerUtil.toStubProperties(partition.getProperties()));

        return cloudControllerPartition;
    }


    public static org.apache.stratos.cloud.controller.stub.Properties convertPropertiesToCCProperties(Properties properties) {

        org.apache.stratos.cloud.controller.stub.Properties ccProperties
                = new org.apache.stratos.cloud.controller.stub.Properties();

        Property[] propertyArray = properties.getProperties();

        for (Property property : properties.getProperties()) {

            org.apache.stratos.cloud.controller.stub.Property ccProperty
                    = new org.apache.stratos.cloud.controller.stub.Property();
            ccProperty.setName(property.getName());
            ccProperty.setName(property.getName());
            ccProperties.addProperties(ccProperty);
        }
        return ccProperties;
    }

    public static Properties convertCCPropertiesToProperties(
            org.apache.stratos.cloud.controller.stub.Properties ccProperties) {

        Properties properties = new Properties();

        if (ccProperties.getProperties() != null) {
            for (org.apache.stratos.cloud.controller.stub.Property ccProperty : ccProperties.getProperties()) {

                Property property = new Property();
                property.setName(ccProperty.getName());
                property.setValue(ccProperty.getValue());
                properties.addProperty(property);
            }
        }
        return properties;
    }

    public static PartitionRef[] convertCCPartitionsToPartitions(org.apache.stratos.cloud.controller.stub.domain.Partition[] ccPartitions) {

        List<PartitionRef> partitions = new ArrayList<PartitionRef>();
        for (org.apache.stratos.cloud.controller.stub.domain.Partition ccPartition : ccPartitions) {
            PartitionRef partition = convertCCPartitionToPartition(ccPartition);
            partitions.add(partition);
        }
        return partitions.toArray(new PartitionRef[ccPartitions.length]);

    }

    public static PartitionRef convertCCPartitionToPartition(org.apache.stratos.cloud.controller.stub.domain.Partition ccPartition) {

        PartitionRef partition = new PartitionRef();

        partition.setId(ccPartition.getId());
        partition.setDescription(ccPartition.getDescription());
        partition.setProperties(convertCCPropertiesToProperties(ccPartition.getProperties()));

        return partition;
    }

}
