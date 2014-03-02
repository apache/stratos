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

package org.apache.stratos.manager.subscription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.pojo.Property;

public class PersistenceContext {

    private static Log log = LogFactory.getLog(PersistenceContext.class);

    private Property persistanceRequiredProperty;
    private Property sizeProperty;
    private Property deleteOnTerminationProperty;

    public PersistenceContext () {
        persistanceRequiredProperty = new Property();
        sizeProperty = new Property();
        deleteOnTerminationProperty = new Property();
    }


    public Property getPersistanceRequiredProperty() {
        return persistanceRequiredProperty;
    }

    public void setPersistanceRequiredProperty(String propertyName, String propertyValue) {
        this.persistanceRequiredProperty.setName(propertyName);
        this.persistanceRequiredProperty.setValue(propertyValue);
    }

    public Property getSizeProperty() {
        return sizeProperty;
    }

    public void setSizeProperty(String propertyName, String propertyValue) {
        this.sizeProperty.setName(propertyName);
        this.sizeProperty.setValue(propertyValue);
    }

    public Property getDeleteOnTerminationProperty() {
        return deleteOnTerminationProperty;
    }

    public void setDeleteOnTerminationProperty(String propertyName, String propertyValue) {
        this.deleteOnTerminationProperty.setName(propertyName);
        this.deleteOnTerminationProperty.setValue(propertyValue);
    }
}
