/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.load.balancer.common.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;

/**
 *
 */
public class LoadBalancerCommonServiceComponent {

    private static final Log log = LogFactory.getLog(LoadBalancerCommonServiceComponent.class);

    protected void activate(ComponentContext context) {
        try {
            if(log.isDebugEnabled()) {
                log.debug("Load Balancer Common Service bundle activated");
            }
        } catch (Exception e) {
            log.error("Could not activate Load Balancer Common Service bundle", e);
        }
    }
}
