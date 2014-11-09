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
package org.apache.stratos.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Had to wrap {@link Property} array using a class, since there's a bug in current
 * stub generation.
 */
public class Properties implements Serializable {

    private static final long serialVersionUID = -9094584151615076171L;
    private static final Log log = LogFactory.getLog(Properties.class);

    private List<Property> properties;

    public Properties() {
        this.properties = new ArrayList<Property>();
    }

    public Property[] getProperties() {
        return properties.toArray(new Property[properties.size()]);
    }

    public void addProperty(Property property) {
        try {
            this.properties.add((Property) property.clone());
        } catch (CloneNotSupportedException e) {
            log.error(e);
        }
    }

    public void setProperties(Property[] properties) {
        this.properties = new ArrayList<Property>();
        Collections.addAll(this.properties, properties.clone());
    }

    @Override
    public String toString() {
        return "Properties [properties=" + this.getProperties() + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }

        if (!(object instanceof Properties)) {
            return false;
        }

        Properties propertiesObject = (Properties) object;
        return Arrays.equals(propertiesObject.getProperties(), this.getProperties());
    }

    @Override
    public int hashCode() {
        return this.hashCode();
    }
}