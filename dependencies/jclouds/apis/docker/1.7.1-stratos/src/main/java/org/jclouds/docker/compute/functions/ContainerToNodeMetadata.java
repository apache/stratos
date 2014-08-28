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
package org.jclouds.docker.compute.functions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;

import org.jclouds.compute.domain.HardwareBuilder;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.Processor;
import org.jclouds.compute.functions.GroupNamingConvention;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.docker.domain.Container;
import org.jclouds.docker.domain.Port;
import org.jclouds.docker.domain.State;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.logging.Logger;
import org.jclouds.providers.ProviderMetadata;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;

/**
 * @author Andrea Turli
 */
@Singleton
public class ContainerToNodeMetadata implements Function<Container, NodeMetadata> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final ProviderMetadata providerMetadata;
   private final Function<State, NodeMetadata.Status> toPortableStatus;
   private final GroupNamingConvention nodeNamingConvention;

   @Inject
   public ContainerToNodeMetadata(ProviderMetadata providerMetadata, Function<State,
           NodeMetadata.Status> toPortableStatus, GroupNamingConvention.Factory namingConvention) {
      this.providerMetadata = checkNotNull(providerMetadata, "providerMetadata");
      this.toPortableStatus = checkNotNull(toPortableStatus, "toPortableStatus cannot be null");
      this.nodeNamingConvention = checkNotNull(namingConvention, "namingConvention").createWithoutPrefix();
   }

   @Override
   public NodeMetadata apply(Container container) {
      String name = cleanUpName(container.getName());
      String group = nodeNamingConvention.extractGroup(name);
      NodeMetadataBuilder builder = new NodeMetadataBuilder();
      builder.ids(container.getId())
              .name(name)
              .group(group)
              .hostname(container.getConfig().getHostname())
               // TODO Set up hardware
              .hardware(new HardwareBuilder()
                      .id("")
                      .ram(container.getConfig().getMemory())
                      .processor(new Processor(container.getConfig().getCpuShares(), container.getConfig().getCpuShares()))
                      .build());
      // TODO Set up location properly
      LocationBuilder locationBuilder = new LocationBuilder();
      locationBuilder.description("");
      locationBuilder.id("");
      locationBuilder.scope(LocationScope.HOST);
      builder.location(locationBuilder.build());
      builder.status(toPortableStatus.apply(container.getState()));
      builder.imageId(container.getImage());
      builder.loginPort(getLoginPort(container));
      builder.publicAddresses(getPublicIpAddresses());
      builder.privateAddresses(getPrivateIpAddresses(container));
      builder.operatingSystem(OperatingSystem.builder().description("linux").family(OsFamily.LINUX).build());
      return builder.build();
   }

   private String cleanUpName(String name) {
      return name.startsWith("/") ? name.substring(1) : name;
   }

   private Iterable<String> getPrivateIpAddresses(Container container) {
      if (container.getNetworkSettings() == null) return ImmutableList.of();
      return ImmutableList.of(container.getNetworkSettings().getIpAddress());
   }

   private List<String> getPublicIpAddresses() {
      String dockerIpAddress = URI.create(providerMetadata.getEndpoint()).getHost();
      return ImmutableList.of(dockerIpAddress);
   }

   protected static int getLoginPort(Container container) {
      if (container.getNetworkSettings() != null) {
          Map<String, List<Map<String,String>>> ports = container.getNetworkSettings().getPorts();
          if(ports != null) {
            return Integer.parseInt(getOnlyElement(ports.get("22/tcp")).get("HostPort"));
          }
      // this is needed in case the container list is coming from listContainers
      } else if (container.getPorts() != null) {
         for (Port port : container.getPorts()) {
            if (port.getPrivatePort() == 22) {
               return port.getPublicPort();
            }
         }
      }
      throw new IllegalStateException("Cannot determine the login port for " + container.getId());
   }
}
