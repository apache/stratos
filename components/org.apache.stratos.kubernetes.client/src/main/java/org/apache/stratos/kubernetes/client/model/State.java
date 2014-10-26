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

public class State {

	private Manifest manifest;
	private String status;
	private String host;
	private String hostIP;
	private String podIP;
	private int replicas;
	private Selector replicaSelector;
	private Pod podTemplate;
	private Policy restartPolicy;
	private Object info;
	
	public Manifest getManifest() {
		return manifest;
	}
	public void setManifest(Manifest manifest) {
		this.manifest = manifest;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getHostIP() {
		return hostIP;
	}
	public void setHostIP(String hostIP) {
		this.hostIP = hostIP;
	}
	public Policy getRestartPolicy() {
		return restartPolicy;
	}
	public void setRestartPolicy(Policy restartPolicy) {
		this.restartPolicy = restartPolicy;
	}
	public String getPodIP() {
		return podIP;
	}
	public void setPodIP(String podIP) {
		this.podIP = podIP;
	}
	public int getReplicas() {
		return replicas;
	}
	public void setReplicas(int replicas) {
		this.replicas = replicas;
	}
	public Selector getReplicaSelector() {
		return replicaSelector;
	}
	public void setReplicaSelector(Selector replicaSelector) {
		this.replicaSelector = replicaSelector;
	}
	public Pod getPodTemplate() {
		return podTemplate;
	}
	public void setPodTemplate(Pod podTemplate) {
		this.podTemplate = podTemplate;
	}
	public Object getInfo() {
		return info;
	}
	public void setInfo(Object info) {
		this.info = info;
	}
	
	@Override
	public String toString() {
		return "State [manifest=" + manifest + ", status=" + status + ", host="
				+ host + ", hostIP=" + hostIP + ", podIP=" + podIP
				+ ", replicas=" + replicas + ", replicaSelector="
				+ replicaSelector + ", podTemplate=" + podTemplate
				+ ", restartPolicy=" + restartPolicy + ", info=" + info + "]";
	}
}
