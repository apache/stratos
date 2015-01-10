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

package org.apache.stratos.load.balancer.messaging.receiver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.context.LoadBalancerContextUtil;
import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.messaging.domain.domain.mapping.DomainMapping;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.signup.CompleteApplicationSignUpsEvent;
import org.apache.stratos.messaging.listener.application.signup.CompleteApplicationSignUpsEventListener;
import org.apache.stratos.messaging.message.receiver.application.signup.ApplicationSignUpEventReceiver;

/**
 * Load balancer application signup event receiver.
 */
public class LoadBalancerApplicationSignUpEventReceiver extends ApplicationSignUpEventReceiver {

    private static final Log log = LogFactory.getLog(LoadBalancerApplicationSignUpEventReceiver.class);

    public LoadBalancerApplicationSignUpEventReceiver() {
        addEventListeners();
    }

    private void addEventListeners() {
        addEventListener(new CompleteApplicationSignUpsEventListener() {
            @Override
            protected void onEvent(Event event) {
                if (log.isDebugEnabled()) {
                    log.debug("Complete application signup event received");
                }
                CompleteApplicationSignUpsEvent completeApplicationSignUpsEvent = (CompleteApplicationSignUpsEvent)event;
                for(ApplicationSignUp applicationSignUp : completeApplicationSignUpsEvent.getApplicationSignUps()) {
                    if(applicationSignUp.getDomainMappings() != null) {
                        for (DomainMapping domainMapping : applicationSignUp.getDomainMappings()) {
                            if(domainMapping != null) {
                                LoadBalancerContextUtil.addClusterAgainstDomain(
                                        domainMapping.getServiceName(),
                                        domainMapping.getClusterId(),
                                        domainMapping.getDomainName());

                                LoadBalancerContextUtil.addContextPathAgainstDomain(domainMapping.getDomainName(),
                                        domainMapping.getContextPath());
                            }
                        }
                    }
                }
            }
        });
    }
}
