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

import java.io.Serializable;

/**
 * This class is used to support <link>CartridgeConfig</link>
 * class for the Rest API
 */
public class IaasConfig implements Serializable {

    private String type;

    private String imageId;

    private int maxInstanceLimit;

    private Properties properties;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public int getMaxInstanceLimit() {
        return maxInstanceLimit;
    }

    public void setMaxInstanceLimit(int maxInstanceLimit) {
        this.maxInstanceLimit = maxInstanceLimit;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String toString () {

        return " [ Type: " + type + ", Image Id: " + imageId + ", Max Instance Limit: " + maxInstanceLimit +
                " Properties: " + getIaasProperties() + " ] ";
    }

    private String getIaasProperties () {

        StringBuilder iaasPropertyBuilder = new StringBuilder();
        if (properties != null) {
            Property [] propertyArray = properties.getProperties();
            if(propertyArray.length > 0) {
                for (Property property : propertyArray) {
                    iaasPropertyBuilder.append(property.toString() + " | ");
                }
            }
        }
        return iaasPropertyBuilder.toString();
    }
}
