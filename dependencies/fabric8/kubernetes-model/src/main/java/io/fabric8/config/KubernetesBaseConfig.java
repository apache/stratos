/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.fabric8.config;

import io.sundr.builder.annotations.ExternalBuildables;

@ExternalBuildables(editableEnabled=false, validationEnabled = true, builderPackage = "io.fabric8.common", value = {
        "io.fabric8.kubernetes.api.model.base.ListMeta",
        "io.fabric8.kubernetes.api.model.base.ObjectMeta",
        "io.fabric8.kubernetes.api.model.base.ObjectReference",
        "io.fabric8.kubernetes.api.model.base.Status",
        "io.fabric8.kubernetes.api.model.base.StatusCause",
        "io.fabric8.kubernetes.api.model.base.StatusDetails"
})
public class KubernetesBaseConfig {
}
