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
package org.apache.stratos.autoscaler.applications.pojo;

import java.io.Serializable;
import java.util.Arrays;

public class PersistenceContext implements Serializable{
	
	private static final long serialVersionUID = 3455721979991902731L;

    private boolean persistanceRequired;
	private VolumeContext[] volumes;

    public String toString () {
        return "Persistence Required: " + isPersistenceRequired();
    }

    public boolean isPersistenceRequired() {
        return persistanceRequired;
    }

    public void setPersistenceRequired(boolean persistenceRequired) {
        this.persistanceRequired = persistanceRequired;
    }

    public VolumeContext[] getVolumes() {
        return volumes;
    }

    public void setVolumes(VolumeContext[] volumes) {
        if(volumes == null) {
            this.volumes = new VolumeContext[0];
        } else {
            this.volumes = Arrays.copyOf(volumes, volumes.length);
        }
    }
}
