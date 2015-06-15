package io.fabric8.kubernetes.api.model;

public interface HasMetadata extends KubernetesResource {

  ObjectMeta getMetadata();
  void setMetadata(ObjectMeta metadata);
}
