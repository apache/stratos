/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.stratos.tenant.activity.ui.utils;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.util.CommonUtil;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class ManagerConfigurations {

    private static final String CONFIG_FILE = "throttling-agent-config.xml";

    private static final String MANAGER_SERVICE_URL_PARAM_NAME = "managerServiceUrl";
    private static final String USERNAME_PARAM_NAME = "userName";
    private static final String PASSWORD_PARAM_NAME = "password";


    private String managerServerUrl;
    private String userName;
    private String password;

    private final static Log log = LogFactory.getLog(ManagerConfigurations.class);

    private static final String CONFIG_NS =
            "http://wso2.com/stratos/multitenancy/throttling/agent/config";
    private static final String PARAMTERS_ELEMENT_NAME = "parameters";
    private static final String PARAMTER_ELEMENT_NAME = "parameter";
    private static final String PARAMTER_NAME_ATTR_NAME = "name";
    private Map<String, String> parameters = new HashMap<String, String>();




    public String getConfigFileName() throws Exception {

        String configFileName = CarbonUtils.getCarbonConfigDirPath() +
                File.separator + StratosConstants.MULTITENANCY_CONFIG_FOLDER +
                File.separator + CONFIG_FILE;

        return configFileName;

    }


    public ManagerConfigurations() throws Exception {

        String throttlingAgentConfigFile = this.getConfigFileName();
        try {
            OMElement meteringConfig =
                    CommonUtil.buildOMElement(new FileInputStream(throttlingAgentConfigFile));
            deSerialize(meteringConfig);
            Map<String, String> throttlingAgentParams = getParameters();
            this.setUserName(throttlingAgentParams.get(USERNAME_PARAM_NAME));
            this.setPassword(throttlingAgentParams.get(PASSWORD_PARAM_NAME));
            this.setManagerServerUrl(throttlingAgentParams.get(MANAGER_SERVICE_URL_PARAM_NAME));
        } catch (FileNotFoundException e) {
            String msg = "Unable to find the file: " + throttlingAgentConfigFile + ".";
            log.error(msg, e);
        }
    }

    public void deSerialize(OMElement throttlingConfigEle) throws Exception {
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


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getManagerServerUrl() {
        return managerServerUrl;
    }

    public void setManagerServerUrl(String managerServerUrl) {
        this.managerServerUrl = managerServerUrl;
    }
}