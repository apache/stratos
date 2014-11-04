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

import java.io.Serializable;

/**
 * Holds a property
 */
public class Property implements Serializable, Cloneable {

    private static final long serialVersionUID = -2191782657999410197L;

    private String name;
    private String value;

    public Property() {
    }

    public Property(String name, String value) {
        this.setName(name);
        this.setValue(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Property clone = new Property();
        clone.setName(this.getName());
        clone.setValue(this.getValue());
        return clone;
    }

    @Override
    public String toString() {
        return "Property [name=" + name + ", value=" + value + "]";
    }

    @Override
    public boolean equals(Object anObject) {
        if (anObject == null) {
            return false;
        }

        if (!(anObject instanceof Property)) {
            return false;
        }

        Property propertyObj = (Property) anObject;
        if (this.name == null) {
            if (propertyObj.getName() != null) {
                return false;
            }
        } else if (!this.name.equals(propertyObj.getName())) {
            return false;
        }

        if (this.value == null) {
            if (propertyObj.getValue() != null) {
                return false;
            }
        } else if (!this.value.equals(propertyObj.getValue())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
    }

}
