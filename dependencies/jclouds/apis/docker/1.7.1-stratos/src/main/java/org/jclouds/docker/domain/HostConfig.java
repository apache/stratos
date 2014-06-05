/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.docker.domain;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.annotations.SerializedName;
import org.jclouds.javax.annotation.Nullable;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Andrea Turli
 */
public class HostConfig {

   @SerializedName("ContainerIDFile")
   private final String containerIDFile;
   @SerializedName("Binds")
   private final List<String> binds;
   @SerializedName("Privileged")
   private final boolean privileged;
   @SerializedName("PortBindings")
   private final Map<String, List<Map<String, String>>> portBindings;
   @SerializedName("Links")
   private final List<String> links;
   @SerializedName("PublishAllPorts")
   private final boolean publishAllPorts;

   @ConstructorProperties({ "ContainerIDFile", "Binds", "Privileged", "PortBindings", "Links", "Size",
           "PublishAllPorts" })
   public HostConfig(@Nullable String containerIDFile, @Nullable List<String> binds, @Nullable boolean privileged,
                     @Nullable Map<String, List<Map<String, String>>> portBindings, @Nullable List<String> links,
                     @Nullable boolean publishAllPorts) {
      this.containerIDFile = containerIDFile;
      this.binds = binds != null ? ImmutableList.copyOf(binds) : ImmutableList.<String> of();
      this.privileged = privileged;
      this.portBindings = portBindings != null ? ImmutableMap.copyOf(portBindings) : ImmutableMap.<String, List<Map<String, String>>> of();
      this.links = links != null ? ImmutableList.copyOf(links) : ImmutableList.<String> of();
      this.publishAllPorts = publishAllPorts;
   }

   public String getContainerIDFile() {
      return containerIDFile;
   }

   public List<String> getBinds() {
      return binds;
   }

   public boolean isPrivileged() {
      return privileged;
   }

   public Map<String, List<Map<String, String>>> getPortBindings() {
      return portBindings;
   }

   @Nullable
   public List<String> getLinks() {
      return links;
   }

   public boolean isPublishAllPorts() {
      return publishAllPorts;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      HostConfig that = (HostConfig) o;

      return Objects.equal(this.containerIDFile, that.containerIDFile) &&
              Objects.equal(this.binds, that.binds) &&
              Objects.equal(this.privileged, that.privileged) &&
              Objects.equal(this.portBindings, that.portBindings) &&
              Objects.equal(this.links, that.links) &&
              Objects.equal(this.publishAllPorts, that.publishAllPorts);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(containerIDFile, binds, privileged, portBindings, links, publishAllPorts);
   }

   @Override
   public String toString() {
      return Objects.toStringHelper(this)
              .add("containerIDFile", containerIDFile)
              .add("binds", binds)
              .add("privileged", privileged)
              .add("portBindings", portBindings)
              .add("links", links)
              .add("publishAllPorts", publishAllPorts)
              .toString();
   }

   public static Builder builder() {
      return new Builder();
   }

   public Builder toBuilder() {
      return builder().fromHostConfig(this);
   }

   public static final class Builder {

      private String containerIDFile;
      private List<String> binds = ImmutableList.of();
      private boolean privileged;
      private Map<String, List<Map<String, String>>> portBindings = ImmutableMap.of();
      private List<String> links = ImmutableList.of();
      private boolean publishAllPorts;

      public Builder containerIDFile(String containerIDFile) {
         this.containerIDFile = containerIDFile;
         return this;
      }

      public Builder binds(List<String> binds) {
         this.binds = ImmutableList.copyOf(checkNotNull(binds, "binds"));
         return this;
      }

      public Builder privileged(boolean privileged) {
         this.privileged = privileged;
         return this;
      }

      public Builder links(List<String> links) {
         this.links = ImmutableList.copyOf(checkNotNull(links, "links"));
         return this;
      }

      public Builder portBindings(Map<String, List<Map<String, String>>> portBindings) {
         this.portBindings = ImmutableMap.copyOf(portBindings);
         return this;
      }

      public Builder publishAllPorts(boolean publishAllPorts) {
         this.publishAllPorts = publishAllPorts;
         return this;
      }

      public HostConfig build() {
         return new HostConfig(containerIDFile, binds, privileged, portBindings, links, publishAllPorts);
      }

      public Builder fromHostConfig(HostConfig in) {
         return this
                 .containerIDFile(in.getContainerIDFile())
                 .binds(in.getBinds())
                 .privileged(in.isPrivileged())
                 .links(in.getLinks())
                 .portBindings(in.getPortBindings())
                 .publishAllPorts(in.isPublishAllPorts());
      }
   }
}
