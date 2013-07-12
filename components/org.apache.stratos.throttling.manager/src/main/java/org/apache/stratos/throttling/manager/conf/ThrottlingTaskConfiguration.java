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
package org.apache.stratos.throttling.manager.conf;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.throttling.manager.exception.ThrottlingException;
import org.apache.stratos.throttling.manager.tasks.Task;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ThrottlingTaskConfiguration {
	private static final Log log =
	        LogFactory.getLog(ThrottlingTaskConfiguration.class);

	private static final String CONFIG_NS =
	        "http://wso2.com/carbon/multitenancy/usage-throttling-agent/config";
	private static final String TASK_CONF_PARAMERTERS = "parameters";
	private static final String TASK_CONF_DATA_PROVIDERS = "dataProviders";
	private static final String TASK_CONF_PARAM_KEY = "parameter";
	private static final String TASK_CONF_PARAM_NAME_KEY = "name";

	public static final String INTERVAL_PARAM_KEY = "interval";
	public static final String DELAY_PARAM_KEY = "delay";

	Task task;
	List<ThrottlingTaskDataProviderConfiguration> dataProviderConfigs;
	Map<String, String> taskParameters;

    public ThrottlingTaskConfiguration(OMElement taskConfigEle) throws ThrottlingException {
        dataProviderConfigs = new ArrayList<ThrottlingTaskDataProviderConfiguration>();
        serialize(taskConfigEle);
    }

    private void serialize(OMElement taskConfigEle) throws ThrottlingException {
        Iterator taskConfigChildIt = taskConfigEle.getChildElements();

        while (taskConfigChildIt.hasNext()) {
            Object taskConfigChildObj = taskConfigChildIt.next();
            if (!(taskConfigChildObj instanceof OMElement)) {
                continue;
            }
            OMElement taskConfigChildEle = (OMElement) taskConfigChildObj;
            if (taskConfigChildEle.getQName().equals(new QName(CONFIG_NS, TASK_CONF_PARAMERTERS))) {
                Iterator parametersIt = taskConfigChildEle.getChildElements();

                taskParameters = extractTaskParameters(parametersIt);
            } else if (taskConfigChildEle.getQName().equals(
                    new QName(CONFIG_NS, TASK_CONF_DATA_PROVIDERS))) {
                Iterator handlerConfigIt = taskConfigChildEle.getChildElements();
                while (handlerConfigIt.hasNext()) {
                    Object handlerConfigObj = handlerConfigIt.next();
                    if (!(handlerConfigObj instanceof OMElement)) {
                        continue;
                    }
                    OMElement handlerConfigEle = (OMElement) handlerConfigObj;
                    ThrottlingTaskDataProviderConfiguration handlerConfig =
                            new ThrottlingTaskDataProviderConfiguration(handlerConfigEle);
                    dataProviderConfigs.add(handlerConfig);
                }
            }
        }

        // create the task instance
        task = new Task(taskParameters, dataProviderConfigs);

    }

	private static Map<String, String> extractTaskParameters(
	        Iterator parameterIt) throws ThrottlingException {
		Map<String, String> parameters = new HashMap<String, String>();
		while (parameterIt.hasNext()) {
			Object parameterObj = parameterIt.next();
			if (!(parameterObj instanceof OMElement)) {
				continue;
			}
			OMElement configChildEle = (OMElement) parameterObj;
			if (!new QName(CONFIG_NS, TASK_CONF_PARAM_KEY, "")
			        .equals(configChildEle.getQName())) {
				continue;
			}
			String paramName =
			        configChildEle.getAttributeValue(new QName(
			                TASK_CONF_PARAM_NAME_KEY));
			String paramValue = configChildEle.getText();
			parameters.put(paramName, paramValue);
		}
		return parameters;
	}

	public List<ThrottlingTaskDataProviderConfiguration> getDataProviderConfigs() {
		return dataProviderConfigs;
	}

	public Task getTask() {
		return task;
	}
}
