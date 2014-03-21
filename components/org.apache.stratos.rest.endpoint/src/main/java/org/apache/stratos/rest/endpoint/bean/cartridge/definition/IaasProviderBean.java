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

package org.apache.stratos.rest.endpoint.bean.cartridge.definition;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement (name = "iaasProvider")
public class IaasProviderBean {

    public String type;

    public String name;

    public String className;

    public String imageId;

    public int maxInstanceLimit;

    public String provider;

    public String identity;

    public String credential;

    public List<PropertyBean> property;
    
    public List<NetworkInterfaceBean> networkInterfaces;

    public String toString () {
        return " [ Type: " + type + ", Name: " + name + ", Class Name: " + className + ", Image Id: " + imageId +
                ", Max Instance Limit: " + maxInstanceLimit + ", Provider: " + provider + ", Identity: " + identity +
                ", Credentials: " + credential + ", Properties: " + getIaasProperties() + ", Network Interfaces: " +
                getNetworkInterfaces() + " ] ";
    }

    private String getIaasProperties () {

        StringBuilder iaasPropertyBuilder = new StringBuilder();
        if(property != null) {
            for (PropertyBean propertyBean : property) {
                iaasPropertyBuilder.append(propertyBean.name + " : " + propertyBean.value + " | ");
            }
        }
        return iaasPropertyBuilder.toString();
    }
    
    private String getNetworkInterfaces() {
        StringBuilder sb = new StringBuilder();
        if (networkInterfaces != null) {
            sb.append('[');
            String delimeter = "";
            for (NetworkInterfaceBean nib:networkInterfaces) {
                sb.append(delimeter).append(nib);
                delimeter = ", ";
            }
            sb.append(']');
        }
        return sb.toString();
    }
}
