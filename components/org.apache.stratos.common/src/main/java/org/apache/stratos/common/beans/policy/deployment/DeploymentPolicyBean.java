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

package org.apache.stratos.common.beans.policy.deployment;


import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class DeploymentPolicyBean {

    private String description;
    private boolean isPublic;
    private ApplicationPolicyBean applicationPolicy;
    private List<ChildPolicyBean> childPolicies;

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

    public ApplicationPolicyBean getApplicationPolicy() {
        return applicationPolicy;
    }

    public void setApplicationPolicy(ApplicationPolicyBean applicationPolicy) {
        this.applicationPolicy = applicationPolicy;
    }

    public List<ChildPolicyBean> getChildPolicies() {
        return childPolicies;
    }

    public void setChildPolicies(List<ChildPolicyBean> childPolicies) {
        this.childPolicies = childPolicies;
    }
}
