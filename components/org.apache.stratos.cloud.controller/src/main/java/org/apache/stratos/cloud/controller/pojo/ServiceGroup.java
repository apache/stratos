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

package org.apache.stratos.cloud.controller.pojo;

import java.io.Serializable;
import java.util.List;

public class ServiceGroup implements Serializable {


	private static final long serialVersionUID = -7413745300105885793L;

	private String name;

    private String [] subGroups;

    private String [] cartridges;

    private Dependencies dependencies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String [] getSubGroups() {
        return subGroups;
    }

    public void setSubGroups(String [] subGroups) {
        this.subGroups = subGroups;
    }

    public String [] getCartridges() {
        return cartridges;
    }

    public void setCartridges(String [] cartridges) {
        this.cartridges = cartridges;
    }

    public Dependencies getDependencies() {
        return dependencies;
    }

    public void setDependencies(Dependencies dependencies) {
        this.dependencies = dependencies;
    }
}
