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
import org.apache.stratos.common.partition.Partition;

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
    convertPartitionsToCCPartitions(Partition[] partitions) {

        List<org.apache.stratos.cloud.controller.stub.domain.Partition> cloudControllerPartitions
                = new ArrayList<org.apache.stratos.cloud.controller.stub.domain.Partition>();
        for (Partition partition : partitions) {
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
    convertPartitionToCCPartition(Partition partition) {

        org.apache.stratos.cloud.controller.stub.domain.Partition cloudControllerPartition = new
                org.apache.stratos.cloud.controller.stub.domain.Partition();

        cloudControllerPartition.setId(partition.getId());
        cloudControllerPartition.setProvider(partition.getProvider());
        cloudControllerPartition.setDescription(partition.getDescription());
        cloudControllerPartition.setKubernetesClusterId(partition.getKubernetesClusterId());
        cloudControllerPartition.setIsPublic(partition.getIsPublic());
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

    public static Partition[] convertCCPartitionsToPartitions(org.apache.stratos.cloud.controller.stub.domain.Partition[] ccPartitions) {

        List<Partition> partitions = new ArrayList<Partition>();
        for (org.apache.stratos.cloud.controller.stub.domain.Partition ccPartition : ccPartitions) {
            Partition partition = convertCCPartitionToPartition(ccPartition);
            partitions.add(partition);
        }
        return partitions.toArray(new Partition[ccPartitions.length]);

    }

    public static Partition convertCCPartitionToPartition(org.apache.stratos.cloud.controller.stub.domain.Partition ccPartition) {

        Partition partition = new Partition();

        partition.setId(ccPartition.getId());
        partition.setProvider(ccPartition.getProvider());
        partition.setDescription(ccPartition.getDescription());
        partition.setIsPublic(ccPartition.getIsPublic());
        partition.setProperties(convertCCPropertiesToProperties(ccPartition.getProperties()));

        return partition;
    }

//
//    public static org.apache.stratos.cloud.controller.stub.Properties toStubProperties(
//            org.apache.stratos.common.Properties properties) {
//        org.apache.stratos.cloud.controller.stub.Properties stubProps = new org.apache.stratos.cloud.controller.stub.Properties();
//
//        if (properties != null && properties.getProperties() != null) {
//            for (org.apache.stratos.common.Property property : properties.getProperties()) {
//                if ((property != null) && (property.getValue() != null)) {
//                    org.apache.stratos.cloud.controller.stub.Property newProperty = new org.apache.stratos.cloud.controller.stub.Property();
//                    newProperty.setName(property.getName());
//                    newProperty.setValue(property.getValue());
//                    stubProps.addProperties(newProperty);
//                }
//            }
//
//        }
//        return stubProps;
//    }
//
//    public static org.apache.stratos.cloud.controller.stub.Properties toStubProperties(
//            java.util.Properties properties) {
//        org.apache.stratos.cloud.controller.stub.Properties stubProperties = new org.apache.stratos.cloud.controller.stub.Properties();
//        if(properties != null) {
//            for(Map.Entry<Object, Object> entry : properties.entrySet()) {
//                org.apache.stratos.cloud.controller.stub.Property newProperty = new org.apache.stratos.cloud.controller.stub.Property();
//                newProperty.setName(entry.getKey().toString());
//                newProperty.setValue(entry.getValue().toString());
//                stubProperties.addProperties(newProperty);
//            }
//        }
//        return stubProperties;
//    }
//
//    public static org.apache.stratos.common.Properties toCommonProperties(
//            org.apache.stratos.cloud.controller.stub.Properties properties) {
//        org.apache.stratos.common.Properties commonProps = new org.apache.stratos.common.Properties();
//
//        if (properties != null && properties.getProperties() != null) {
//
//            for (org.apache.stratos.cloud.controller.stub.Property property : properties.getProperties()) {
//                if ((property != null) && (property.getValue() != null)) {
//                    org.apache.stratos.common.Property newProperty = new org.apache.stratos.common.Property();
//                    newProperty.setName(property.getName());
//                    newProperty.setValue(property.getValue());
//                    commonProps.addProperty(newProperty);
//                }
//            }
//
//        }
//
//        return commonProps;
//    }
//
//    public static org.apache.stratos.common.Properties toCommonProperties(
//            org.apache.stratos.cloud.controller.stub.Property[] propertyArray) {
//
//        org.apache.stratos.cloud.controller.stub.Properties properties = new org.apache.stratos.cloud.controller.stub.Properties();
//        properties.setProperties(propertyArray);
//        return toCommonProperties(properties);
//    }
//

//    public static NetworkPartition convertNetworkParitionStubToPojo(org.apache.stratos.cloud.controller.stub.domain.NetworkPartition np) {
//    	
//    	NetworkPartition networkPartition = new NetworkPartition();
//    	networkPartition.setId(np.getId());
//    	networkPartition.setKubernetesClusterId(np.getKubernetesClusterId());
//    	networkPartition.setPartitions(convertPartitionStubToPojo(np.getPartitions()));
//    	
//    	return networkPartition;
//    }

//	private static Partition[] convertPartitionStubToPojo(
//            Partition[] partitions) {
//		
//		List<Partition> partitionList = 
//				new ArrayList<Partition>();
//		
//		for (Partition p : partitions) {
//			Partition partition =
//					new Partition();
//			partition.setDescription(p.getDescription());
//			partition.setId(p.getId());
//			partition.setIsPublic(p.getIsPublic());
//			partition.setKubernetesClusterId(p.getKubernetesClusterId());
//			partition.setProperties(convertProperties(p.getProperties()));
//			partition.setProvider(p.getProvider());
//        }
//		
//	    return partitionList.toArray(
//	    		new Partition[partitions.length]);
//    }

//	private static Properties convertProperties(
//            org.apache.stratos.cloud.controller.stub.Properties properties) {
//	    
//		Properties props = new Properties();
//		Property[] propArray = properties.getProperties();
//		for (Property property : propArray) {
//	        props.addProperty(new Property(property.getName(),property.getValue()));
//        }
//		
//	    return props;
//    }
}
