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

package org.apache.stratos.cloud.controller.pojo;

import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;

import java.io.Serializable;

/**
 * This class is used to support <link>CartridgeConfig</link>
 * class for the Rest API
 */
public class LoadbalancerConfig implements Serializable {

    private static final long serialVersionUID = 289225330995632449L;

    private String type;
   
    private Properties properties;
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String toString () {

        return " [ Type: " + type + ", Properties: " + getIaasProperties() + " ] ";
    }

    private String getIaasProperties () {

        StringBuilder iaasPropertyBuilder = new StringBuilder();
        if (properties != null) {
            Property[] propertyArray = properties.getProperties();
            if(propertyArray.length > 0) {
                for (Property property : propertyArray) {
                    iaasPropertyBuilder.append(property.toString() + " | ");
                }
            }
        }
        return iaasPropertyBuilder.toString();
    }
}
