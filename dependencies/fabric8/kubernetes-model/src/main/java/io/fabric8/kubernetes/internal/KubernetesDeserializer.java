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
