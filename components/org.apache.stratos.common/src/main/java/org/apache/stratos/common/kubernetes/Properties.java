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
package org.apache.stratos.common.kubernetes;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Had to wrap {@link Property} array using a class, since there's a bug in current
 * stub generation.
 */
public class Properties implements Serializable {

    private Property[] properties;

    public Property[] getProperties() {
        return properties;
    }

    public void setProperties(Property[] properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "Properties [properties=" + Arrays.toString(properties) + "]";
    }

    @Override
    public boolean equals(Object anObject) {
        if (anObject == null) {
            return false;
        }
        if (this == anObject) {
            return false;
        }

        if (!(anObject instanceof Properties)) {
            return false;
        }
        Properties propertiesObj = (Properties) anObject;
        if (this.properties == null) {
            if (propertiesObj.getProperties() != null) {
                return false;
            }
        } else if (!Arrays.equals(this.properties, propertiesObj.getProperties())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.properties == null) ? 0 : Arrays.hashCode(this.properties));
        return result;
    }
}