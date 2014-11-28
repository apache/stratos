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

package org.apache.stratos.cloud.controller.iaas;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.exception.InvalidHostException;
import org.apache.stratos.cloud.controller.exception.InvalidRegionException;
import org.apache.stratos.cloud.controller.exception.InvalidZoneException;
import org.apache.stratos.cloud.controller.util.ComputeServiceBuilderUtil;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.iaas.validators.DockerPartitionValidator;
import org.apache.stratos.cloud.controller.iaas.validators.PartitionValidator;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;

import java.util.Set;

/**
 * Docker iaas provider definition.
 */
public class DockerIaas extends Iaas {

    private static final Log log = LogFactory.getLog(AWSEC2Iaas.class);

    public DockerIaas(IaasProvider iaasProvider) {
        super(iaasProvider);
    }

    @Override
    public void buildComputeServiceAndTemplate() {
        // builds and sets Compute Service
        ComputeServiceBuilderUtil.buildDefaultComputeService(getIaasProvider());

        // builds and sets Template
        buildTemplate();
    }

    @Override
    public void setDynamicPayload() {
        log.warn("Not implemented: DockerIaas.setDynamicPayload()");
    }

    @Override
    public String associateAddress(NodeMetadata node) {
        log.warn("Not implemented: DockerIaas.associateAddress()");
        return null;
    }

    @Override
    public String associatePredefinedAddress(NodeMetadata node, String ip) {
        log.warn("Not implemented: DockerIaas.associatePredefinedAddress()");
        return null;
    }

    @Override
    public void releaseAddress(String ip) {
        log.warn("Not implemented: DockerIaas.releaseAddress()");
    }

    @Override
    public boolean createKeyPairFromPublicKey(String region, String keyPairName, String publicKey) {
        return false;
    }

    @Override
    public boolean isValidRegion(String region) throws InvalidRegionException {
        return true;
    }

    @Override
    public boolean isValidZone(String region, String zone) throws InvalidZoneException {
        return true;
    }

    @Override
    public boolean isValidHost(String zone, String host) throws InvalidHostException {
        return true;
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return new DockerPartitionValidator();
    }

    @Override
    public void buildTemplate() {
        IaasProvider iaasProvider = getIaasProvider();
        ComputeService computeService = iaasProvider.getComputeService();
        Set<? extends Image> images = computeService.listImages();
        Image image = findImage(images, iaasProvider.getImage());
        if(image == null) {
            throw new CloudControllerException(String.format("Docker image not found: %s", iaasProvider.getImage()));
        }
        Template template = computeService.templateBuilder().fromImage(image).build();
        iaasProvider.setTemplate(template);
    }

    private Image findImage(Set<? extends Image> images, String name) {
        for(Image image : images) {
            if(image.getDescription().contains(name))
                return image;
        }
        return null;
    }

    @Override
    public String createVolume(int sizeGB, String snapshotId) {
        return null;
    }

    @Override
    public String attachVolume(String instanceId, String volumeId, String deviceName) {
        return null;
    }

    @Override
    public void detachVolume(String instanceId, String volumeId) {

    }

    @Override
    public void deleteVolume(String volumeId) {

    }

    @Override
    public String getIaasDevice(String device) {
        return null;
    }
}
