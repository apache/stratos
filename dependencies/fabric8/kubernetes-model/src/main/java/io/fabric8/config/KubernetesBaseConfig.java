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
