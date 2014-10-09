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
package org.apache.stratos.rest.endpoint.bean.kubernetes;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Had to wrap {@link Property} array using a class, since there's a bug in current
 * stub generation.
 */
public class Properties implements Serializable{

    private static final long serialVersionUID = 1986895299288322592L;
    private Property[] properties;

    public Property[] getProperties() {
        return properties;
    }

    public void setProperties(Property[] properties) {
        if(properties == null) {
            this.properties = new Property[0];
        } else {
            this.properties = Arrays.copyOf(properties, properties.length);
        }
    }

    @Override
    public String toString() {
        return "Properties [properties=" + Arrays.toString(properties) + "]";
    }
    
}