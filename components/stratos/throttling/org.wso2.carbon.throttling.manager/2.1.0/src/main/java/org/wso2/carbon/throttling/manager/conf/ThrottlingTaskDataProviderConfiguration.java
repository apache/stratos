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
package org.wso2.carbon.throttling.manager.conf;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.throttling.manager.dataproviders.DataProvider;
import org.wso2.carbon.throttling.manager.exception.ThrottlingException;
import org.wso2.carbon.throttling.manager.utils.Util;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ThrottlingTaskDataProviderConfiguration {
	private static final Log log =
	        LogFactory.getLog(ThrottlingTaskDataProviderConfiguration.class);

	private static final String CONFIG_NS =
	        "http://wso2.com/carbon/multitenancy/throttling/config";
	private static final String HANDLER_CONF_PARAM_KEY = "parameter";
	private static final String HANDLER_CLASS_ATTR = "class";
	private static final String HANDLER_CONF_PARAM_NAME_KEY = "name";
	private static final String HANDLER_SERVICE_ATTR = "service";

	private String dataProviderServiceName;
	private Map<String, String> dataProviderParameters;
	private DataProvider dataProvider;
	// to keep the task class that are available.
	private Map<String, DataProvider> dataProviders;

	public ThrottlingTaskDataProviderConfiguration(OMElement handlerConfigEle)
            throws ThrottlingException {
		serialize(handlerConfigEle);
		dataProviders = new HashMap<String, DataProvider>();
	}

    private void serialize(OMElement handlerConfigEle) throws ThrottlingException {
        Iterator handlerParameterChildIt = handlerConfigEle.getChildElements();
        Map<String, String> parameters = extractParameters(handlerParameterChildIt);
        // get the task class
        String handlerClassName = handlerConfigEle.getAttributeValue(new QName(HANDLER_CLASS_ATTR));

        if (handlerClassName == null) {
            dataProviderServiceName =
                    handlerConfigEle.getAttributeValue(new QName(HANDLER_SERVICE_ATTR));
            dataProviderParameters = parameters;
        } else {
            dataProvider = (DataProvider) Util.constructObject(handlerClassName);
            dataProvider.init(parameters);
        }
    }

    private static Map<String, String> extractParameters(Iterator parameterIt)
            throws ThrottlingException {
        Map<String, String> parameters = new HashMap<String, String>();
        while (parameterIt.hasNext()) {
            Object parameterObj = parameterIt.next();
            if (!(parameterObj instanceof OMElement)) {
                continue;
            }
            OMElement configChildEle = (OMElement) parameterObj;
            if (!new QName(CONFIG_NS, HANDLER_CONF_PARAM_KEY, "").equals(configChildEle.getQName())) {
                continue;
            }
            String paramName =
                    configChildEle.getAttributeValue(new QName(HANDLER_CONF_PARAM_NAME_KEY));
            String paramValue = configChildEle.getText();
            parameters.put(paramName, paramValue);
        }
        return parameters;
    }

	// get task have to be called to initialize tasks which are registered as
	// OSGI services
	public DataProvider getDataProvider() throws ThrottlingException {
		if (dataProvider == null && dataProviderServiceName != null) {
			dataProvider = dataProviders.get(dataProviderServiceName);
			if (dataProvider == null) {
				String msg =
				        "The scheduler helper service: " +
				                dataProviderServiceName + " is not loaded.";
				log.error(msg);
				throw new ThrottlingException(msg);
			}
			dataProvider.init(dataProviderParameters);
		}
		return dataProvider;
	}

	public void loadDataProviderService() throws ThrottlingException {
		if (dataProvider == null && dataProviderServiceName != null) {
			dataProvider = dataProviders.get(dataProviderServiceName);
			if (dataProvider != null) {
				dataProvider.init(dataProviderParameters);
			}
		}
	}
}
