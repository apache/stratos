/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.stratos.kubernetes.client.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * https://github.com/GoogleCloudPlatform/kubernetes/blob/master/api/doc/pod-schema.json
 * @author nirmal
 *
 */
@XmlRootElement
public class Pod {

	private String kind;
	private String id;
	private String creationTimestamp;
	private String selfLink;
	private String resourceVersion;
	private String apiVersion;
	private State desiredState;
	private State currentState;
	private String status;
	private String message;
	private String code;
	private Label labels;
	
	public String getKind() {
		return kind;
	}
	public void setKind(String kind) {
		this.kind = kind;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getCreationTimestamp() {
		return creationTimestamp;
	}
	public void setCreationTimestamp(String creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}
	public String getSelfLink() {
		return selfLink;
	}
	public void setSelfLink(String selfLink) {
		this.selfLink = selfLink;
	}
	public State getDesiredState() {
		return desiredState;
	}
	public void setDesiredState(State desiredState) {
		this.desiredState = desiredState;
	}
	public State getCurrentState() {
		return currentState;
	}
	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}
	public String getResourceVersion() {
		return resourceVersion;
	}
	public void setResourceVersion(String resourceVersion) {
		this.resourceVersion = resourceVersion;
	}
	public String getApiVersion() {
		return apiVersion;
	}
	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}
	public Label getLabels() {
		return labels;
	}
	public void setLabels(Label labels) {
		this.labels = labels;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	@Override
	public String toString() {
		return "Pod [kind=" + kind + ", id=" + id + ", creationTimestamp="
				+ creationTimestamp + ", selfLink=" + selfLink
				+ ", resourceVersion=" + resourceVersion + ", apiVersion="
				+ apiVersion + ", desiredState=" + desiredState
				+ ", currentState=" + currentState + ", status=" + status
				+ ", message=" + message + ", code=" + code + ", labels="
				+ labels + "]";
	}
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Pod other = (Pod) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
	
	
}
