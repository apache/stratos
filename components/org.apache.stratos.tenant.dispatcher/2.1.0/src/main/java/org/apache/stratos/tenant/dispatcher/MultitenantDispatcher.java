/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.tenant.dispatcher;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.xml.namespace.QName;

/**
 * If none of the dispatcher were able to find an Axis2 service or operation, this dispatcher will
 * be reached, and it will dispatch to the MultitenantService, which is associated with the
 * {@link org.wso2.carbon.core.multitenancy.MultitenantMessageReceiver}
 */
public class MultitenantDispatcher extends AbstractDispatcher {

    public static final String NAME = "MultitenantDispatcher";

    public void initDispatcher() {
        QName qn = new QName("http://wso2.org/projects/carbon", NAME);
        HandlerDescription hd = new HandlerDescription(qn.getLocalPart());
        super.init(hd);
    }

    public AxisService findService(MessageContext mc) throws AxisFault {
        AxisService service = mc.getAxisService();
        if (service == null) {
            String to = mc.getTo().getAddress();

            int tenantDelimiterIndex = to.indexOf("/t/");
            if (tenantDelimiterIndex != -1) {
                AxisConfiguration ac = mc.getConfigurationContext().getAxisConfiguration();
                return ac.getService(MultitenantConstants.MULTITENANT_DISPATCHER_SERVICE);
            }
        }
        return service;
    }

    public AxisOperation findOperation(AxisService svc, MessageContext mc) throws AxisFault {
        AxisOperation operation = mc.getAxisOperation();
        if (operation == null) {
            return svc.getOperation(MultitenantConstants.MULTITENANT_DISPATCHER_OPERATION);
        }
        return operation;
    }
}
