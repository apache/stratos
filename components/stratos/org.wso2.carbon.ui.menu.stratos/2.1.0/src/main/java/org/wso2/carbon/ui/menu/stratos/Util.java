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

package org.wso2.carbon.ui.menu.stratos;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.carbon.stratos.common.config.CloudServiceConfig;
import org.wso2.carbon.stratos.common.config.CloudServiceConfigParser;
import org.wso2.carbon.stratos.common.exception.StratosException;

import java.util.Map;

/**
 * Implements the Utility methods for Stratos UI General Menu component such as extracting the
 * Manager Home page URL from the config files.
 */
public class Util {

    private static Log log = LogFactory.getLog(Util.class);

    // use "https://stratoslive.wso2.com" as the default value.
    private static String managerHomepageURL = "https://stratoslive.wso2.com";

    // constants used to parse the cloud-services-desc.xml    
    private static final String STRATOS_MANAGER = "WSO2 Stratos Manager";

    /**
     * Extracts the Manager homepage URL from the CloudServiceConfiguration which is parsed via cloud-services-desc.xml.
     * @throws StratosException Failing to obtain the Manager home page URL via CloudServiceConfiguration. 
     */
    public static void readManagerURLFromConfig() throws StratosException {
        try {
            Map<String, CloudServiceConfig> cloudServiceConfigs = CloudServiceConfigParser.
                        loadCloudServicesConfiguration().getCloudServiceConfigs();
            managerHomepageURL = cloudServiceConfigs.get(STRATOS_MANAGER).getLink() +
                    "/carbon/sso-acs/redirect_ajaxprocessor.jsp";

        } catch (Exception e) {
            String errorMsg = "Error while reading the Manager Homepage URL from " +
                              "cloud service configuration.";
            log.error(errorMsg, e);
            throw new StratosException(errorMsg, e);
        }
    }

    /**
     * Get the Manager Homepage URL.
     * @return returns the value specified in SAML2SSOConfiguration.ManagerHomepage, if not returns
     * the default value, "https://stratoslive.wso2.com".
     */
    public static String getManagerHomepageURL(){
        return managerHomepageURL;
    }

    /**
     * Read the element value for the given element
     * @param element   Parent element
     * @param tagName   name of the child element
     * @return value of the element
     */
    private static String getTextValue(Element element, String tagName) {
		String textVal = null;
		NodeList nl = element.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}
		return textVal;
	}
}
