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
package org.apache.stratos.cloud.controller.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.config.CloudControllerConfig;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.domain.InstanceMetadata;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.utils.CarbonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Usage data publisher for publishing instance usage data to BAM.
 */
public class BAMUsageDataPublisher {

    private static final Log log = LogFactory.getLog(BAMUsageDataPublisher.class);

    private static AsyncDataPublisher dataPublisher;
    private static StreamDefinition streamDefinition;
    private static final String cloudControllerEventStreamVersion = "1.0.0";

    /**
     * Publish events to BAM
     *
     * @param memberId          member id
     * @param partitionId       partition id
     * @param networkId         network partition id
     * @param clusterId         cluster id
     * @param clusterInstanceId cluster instance id
     * @param serviceName       service name
     * @param status            member status
     * @param timeStamp         time
     * @param autoscalingReason scaling reason related to member
     * @param scalingTime       scaling time
     * @param metadata          meta-data
     */
    public static void publish(String memberId,
                               String partitionId,
                               String networkId,
                               String clusterId,
                               String clusterInstanceId,
                               String serviceName,
                               String status,
                               Long timeStamp,
                               String autoscalingReason,
                               Long scalingTime,
                               InstanceMetadata metadata) {
        if (!CloudControllerConfig.getInstance().isBAMDataPublisherEnabled()) {
            return;
        }
        log.debug(CloudControllerConstants.DATA_PUB_TASK_NAME + " cycle started.");

        if (dataPublisher == null) {
            createDataPublisher();

            //If we cannot create a data publisher we should give up
            //this means data will not be published
            if (dataPublisher == null) {
                log.error("Data Publisher cannot be created or found.");
                release();
                return;
            }
        }

        MemberContext memberContext = CloudControllerContext.getInstance().getMemberContextOfMemberId(memberId);
        String cartridgeType = memberContext.getCartridgeType();
        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
        String instanceType = CloudControllerContext.getInstance().getIaasProviderOfPartition(cartridgeType,
                partitionId).getProperty(CloudControllerConstants.INSTANCE_TYPE);

        //Construct the data to be published
        List<Object> payload = new ArrayList<Object>();
        // Payload values
        payload.add(timeStamp);
        payload.add(memberId);
        payload.add(serviceName);
        payload.add(clusterId);
        payload.add(clusterInstanceId);
        payload.add(handleNull(memberContext.getLbClusterId()));
        payload.add(handleNull(partitionId));
        payload.add(handleNull(networkId));
        payload.add(handleNull(instanceType));
        payload.add(handleNull(autoscalingReason));
        payload.add(handleNull(scalingTime));
        if (cartridge != null) {
            payload.add(handleNull(String.valueOf(cartridge.isMultiTenant())));
        } else {
            payload.add("");
        }
        payload.add(handleNull(memberContext.getPartition().getProvider()));
        payload.add(handleNull(status));

        if (metadata != null) {
            payload.add(metadata.getHostname());
            payload.add(metadata.getHypervisor());
            payload.add(String.valueOf(metadata.getRam()));
            payload.add(metadata.getImageId());
            payload.add(metadata.getLoginPort());
            payload.add(metadata.getOperatingSystemName());
            payload.add(metadata.getOperatingSystemVersion());
            payload.add(metadata.getOperatingSystemArchitecture());
            payload.add(String.valueOf(metadata.isOperatingSystem64bit()));
        } else {
            payload.add("");
            payload.add("");
            payload.add("");
            payload.add("");
            payload.add(0);
            payload.add("");
            payload.add("");
            payload.add("");
            payload.add("");
        }

        payload.add(handleNull(Arrays.toString(memberContext.getPrivateIPs())));
        payload.add(handleNull(Arrays.toString(memberContext.getPublicIPs())));
        payload.add(handleNull(Arrays.toString(memberContext.getAllocatedIPs())));

        Event event = new Event();
        event.setPayloadData(payload.toArray());
        event.setArbitraryDataMap(new HashMap<String, String>());

        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Publishing BAM event: [stream] %s [version] %s", streamDefinition.getName(),
                        streamDefinition.getVersion()));
            }
            dataPublisher.publish(streamDefinition.getName(), streamDefinition.getVersion(), event);
        } catch (AgentException e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not publish BAM event: [stream] %s [version] %s",
                        streamDefinition.getName(), streamDefinition.getVersion()), e);
            }
        }
    }

    private static void release() {
        CloudControllerContext.getInstance().setPublisherRunning(false);
    }

    private static StreamDefinition initializeStream() throws Exception {
        streamDefinition = new StreamDefinition(
                CloudControllerConstants.CLOUD_CONTROLLER_EVENT_STREAM,
                cloudControllerEventStreamVersion);
        streamDefinition.setNickName("cloud.controller");
        streamDefinition.setDescription("Instances booted up by the Cloud Controller");
        // Payload definition
        List<Attribute> payloadData = new ArrayList<Attribute>();
        payloadData.add(new Attribute(CloudControllerConstants.TIME_STAMP, AttributeType.LONG));
        payloadData.add(new Attribute(CloudControllerConstants.MEMBER_ID_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.CARTRIDGE_TYPE_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.CLUSTER_ID_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.CLUSTER_INSTANCE_ID_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.LB_CLUSTER_ID_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.PARTITION_ID_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.NETWORK_ID_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.INSTANCE_TYPE, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.SCALING_REASON, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.SCALING_TIME, AttributeType.LONG));
        payloadData.add(new Attribute(CloudControllerConstants.IS_MULTI_TENANT_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.IAAS_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.STATUS_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.HOST_NAME_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.HYPERVISOR_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.RAM_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.IMAGE_ID_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.LOGIN_PORT_COL, AttributeType.INT));
        payloadData.add(new Attribute(CloudControllerConstants.OS_NAME_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.OS_VERSION_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.OS_ARCH_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.OS_BIT_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.PRIV_IP_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.PUB_IP_COL, AttributeType.STRING));
        payloadData.add(new Attribute(CloudControllerConstants.ALLOCATE_IP_COL, AttributeType.STRING));
        streamDefinition.setPayloadData(payloadData);
        return streamDefinition;
    }


    private static void createDataPublisher() {
        //creating the agent

        ServerConfiguration serverConfig = CarbonUtils.getServerConfiguration();
        String trustStorePath = serverConfig.getFirstProperty("Security.TrustStore.Location");
        String trustStorePassword = serverConfig.getFirstProperty("Security.TrustStore.Password");
        String bamServerUrl = serverConfig.getFirstProperty("BamServerURL");
        String adminUsername = CloudControllerConfig.getInstance().getDataPubConfig().getBamUsername();
        String adminPassword = CloudControllerConfig.getInstance().getDataPubConfig().getBamPassword();

        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);


        try {
            dataPublisher = new AsyncDataPublisher("tcp://" + bamServerUrl + "", adminUsername, adminPassword);
            CloudControllerContext.getInstance().setDataPublisher(dataPublisher);
            initializeStream();
            dataPublisher.addStreamDefinition(streamDefinition);
        } catch (Exception e) {
            String msg = "Unable to create a data publisher to " + bamServerUrl +
                    ". Usage Agent will not function properly. ";
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }
    }

    private static String handleNull(String val) {
        if (val == null) {
            return "";
        }
        return val;
    }

    private static Long handleNull(Long val) {
        if (val == null) {
            return -1L;
        }
        return val;
    }
}
