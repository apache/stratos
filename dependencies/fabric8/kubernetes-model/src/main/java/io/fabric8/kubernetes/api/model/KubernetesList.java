package io.fabric8.kubernetes.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.annotation.Generated;
import java.util.List;


/**
 *
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "annotations",
        "apiVersion",
        "creationTimestamp",
        "deletionTimestamp",
        "generateName",
        "id",
        "items",
        "kind",
        "namespace",
        "resourceVersion",
        "selfLink",
        "uid"
})
@JsonDeserialize(using = JsonDeserializer.None.class)
public class KubernetesList extends BaseKubernetesList implements KubernetesResource {

    /**
     * No args constructor for use in serialization
     */
    public KubernetesList() {
        super();
    }

    public KubernetesList(KubernetesList.ApiVersion apiVersion,
                          List<HasMetadata> items,
                          String kind,
                          ListMeta metadata) {
        super(apiVersion, items, kind, metadata);
        this.setItems(items);
    }
}
