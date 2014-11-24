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

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

/**
 * https://github.com/GoogleCloudPlatform/kubernetes/blob/master/api/doc/manifest-schema.json
 *
 *
 */
public class Manifest {

	private String version;
	private String id;
	private Container[] containers;
	private Volume[] volumes;
	
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Container[] getContainers() {
		return containers;
	}
	public void setContainers(Container[] containers) {
		this.containers = ArrayUtils.clone(containers);
	}
	public Volume[] getVolumes() {
		return volumes;
	}
	public void setVolumes(Volume[] volumes) {
		this.volumes = ArrayUtils.clone(volumes);
	}
	@Override
	public String toString() {
		return "Manifest [version=" + version + ", id=" + id + ", containers="
				+ Arrays.toString(containers) + ", volumes="
				+ Arrays.toString(volumes) + "]";
	}
	
}
