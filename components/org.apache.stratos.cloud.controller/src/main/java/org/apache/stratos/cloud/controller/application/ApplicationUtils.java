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

package org.apache.stratos.cloud.controller.application;

import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.PortMapping;
import org.apache.stratos.cloud.controller.pojo.application.SubscribableContext;
import org.apache.stratos.cloud.controller.pojo.application.SubscribableInfoContext;
import org.apache.stratos.cloud.controller.pojo.payload.MetaDataHolder;
import org.apache.stratos.metadata.client.config.MetaDataClientConfig;

import java.util.*;
import java.util.regex.Pattern;

public class ApplicationUtils {

    public static boolean isAliasValid (String alias) {

        String patternString = "([a-z0-9]+([-][a-z0-9])*)+";
        Pattern pattern = Pattern.compile(patternString);

        return pattern.matcher(alias).matches();
    }

    public static boolean isValid (String arg) {

        if (arg == null || arg.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    public static Properties getGlobalPayloadData () {

        Properties globalProperties = new Properties();

        if (System.getProperty("puppet.ip") != null) {
            globalProperties.setProperty("PUPPET_IP", System.getProperty("puppet.ip"));
        }
        if (System.getProperty("puppet.hostname") != null) {
            globalProperties.setProperty("PUPPET_HOSTNAME", System.getProperty("puppet.hostname"));
        }
        if (System.getProperty("puppet.env") != null) {
            globalProperties.setProperty("PUPPET_ENV", System.getProperty("puppet.env"));
        }
        if (System.getProperty("puppet.dns.available") != null) {
            globalProperties.setProperty("PUPPET_DNS_AVAILABLE", System.getProperty("puppet.dns.available"));
        }

        return globalProperties;
    }

    public static MetaDataHolder getClusterLevelPayloadData (String appId, String groupName, int tenantId, String key,
                                                                String hostname, String tenantRange, String clusterId,
                                                                SubscribableContext subscribableCtxt,
                                                                SubscribableInfoContext subscribableInfoCtxt,
                                                                Cartridge cartridge) {

        MetaDataHolder metaDataHolder;
        if (groupName != null) {
            metaDataHolder = new MetaDataHolder(appId, groupName, clusterId);
        } else {
            metaDataHolder = new MetaDataHolder(appId, clusterId);
        }

        Properties clusterLevelPayloadProperties = new Properties();
        // app id
        clusterLevelPayloadProperties.setProperty("APP_ID", appId);
        // group name
        if (groupName != null) {
            clusterLevelPayloadProperties.setProperty("GROUP_NAME", groupName);
        }
        // service name
        if (subscribableCtxt.getType() != null) {
            clusterLevelPayloadProperties.put("SERVICE_NAME", subscribableCtxt.getType());
        }
        // host name
        if  (hostname != null) {
            clusterLevelPayloadProperties.put("HOST_NAME", hostname);
        }
        // multi tenant
        clusterLevelPayloadProperties.put("MULTITENANT", String.valueOf(cartridge.isMultiTenant()));
        // tenant range
        if (tenantRange != null) {
            clusterLevelPayloadProperties.put("TENANT_RANGE", tenantRange);
        }
        // cartridge alias
        if (subscribableCtxt.getAlias() != null) {
            clusterLevelPayloadProperties.put("CARTRIDGE_ALIAS", subscribableCtxt.getAlias());
        }
        // cluster id
        if (clusterId != null) {
            clusterLevelPayloadProperties.put("CLUSTER_ID", clusterId);
        }
        // repo url
        if (subscribableInfoCtxt.getRepoUrl() != null) {
            clusterLevelPayloadProperties.put("REPO_URL", subscribableInfoCtxt.getRepoUrl());
        }
        // ports
        if (createPortMappingPayloadString(cartridge) != null) {
            clusterLevelPayloadProperties.put("PORTS", createPortMappingPayloadString(cartridge));
        }
        // provider
        if (cartridge.getProvider() != null) {
            clusterLevelPayloadProperties.put("PROVIDER", cartridge.getProvider());
        }
        // tenant id
        clusterLevelPayloadProperties.setProperty("TENANT_ID", String.valueOf(tenantId));
        // cartridge key
        clusterLevelPayloadProperties.setProperty("CARTRIDGE_KEY", key);
        // get global payload params
        clusterLevelPayloadProperties.putAll(ApplicationUtils.getGlobalPayloadData());

        metaDataHolder.setProperties(clusterLevelPayloadProperties);
        return metaDataHolder;
    }

    private static String createPortMappingPayloadString (Cartridge cartridge) {

        // port mappings
        StringBuilder portMapBuilder = new StringBuilder();
        List<PortMapping> portMappings = cartridge.getPortMappings();
        for (PortMapping portMapping : portMappings) {
            String port = portMapping.getPort();
            portMapBuilder.append(port).append("|");
        }

        // remove last "|" character

        return portMapBuilder.toString().replaceAll("\\|$", "");
    }

    public static StringBuilder getTextPayload (String appId, String groupName, String clusterId) {

        StringBuilder payloadBuilder = new StringBuilder();
        payloadBuilder.append("APP_ID=" + appId);
        if (groupName != null) {
            payloadBuilder.append(",");
            payloadBuilder.append("GROUP_NAME=" + groupName);
        }
        payloadBuilder.append(",");
        payloadBuilder.append("CLUSTER_ID=" + clusterId);
        // meta data endpoint
        if (MetaDataClientConfig.getInstance().getMetaDataServiceBaseUrl() != null) {
            // TODO
            //payloadBuilder.append(",");
            //payloadBuilder.append("METADATA_ENDPOINT=" + MetaDataClientConfig.getInstance().getMetaDataServiceBaseUrl());
        }
        payloadBuilder.append(",");

        return payloadBuilder;
    }
}
