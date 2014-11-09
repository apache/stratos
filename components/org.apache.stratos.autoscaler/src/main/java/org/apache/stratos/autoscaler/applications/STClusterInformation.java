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

package org.apache.stratos.autoscaler.applications;

public class STClusterInformation implements ClusterInformation {

    @Override
    public String getClusterId(String alias, String cartridgeType) {

        if (!ApplicationUtils.isValid(alias)) {
            // cannot happen
            throw new IllegalArgumentException("Invalid alias value provided: [ " + alias + " ]");
        }

        if (!ApplicationUtils.isValid(cartridgeType)) {
            // cannot happen
            throw new IllegalArgumentException("Invalid cartridge type value provided: [ " + cartridgeType + " ]");
        }

        return alias + "." + cartridgeType + ".domain";
    }

    @Override
    public String getHostName(String alias, String cartridgeDefinitionHostName) {

        if (!ApplicationUtils.isValid(alias)) {
            // cannot happen
            throw new IllegalArgumentException("Invalid alias value provided: [ " + alias + " ]");
        }

        if (!ApplicationUtils.isValid(cartridgeDefinitionHostName)) {
            // cannot happen
            throw new IllegalArgumentException("Invalid host name value provided: [ " + cartridgeDefinitionHostName + " ]");
        }

        return alias + "." + cartridgeDefinitionHostName;
    }
}
