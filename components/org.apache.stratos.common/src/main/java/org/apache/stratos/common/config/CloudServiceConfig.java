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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CloudServiceConfig {
    private static final String CONFIG_NS = "http://wso2.com/carbon/cloud/mgt/services";

    private static final String NAME_ATTR_NAME = "name";
    private static final String DEFAULT_ATTR_NAME = "default";
    private static final String LABEL_ELEMENT_NAME = "label";
    private static final String LINK_ELEMENT_NAME = "link";
    private static final String ICON_ELEMENT_NAME = "icon";
    private static final String DESCRIPTION_ELEMENT_NAME = "description";
    private static final String PERMISSIONS_ELEMENT_NAME = "permissions";
    private static final String PERMISSION_ELEMENT_NAME = "permission";
    private static final String PRODUCT_PAGE_URL_ELEMENT_NAME = "productPageURL";

    private String name;
    private String label;
    private String link;
    private String icon;
    private String description;
    private List<PermissionConfig> permissionConfigs;
    boolean defaultActive;
    private String productPageURL;

    public CloudServiceConfig(OMElement configEle) {
        permissionConfigs = new ArrayList<PermissionConfig>();
        serialize(configEle);
    }

    public void serialize(OMElement configEle) {
        Iterator cloudServiceChildIt = configEle.getChildElements();
        name = configEle.getAttributeValue(new QName(null, NAME_ATTR_NAME));
        defaultActive = "true".equals(configEle.
                getAttributeValue(new QName(null, DEFAULT_ATTR_NAME)));
        while (cloudServiceChildIt.hasNext()) {
            Object cloudServiceChildObj = cloudServiceChildIt.next();
            if (!(cloudServiceChildObj instanceof OMElement)) {
                continue;
            }
            OMElement cloudServiceChildEle = (OMElement) cloudServiceChildObj;
            if (new QName(CONFIG_NS, LABEL_ELEMENT_NAME, "").
                    equals(cloudServiceChildEle.getQName())) {
                label = cloudServiceChildEle.getText();
            } else if (new QName(CONFIG_NS, ICON_ELEMENT_NAME, "").
                    equals(cloudServiceChildEle.getQName())) {
                icon = cloudServiceChildEle.getText();
            } else if (new QName(CONFIG_NS, LINK_ELEMENT_NAME, "").
                    equals(cloudServiceChildEle.getQName())) {
                link = cloudServiceChildEle.getText();
            } else if (new QName(CONFIG_NS, PRODUCT_PAGE_URL_ELEMENT_NAME, "").
                    equals(cloudServiceChildEle.getQName())) {
                productPageURL = cloudServiceChildEle.getText();
            } else if (new QName(CONFIG_NS, DESCRIPTION_ELEMENT_NAME, "").
                    equals(cloudServiceChildEle.getQName())) {
                description = cloudServiceChildEle.getText();
            } else if (new QName(CONFIG_NS, PERMISSIONS_ELEMENT_NAME, "").
                    equals(cloudServiceChildEle.getQName())) {
                Iterator permissionChildIt = cloudServiceChildEle.getChildElements();
                while (permissionChildIt.hasNext()) {
                    Object permissionChildObj = permissionChildIt.next();
                    if (!(permissionChildObj instanceof OMElement)) {
                        continue;
                    }
                    OMElement permissionChildEle = (OMElement) permissionChildObj;

                    if (new QName(CONFIG_NS, PERMISSION_ELEMENT_NAME, "").
                            equals(permissionChildEle.getQName())) {
                        PermissionConfig permissionConfig =
                                new PermissionConfig(permissionChildEle);
                        permissionConfigs.add(permissionConfig);
                    }
                }
            }

        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PermissionConfig> getPermissionConfigs() {
        return permissionConfigs;
    }

    public void setPermissionConfigs(List<PermissionConfig> permissionConfigs) {
        this.permissionConfigs = permissionConfigs;
    }

    public boolean isDefaultActive() {
        return defaultActive;
    }

    public void setDefaultActive(boolean defaultActive) {
        this.defaultActive = defaultActive;
    }

    public String getProductPageURL() {
        return productPageURL;
    }

    public void setProductPageURL(String productPageURL) {
        this.productPageURL = productPageURL;
    }
}
