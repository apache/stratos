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

package io.fabric8.kubernetes.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.KubernetesKind;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.io.IOException;

public class KubernetesDeserializer extends JsonDeserializer<KubernetesResource> {

    private static final String KIND = "kind";

    @Override
    public KubernetesResource deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectNode node = jp.readValueAsTree();
        JsonNode kind = node.get(KIND);
        if (kind != null) {
            String value = kind.textValue();
            Class<? extends KubernetesResource> resourceType = KubernetesKind.getTypeForName(value);
            if (resourceType == null) {
                throw ctxt.mappingException("No resource type found for kind:" + value);
            } else {
                return jp.getCodec().treeToValue(node, resourceType);
            }
        }
        return null;
    }
}
