/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.common.config;

import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;
import java.util.Iterator;

public class PermissionConfig {
    private static final String CONFIG_NS = "http://wso2.com/carbon/cloud/mgt/services";
    private static final String PATH = "path";
    private static final String NAME = "name";
    String name;
    String path;

    public PermissionConfig(OMElement configEle) {
        serialize(configEle);
    }

    public void serialize(OMElement configEle) {

        Iterator configChildIt = configEle.getChildElements();
        while (configChildIt.hasNext()) {
            Object configChildObj = configChildIt.next();
            if (!( configChildObj instanceof OMElement)) {
                continue;
            }
            OMElement configChildEle = (OMElement)configChildObj;
            if (new QName(CONFIG_NS, NAME, "").
                    equals(configChildEle.getQName())) {
                name = configChildEle.getText();
            } else if (new QName(CONFIG_NS, PATH, "").
                    equals(configChildEle.getQName())) {
                path = configChildEle.getText();
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
