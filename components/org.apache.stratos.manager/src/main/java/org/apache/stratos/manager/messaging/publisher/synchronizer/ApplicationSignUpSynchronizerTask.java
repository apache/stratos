/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.manager.messaging.publisher.synchronizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.components.ApplicationSignUpHandler;
import org.apache.stratos.manager.messaging.publisher.ApplicationSignUpEventPublisher;
import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
import org.wso2.carbon.ntask.core.Task;

import java.util.List;
import java.util.Map;

/**
 * Application signup synchronizer task.
 */
public class ApplicationSignUpSynchronizerTask implements Task {

    private static final Log log = LogFactory.getLog(ApplicationSignUpSynchronizerTask.class);

    private ApplicationSignUpHandler applicationSignUpHandler;

    @Override
    public void setProperties(Map<String, String> map) {
    }

    @Override
    public void init() {
        applicationSignUpHandler = new ApplicationSignUpHandler();
    }

    @Override
    public void execute() {
        try {
            List<ApplicationSignUp> applicationSignUps = applicationSignUpHandler.getApplicationSignUps();
            if((applicationSignUps != null) && (applicationSignUps.size() > 0)) {
                ApplicationSignUpEventPublisher.publishCompleteApplicationSignUpsEvent(applicationSignUps);
            }
        } catch (Exception e) {
            String message = "Could not publish complete application signup event";
            log.error(message, e);
        }
    }
}
