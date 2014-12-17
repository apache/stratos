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

package org.apache.stratos.common.beans.autoscaler.policy.deployment;


import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class DeploymentPolicy {

    private String applicationId;
    private String description;
    private boolean isPublic;
    private ApplicationPolicy applicationPolicy;
    private List<ChildPolicy> childPolicies;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public boolean getIsPublic() {
        return isPublic();
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ApplicationPolicy getApplicationPolicy() {
        return applicationPolicy;
    }

    public void setApplicationPolicy(ApplicationPolicy applicationPolicy) {
        this.applicationPolicy = applicationPolicy;
    }

    public List<ChildPolicy> getChildPolicies() {
        return childPolicies;
    }

    public void setChildPolicies(List<ChildPolicy> childPolicies) {
        this.childPolicies = childPolicies;
    }
}
