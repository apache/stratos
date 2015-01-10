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

package org.apache.stratos.common.beans.cartridge;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement (name = "iaasProvider")
public class IaasProviderBean {

    private String type;
    private String name;
    private String className;
    private String imageId;
    private String provider;
    private String identity;
    private String credential;
    private List<PropertyBean> property;
    private List<NetworkInterfaceBean> networkInterfaces;

    public List<NetworkInterfaceBean> getNetworkInterfaces() {
        return networkInterfaces;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public List<PropertyBean> getProperty() {
        return property;
    }

    public void setProperty(List<PropertyBean> property) {
        this.property = property;
    }

    public void setNetworkInterfaces(List<NetworkInterfaceBean> networkInterfaces) {
        this.networkInterfaces = networkInterfaces;
    }

    private String getIaasPropertiesString () {
        StringBuilder iaasPropertyBuilder = new StringBuilder();
        if(getProperty() != null) {
            for (PropertyBean propertyBean : getProperty()) {
                iaasPropertyBuilder.append(propertyBean.getName() + " : " + propertyBean.getValue() + " | ");
            }
        }
        return iaasPropertyBuilder.toString();
    }

    private String getNetworkInterfacesString() {
        StringBuilder sb = new StringBuilder();
        if (getNetworkInterfaces() != null) {
            sb.append('[');
            String delimeter = "";
            for (NetworkInterfaceBean nib: getNetworkInterfaces()) {
                sb.append(delimeter).append(nib);
                delimeter = ", ";
            }
            sb.append(']');
        }
        return sb.toString();
    }

    public String toString () {
        return " [ Type: " + getType() + ", Name: " + getName() + ", Class Name: " + getClassName() + ", Image Id: " + getImageId() +
                ", Provider: " + getProvider() + ", Identity: " + getIdentity() +
                ", Credentials: " + getCredential() + ", Properties: " + getIaasPropertiesString() + ", Network Interfaces: " +
                getNetworkInterfacesString() + " ] ";
    }
}
