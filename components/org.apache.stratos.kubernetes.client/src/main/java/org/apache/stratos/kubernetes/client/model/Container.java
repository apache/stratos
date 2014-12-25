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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public class Container {

	private String name;
	private String image;
	private String workingDir;
	private String[] command;
	private VolumeMount[] volumeMounts;
	private List<Port> ports;
	private String imagePullPolicy;
	private EnvironmentVariable[] env;

	public Container() {
		ports = new ArrayList<Port>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getWorkingDir() {
		return workingDir;
	}

	public void setWorkingDir(String workingDir) {
		this.workingDir = workingDir;
	}

	public String[] getCommand() {
		return command;
	}

	public void setCommand(String[] command) {
		this.command = ArrayUtils.clone(command);
	}

	public VolumeMount[] getVolumeMounts() {
		return volumeMounts;
	}

	public void setVolumeMounts(VolumeMount[] volumeMounts) {
		this.volumeMounts = ArrayUtils.clone(volumeMounts);
	}

	public List<Port> getPorts() {
		return ports;
	}

	public void addPort(Port port) {
		this.ports.add(port);
	}

	public void setPorts(List<Port> ports) {
		this.ports = ports;
	}

	public EnvironmentVariable[] getEnv() {
		return env;
	}

	public void setEnv(EnvironmentVariable[] env) {
		this.env = ArrayUtils.clone(env);
	}

	@Override
	public String toString() {
		return "Container [name=" + name + ", image=" + image + ", workingDir="
				+ workingDir + ", command=" + Arrays.toString(command)
				+ ", volumeMounts=" + Arrays.toString(volumeMounts)
				+ ", ports=" + ports + ", env="
				+ Arrays.toString(env) + "]";
	}

	public String getImagePullPolicy() {
		return imagePullPolicy;
	}

	public void setImagePullPolicy(String imagePullPolicy) {
		this.imagePullPolicy = imagePullPolicy;
	}
}
