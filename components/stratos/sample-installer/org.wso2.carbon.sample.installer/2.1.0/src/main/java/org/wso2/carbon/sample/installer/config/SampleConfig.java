/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.sample.installer.config;

import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Contains a configuration for a sample.
 */
public class SampleConfig {
    private static final String CONFIG_NS = "http://wso2.com/stratos/samples";

    private static final String NAME_ATTRIBUTE_NAME = "name";
    private static final String FILE_NAME_ELEMENT_NAME = "fileName";
    private static final String CLOUD_SERVICES_ELEMENT_NAME = "cloudServices";
    private static final String CLOUD_SERVICE_ELEMENT_NAME = "cloudService";

    private String name;
    private String fileName;
    private List<String> cloudServices;

    /**
     * Constructs a sample configuration object.
     *
     * @param configEle the configuration element
     */
    public SampleConfig(OMElement configEle) {
        cloudServices = new ArrayList<String>();
        serialize(configEle);
    }

    private void serialize(OMElement configEle) {
        Iterator sampleChildIt = configEle.getChildElements();
        name = configEle.getAttributeValue(new QName(null, NAME_ATTRIBUTE_NAME));
        while (sampleChildIt.hasNext()) {
            Object sampleChildObj = sampleChildIt.next();
            if (!(sampleChildObj instanceof OMElement)) {
                continue;
            }
            OMElement sampleChildEle = (OMElement)sampleChildObj;
            if (new QName(CONFIG_NS, FILE_NAME_ELEMENT_NAME, "").
                    equals(sampleChildEle.getQName())) {
                fileName = sampleChildEle.getText();
            } else if (new QName(CONFIG_NS, CLOUD_SERVICES_ELEMENT_NAME, "").
                    equals(sampleChildEle.getQName())) {
                Iterator cloudServicesChildIt = sampleChildEle.getChildElements();
                while (cloudServicesChildIt.hasNext()) {
                    Object cloudServiceChildObj = cloudServicesChildIt.next();
                    if (!(cloudServiceChildObj instanceof OMElement)) {
                        continue;
                    }
                    OMElement cloudServiceChildEle = (OMElement)cloudServiceChildObj;

                    if (new QName(CONFIG_NS, CLOUD_SERVICE_ELEMENT_NAME, "").
                            equals(cloudServiceChildEle.getQName())) {
                        cloudServices.add(cloudServiceChildEle.getAttributeValue(
                                new QName(null, NAME_ATTRIBUTE_NAME)));
                    }
                }
            }
        }
    }

    /**
     * Method to obtain the file name of the sample.
     *
     * @return the file name of the sample.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Method to obtain the list of cloud services used by the sample.
     *
     * @return the list of cloud services used by the sample.
     */
    public String[] getCloudServices() {
        return cloudServices.toArray(new String[cloudServices.size()]);
    }

    /**
     * Method to obtain the name of the sample.
     *
     * @return the name of the sample.
     */
    public String getName() {
        return name;
    }
}
