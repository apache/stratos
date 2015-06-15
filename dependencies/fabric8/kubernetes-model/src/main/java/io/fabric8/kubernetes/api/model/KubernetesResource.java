package io.fabric8.kubernetes.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

@JsonDeserialize(using = KubernetesDeserializer.class)
public interface KubernetesResource {
}
