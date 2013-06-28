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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.throttling.manager.exception.ThrottlingException;
import org.wso2.carbon.throttling.manager.tasks.Task;

public class ThrottlingConfiguration {
    private static final Log log = LogFactory.getLog(ThrottlingConfiguration.class);
    private static final String CONFIG_NS = "http://wso2.com/carbon/multitenancy/usage-throttling-agent/config";
    List<ThrottlingTaskConfiguration> throttlingTaskConfigs;
    List<Task> tasks;

    public ThrottlingConfiguration(String throttlingConfigFile) throws ThrottlingException {
        try {
            OMElement throttlingConfig =
                    CommonUtil.buildOMElement(new FileInputStream(throttlingConfigFile));
            deserialize(throttlingConfig);
        } catch (FileNotFoundException e) {
            String msg = "Unable to find the file: " + throttlingConfigFile + ".";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        } catch (Exception e) {
            String msg = "Error in building the throttling config, config file: " +
                            throttlingConfigFile + ".";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        }
    }

    public void deserialize(OMElement throttlingConfigEle) throws ThrottlingException {
        OMElement throttlingManagerConfigs=null;
        Iterator childElements = throttlingConfigEle.getChildElements();
        while (childElements.hasNext()) {
            Object configChildElement = childElements.next();

            if (!(configChildElement instanceof OMElement)) {
                continue;
            }
            OMElement configChildOMElement = (OMElement) configChildElement;
            if (new QName(CONFIG_NS, "ThrottlingManagerTask", "").equals(configChildOMElement.getQName())) {
               throttlingManagerConfigs=(OMElement)configChildElement;
            }
        }
       // Iterator throttlingConfigChildIt = throttlingConfigEle.getChildElements();
        Iterator throttlingConfigChildIt = throttlingManagerConfigs.getChildElements();
        while (throttlingConfigChildIt.hasNext()) {
            Object throttlingConfigChild = throttlingConfigChildIt.next();
            if (!(throttlingConfigChild instanceof OMElement)) {
                continue;
            }
            OMElement throttlingConfigChildEle = (OMElement) throttlingConfigChild;

            if (new QName(CONFIG_NS, "tasks", "").equals(throttlingConfigChildEle.getQName())) {
                throttlingTaskConfigs = new ArrayList<ThrottlingTaskConfiguration>();
                tasks = new ArrayList<Task>();
                Iterator tasksConfigChildIt = throttlingConfigChildEle.getChildElements();
                while (tasksConfigChildIt.hasNext()) {
                    Object taskConfigChild = tasksConfigChildIt.next();
                    if (!(taskConfigChild instanceof OMElement)) {
                        continue;
                    }
                    ThrottlingTaskConfiguration taskConfiguration =
                            new ThrottlingTaskConfiguration((OMElement) taskConfigChild);
                    throttlingTaskConfigs.add(taskConfiguration);
                    tasks.add(taskConfiguration.getTask());
                }
            }
        }
    }

	public List<ThrottlingTaskConfiguration> getThrottlingTaskConfigs() {
		return throttlingTaskConfigs;
	}

	public List<Task> getThrottlingTasks() {
		return tasks;
	}
}
