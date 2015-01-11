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

package org.apache.stratos.common.beans.application.signup;

import org.apache.stratos.common.beans.artifact.repository.ArtifactRepositoryBean;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * Application signup definition.
 */
@XmlRootElement(name="applicationSignUp")
public class ApplicationSignUpBean implements Serializable {

    private static final long serialVersionUID = -3055522170914869018L;

    private List<ArtifactRepositoryBean> artifactRepositories;

    public List<ArtifactRepositoryBean> getArtifactRepositories() {
        return artifactRepositories;
    }

    public void setArtifactRepositories(List<ArtifactRepositoryBean> artifactRepositories) {
        this.artifactRepositories = artifactRepositories;
    }
}
