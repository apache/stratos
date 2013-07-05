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

package org.apache.stratos.autoscaler.service.internal;

import org.apache.stratos.autoscaler.service.impl.AutoscalerServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.apache.stratos.autoscaler.service.IAutoscalerService;

/**
 * Registering Autoscaler Service.
 * @scr.component name="org.apache.stratos.autoscaler.service" immediate="true"
 */
public class AutoscalerServiceDSComponent {

    private static final Log log = LogFactory.getLog(AutoscalerServiceDSComponent.class);

    protected void activate(ComponentContext context) {
        try {
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(IAutoscalerService.class.getName(),
                                          new AutoscalerServiceImpl(), null);

            log.debug("******* Autoscaler Service bundle is activated ******* ");
        } catch (Exception e) {
            log.error("******* Autoscaler Service bundle is failed to activate ****", e);
        }
    }
}