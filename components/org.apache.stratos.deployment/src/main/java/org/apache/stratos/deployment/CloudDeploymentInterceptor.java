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

package org.apache.stratos.deployment;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.axis2.engine.AxisObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.util.ArrayList;

/**
 * Deployment interceptor for handling modification of ServiceAdmin so that the service list is not
 * displayed when the user has not logged in.
 */
public class CloudDeploymentInterceptor implements AxisObserver {
    private static final Log log = LogFactory.getLog(CloudDeploymentInterceptor.class);

    public void init(AxisConfiguration axisConfiguration) {
    }

    public void serviceUpdate(AxisEvent axisEvent, AxisService axisService) {
        try {
            if (axisEvent.getEventType() == AxisEvent.SERVICE_DEPLOY &&
                    axisService.getName().equals("ServiceAdmin")){
                AxisOperation operation = axisService.getOperation(new QName("listServiceGroups"));
                ArrayList<Parameter> params = operation.getParameters();
                for(Parameter param: params) {
                    operation.removeParameter(param);
                }
                Parameter authAction = new Parameter("AuthorizationAction",
                        "/permission/admin/manage/monitor/service");
                operation.addParameter(authAction);
            }
        } catch (AxisFault e) {
            log.error("Cannot add AuthorizationAction parameter to ServiceAdmin", e);
        }
    }

    public void serviceGroupUpdate(AxisEvent axisEvent, AxisServiceGroup axisServiceGroup) {
    }

    public void moduleUpdate(AxisEvent axisEvent, AxisModule axisModule) {
    }

    public void addParameter(Parameter parameter) throws AxisFault {
    }

    public void removeParameter(Parameter parameter) throws AxisFault {
    }

    public void deserializeParameters(OMElement omElement) throws AxisFault {
    }

    public Parameter getParameter(String s) {
        return null;
    }

    public ArrayList<Parameter> getParameters() {
        return null;
    }

    public boolean isParameterLocked(String s) {
        return false;
    }
}
