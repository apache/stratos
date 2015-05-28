/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.stratos.kubernetes.client.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Arrays;

/**
 * https://github.com/GoogleCloudPlatform/kubernetes/blob/master/api/doc/service-schema.json
 */
@XmlRootElement
public class Service implements Serializable {

    private static final long serialVersionUID = 7766915353839414993L;

    private String kind;
    private String id;
    private String creationTimestamp;
    private String selfLink;
    private String name;
    private int port;
    private String containerPort;
    private Selector selector;
    private String apiVersion;
    private Labels labels;
    private String[] publicIPs;
    private String portalIP;
    private String sessionAffinity;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Labels getLabels() {
        return labels;
    }

    public void setLabels(Labels labels) {
        this.labels = labels;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getContainerPort() {
        return containerPort;
    }

    public void setContainerPort(String containerPort) {
        this.containerPort = containerPort;
    }

    public Selector getSelector() {
        return selector;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public String[] getPublicIPs() {
        return publicIPs;
    }

    public void setPublicIPs(String[] publicIPs) {
        this.publicIPs = publicIPs;
    }

    public String getPortalIP() {
        return portalIP;
    }

    public void setPortalIP(String portalIP) {
        this.portalIP = portalIP;
    }

    public void setSessionAffinity(String sessionAffinity) {
        this.sessionAffinity = sessionAffinity;
    }

    public String getSessionAffinity() {
        return sessionAffinity;
    }

    @Override
    public String toString() {
        return "Service [kind=" + kind + ", id=" + id + ", creationTimestamp=" + creationTimestamp + ", selfLink="
                + selfLink + ", name=" + name + ", port=" + port + ", containerPort=" + containerPort + ", selector="
                + selector + ", apiVersion=" + apiVersion + ", labels=" + labels + ", publicIPs="
                + Arrays.toString(publicIPs) + "portalIP=" + portalIP + ", sessionAffinity=" + sessionAffinity + "]";
    }
}
