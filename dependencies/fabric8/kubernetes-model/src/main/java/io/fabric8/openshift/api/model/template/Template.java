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

package io.fabric8.openshift.api.model.template;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.*;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;


/**
 *
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "apiVersion",
    "kind",
    "labels",
    "metadata",
    "objects",
    "parameters"
})
@JsonDeserialize(using = JsonDeserializer.None.class)
public class Template implements HasMetadata {

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("apiVersion")
    @NotNull
    private Template.ApiVersion apiVersion = Template.ApiVersion.fromValue("v1beta3");
    /**
     *
     * (Required)
     *
     */
    @JsonProperty("kind")
    @NotNull
    private java.lang.String kind = "Template";
    /**
     *
     *
     */
    @JsonProperty("labels")
    @Valid
    private Map<String, String> labels;
    /**
     *
     *
     */
    @JsonProperty("metadata")
    @Valid
    private ObjectMeta metadata;
    /**
     *
     *
     */
    @JsonProperty("objects")
    @Valid
    private List<HasMetadata> objects = new ArrayList<HasMetadata>();
    /**
     *
     *
     */
    @JsonProperty("parameters")
    @Valid
    private List<Parameter> parameters = new ArrayList<Parameter>();
    @JsonIgnore
    private Map<java.lang.String, java.lang.Object> additionalProperties = new HashMap<java.lang.String, java.lang.Object>();


    /**
     * No args constructor for use in serialization
     *
     */
    public Template() {
    }

    /**
     *
     * @param apiVersion
     * @param labels
     * @param parameters
     * @param objects
     * @param kind
     * @param metadata
     */
    public Template(Template.ApiVersion apiVersion, java.lang.String kind, Map<String, String> labels, ObjectMeta metadata, List<HasMetadata> objects, List<Parameter> parameters) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.labels = labels;
        this.metadata = metadata;
        this.objects = objects;
        this.parameters = parameters;

        this.setObjects(objects);
    }

    /**
     *
     * (Required)
     *
     * @return
     *     The apiVersion
     */
    @JsonProperty("apiVersion")
    public Template.ApiVersion getApiVersion() {
        return apiVersion;
    }

    /**
     *
     * (Required)
     *
     * @param apiVersion
     *     The apiVersion
     */
    @JsonProperty("apiVersion")
    public void setApiVersion(Template.ApiVersion apiVersion) {
        this.apiVersion = apiVersion;
    }

    /**
     *
     * (Required)
     *
     * @return
     *     The kind
     */
    @JsonProperty("kind")
    public java.lang.String getKind() {
        return kind;
    }

    /**
     *
     * (Required)
     *
     * @param kind
     *     The kind
     */
    @JsonProperty("kind")
    public void setKind(java.lang.String kind) {
        this.kind = kind;
    }

    /**
     *
     *
     * @return
     *     The labels
     */
    @JsonProperty("labels")
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     *
     *
     * @param labels
     *     The labels
     */
    @JsonProperty("labels")
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    /**
     *
     *
     * @return
     *     The metadata
     */
    @JsonProperty("metadata")
    public ObjectMeta getMetadata() {
        return metadata;
    }

    /**
     *
     *
     * @param metadata
     *     The metadata
     */
    @JsonProperty("metadata")
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    /**
     *
     *
     * @return
     *     The objects
     */
    @JsonProperty("objects")
    public List<HasMetadata> getObjects() {
        return objects;
    }

    public void setObjects(List<HasMetadata> objects) {
        this.objects = objects;
    }

    /**
     *
     *
     * @return
     *     The parameters
     */
    @JsonProperty("parameters")
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     *
     *
     * @param parameters
     *     The parameters
     */
    @JsonProperty("parameters")
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public java.lang.String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @JsonAnyGetter
    public Map<java.lang.String, java.lang.Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(java.lang.String name, java.lang.Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(apiVersion).append(kind).append(labels).append(metadata).append(objects).append(parameters).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(java.lang.Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Template) == false) {
            return false;
        }
        Template rhs = ((Template) other);
        return new EqualsBuilder().append(apiVersion, rhs.apiVersion).append(kind, rhs.kind).append(labels, rhs.labels).append(metadata, rhs.metadata).append(objects, rhs.objects).append(parameters, rhs.parameters).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

    @Generated("org.jsonschema2pojo")
    public static enum ApiVersion {

        V_1_BETA_3("v1beta3"),
        V_1("v1");
        private final java.lang.String value;
        private static Map<java.lang.String, Template.ApiVersion> constants = new HashMap<java.lang.String, Template.ApiVersion>();

        static {
            for (Template.ApiVersion c: values()) {
                constants.put(c.value, c);
            }
        }

        private ApiVersion(java.lang.String value) {
            this.value = value;
        }

        @JsonValue
        @Override
        public java.lang.String toString() {
            return this.value;
        }

        @JsonCreator
        public static Template.ApiVersion fromValue(java.lang.String value) {
            Template.ApiVersion constant = constants.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }
}
