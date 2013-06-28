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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains the configuration for all samples
 */
public class SamplesDescConfig {
    private static final String CONFIG_NS = "http://wso2.com/stratos/samples";
    private static final String SAMPLE_ELEMENT_NAME = "sample";

    private List<SampleConfig> sampleConfigs;

    /**
     * Constructs a configuration object for all samples.
     *
     * @param configEle the configuration element
     */
    public SamplesDescConfig(OMElement configEle) {
        // as the cloud service configs are kept in an order, we use an ordered map.
        sampleConfigs = new LinkedList<SampleConfig>();
        serialize(configEle);
    }

    private void serialize(OMElement configEle) {
        Iterator configChildIt = configEle.getChildElements();
        while (configChildIt.hasNext()) {
            Object configChildObj = configChildIt.next();
            if (configChildObj instanceof OMElement) {
                OMElement configChildEle = (OMElement)configChildObj;
                if (new QName(CONFIG_NS, SAMPLE_ELEMENT_NAME, "").
                        equals(configChildEle.getQName())) {
                    sampleConfigs.add(new SampleConfig(configChildEle));
                }
            }
        }
    }

    /**
     * Method to obtain the configurations of each individual sample.
     *
     * @return the configurations of each individual sample.
     */
    public SampleConfig[] getSampleConfigs() {
        return sampleConfigs.toArray(new SampleConfig[sampleConfigs.size()]);
    }
}
