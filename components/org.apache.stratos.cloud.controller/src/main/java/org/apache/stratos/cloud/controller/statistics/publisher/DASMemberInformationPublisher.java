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
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.InstanceMetadata;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.common.statistics.publisher.ThriftStatisticsPublisher;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * MemberInfoPublisher to publish member information/metadata to DAS.
 */
public class DASMemberInformationPublisher extends ThriftStatisticsPublisher implements MemberInformationPublisher {

    private static final Log log = LogFactory.getLog(DASMemberInformationPublisher.class);
    private static volatile DASMemberInformationPublisher dasMemberInformationPublisher;
    private static final String DATA_STREAM_NAME = "member_info";
    private static final String VERSION = "1.0.0";
    private static final String DAS_THRIFT_CLIENT_NAME = "das";
    private static final int STATS_PUBLISHER_THREAD_POOL_SIZE = 10;
    private static final String VALUE_NOT_FOUND = "Value Not Found";
    private ExecutorService executorService;

    private DASMemberInformationPublisher() {
        super(createStreamDefinition(), DAS_THRIFT_CLIENT_NAME);
        executorService = StratosThreadPool.getExecutorService(CloudControllerConstants.STATS_PUBLISHER_THREAD_POOL_ID, STATS_PUBLISHER_THREAD_POOL_SIZE);
    }

    public static DASMemberInformationPublisher getInstance() {
        if (dasMemberInformationPublisher == null) {
            synchronized (DASMemberInformationPublisher.class) {
                if (dasMemberInformationPublisher == null) {
                    dasMemberInformationPublisher = new DASMemberInformationPublisher();
                }
            }
        }
        return dasMemberInformationPublisher;
    }

    private static StreamDefinition createStreamDefinition() {
        try {
            // Create stream definition
            StreamDefinition streamDefinition = new StreamDefinition(DATA_STREAM_NAME, VERSION);
            streamDefinition.setNickName("Member Information");
            streamDefinition.setDescription("Member Information");
            List<Attribute> payloadData = new ArrayList<Attribute>();

            // Set payload definition
            payloadData.add(new Attribute(CloudControllerConstants.MEMBER_ID_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.INSTANCE_TYPE_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.SCALING_DECISION_ID_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.IS_MULTI_TENANT_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.PRIV_IP_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.PUB_IP_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.ALLOCATED_IP_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.HOST_NAME_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.HYPERVISOR_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.CPU_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.RAM_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.IMAGE_ID_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.LOGIN_PORT_COL, AttributeType.INT));
            payloadData.add(new Attribute(CloudControllerConstants.OS_NAME_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.OS_VERSION_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.OS_ARCH_COL, AttributeType.STRING));
            payloadData.add(new Attribute(CloudControllerConstants.OS_BIT_COL, AttributeType.BOOL));
            streamDefinition.setPayloadData(payloadData);
            return streamDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Could not create stream definition", e);
        }
    }

    /**
     * Publishing member info to DAS.
     *
     * @param memberId          Member Id
     * @param scalingDecisionId Scaling Decision Id
     * @param metadata          InstanceMetadata
     */
    @Override
    public void publish(final String memberId, final String scalingDecisionId, final InstanceMetadata metadata) {

        Runnable publisher = new Runnable() {
            @Override
            public void run() {

                if (metadata == null) {
                    log.warn("Couldn't publish member information as instance metadata is null");
                    return;
                } else {
                    MemberContext memberContext = CloudControllerContext.getInstance().getMemberContextOfMemberId(memberId);
                    String cartridgeType = memberContext.getCartridgeType();
                    Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
                    IaasProvider iaasProvider = CloudControllerContext.getInstance().getIaasProviderOfPartition(
                            cartridge.getType(), memberContext.getPartition().getId());
                    String instanceType = iaasProvider.getProperty(CloudControllerConstants.INSTANCE_TYPE);

                    //adding payload data
                    List<Object> payload = new ArrayList<Object>();
                    payload.add(memberId);
                    payload.add(handleNull(instanceType));
                    payload.add(scalingDecisionId);
                    payload.add(String.valueOf(cartridge.isMultiTenant()));
                    payload.add(handleNull(Arrays.toString(memberContext.getPrivateIPs())));
                    payload.add(handleNull(Arrays.toString(memberContext.getPublicIPs())));
                    payload.add(handleNull(Arrays.toString(memberContext.getAllocatedIPs())));
                    payload.add(handleNull(metadata.getHostname()));
                    payload.add(handleNull(metadata.getHypervisor()));
                    payload.add(handleNull(metadata.getCpu()));
                    payload.add(handleNull(metadata.getRam()));
                    payload.add(handleNull(metadata.getImageId()));
                    payload.add(metadata.getLoginPort());
                    payload.add(handleNull(metadata.getOperatingSystemName()));
                    payload.add(handleNull(metadata.getOperatingSystemVersion()));
                    payload.add(handleNull(metadata.getOperatingSystemArchitecture()));
                    payload.add(Boolean.valueOf(metadata.isOperatingSystem64bit()));
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Publishing member information: [member_id] %s [instance_type] %s " +
                                        "[scaling_decison_id] %s [is_multi_tenant] %s [private_IPs] %s " +
                                        "[public_IPs] %s [allocated_IPs] %s [host_name] %s [hypervisor] %s [cpu] %s " +
                                        "[ram] %s [image_id] %s [login_port] %d [os_name] %s " +
                                        "[os_version] %s [os_arch] %s [is_os_64bit] %b",
                                memberId, instanceType, scalingDecisionId, String.valueOf(cartridge.isMultiTenant()),
                                memberContext.getPrivateIPs(), memberContext.getPublicIPs(),
                                memberContext.getAllocatedIPs(), metadata.getHostname(), metadata.getHypervisor(),
                                metadata.getCpu(), metadata.getRam(), metadata.getImageId(), metadata.getLoginPort(),
                                metadata.getOperatingSystemName(), metadata.getOperatingSystemVersion(),
                                metadata.getOperatingSystemArchitecture(), metadata.isOperatingSystem64bit()));
                    }
                    DASMemberInformationPublisher.super.publish(payload.toArray());
                }
            }
        };
        executorService.execute(publisher);
    }

    public static String handleNull(String param) {
        if (null != param) {
            return param;
        }
        return VALUE_NOT_FOUND;
    }
}
