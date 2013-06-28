/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.throttling.agent.conf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.util.CommonUtil;

public class ThrottlingAgentConfiguration {
    private static final Log log = LogFactory.getLog(ThrottlingAgentConfiguration.class);
    private static final String CONFIG_NS =
            "http://wso2.com/carbon/multitenancy/throttling/agent/config";
    private static final String PARAMTERS_ELEMENT_NAME = "parameters";
    private static final String PARAMTER_ELEMENT_NAME = "parameter";
    private static final String PARAMTER_NAME_ATTR_NAME = "name";
    private Map<String, String> parameters = new HashMap<String, String>();

    public ThrottlingAgentConfiguration(String throttlingConfigFile) throws Exception {
        try {
            OMElement meteringConfig =
                    CommonUtil.buildOMElement(new FileInputStream(throttlingConfigFile));
            deserialize(meteringConfig);
        } catch (FileNotFoundException e) {
            String msg = "Unable to find the file: " + throttlingConfigFile + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    public void deserialize(OMElement throttlingConfigEle) throws Exception {
        Iterator meteringConfigChildIt = throttlingConfigEle.getChildElements();
        while (meteringConfigChildIt.hasNext()) {
            Object meteringConfigChild = meteringConfigChildIt.next();
            if (!(meteringConfigChild instanceof OMElement)) {
                continue;
            }
            OMElement meteringConfigChildEle = (OMElement) meteringConfigChild;
            if (new QName(CONFIG_NS, PARAMTERS_ELEMENT_NAME, "").equals(meteringConfigChildEle
                    .getQName())) {
                Iterator parametersChildIt = meteringConfigChildEle.getChildElements();
                while (parametersChildIt.hasNext()) {
                    Object taskConfigChild = parametersChildIt.next();
                    if (!(taskConfigChild instanceof OMElement)) {
                        continue;
                    }
                    OMElement parameterChildEle = (OMElement) taskConfigChild;
                    if (!new QName(CONFIG_NS, PARAMTER_ELEMENT_NAME, "").equals(parameterChildEle
                            .getQName())) {
                        continue;
                    }
                    String parameterName =
                            parameterChildEle.getAttributeValue(new QName(PARAMTER_NAME_ATTR_NAME));
                    String parameterValue = parameterChildEle.getText();
                    parameters.put(parameterName, parameterValue);
                }
            }
        }
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
}
