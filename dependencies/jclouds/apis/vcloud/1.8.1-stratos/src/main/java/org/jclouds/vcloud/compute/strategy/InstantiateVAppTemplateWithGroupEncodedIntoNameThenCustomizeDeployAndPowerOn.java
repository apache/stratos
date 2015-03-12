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
package org.jclouds.vcloud.compute.strategy;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Iterables.get;
import static org.jclouds.compute.util.ComputeServiceUtils.getCores;
import static org.jclouds.compute.util.ComputeServiceUtils.metadataAndTagsAsCommaDelimitedValue;
import static org.jclouds.util.Predicates2.retry;
import static org.jclouds.vcloud.compute.util.VCloudComputeUtils.getCredentialsFrom;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.jclouds.cim.ResourceAllocationSettingData;
import org.jclouds.compute.ComputeServiceAdapter.NodeAndInitialCredentials;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.logging.Logger;
import org.jclouds.ovf.Network;
import org.jclouds.predicates.validators.DnsNameValidator;
import org.jclouds.rest.annotations.BuildVersion;
import org.jclouds.vcloud.TaskStillRunningException;
import org.jclouds.vcloud.VCloudApi;
import org.jclouds.vcloud.compute.options.VCloudTemplateOptions;
import org.jclouds.vcloud.domain.*;
import org.jclouds.vcloud.domain.NetworkConnectionSection.Builder;
import org.jclouds.vcloud.domain.internal.VmImpl;
import org.jclouds.vcloud.domain.network.IpAddressAllocationMode;
import org.jclouds.vcloud.domain.network.NetworkConfig;
import org.jclouds.vcloud.domain.ovf.VCloudNetworkAdapter;
import org.jclouds.vcloud.options.InstantiateVAppTemplateOptions;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import org.jclouds.vcloud.predicates.TaskSuccess;

@Singleton
public class InstantiateVAppTemplateWithGroupEncodedIntoNameThenCustomizeDeployAndPowerOn {
   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   protected final VCloudApi client;
   protected final Predicate<URI> successTester;
   protected final LoadingCache<URI, VAppTemplate> vAppTemplates;
   protected final NetworkConfigurationForNetworkAndOptions networkConfigurationForNetworkAndOptions;
   protected final String buildVersion;


   @Inject
   protected InstantiateVAppTemplateWithGroupEncodedIntoNameThenCustomizeDeployAndPowerOn(VCloudApi client,
            Predicate<URI> successTester, LoadingCache<URI, VAppTemplate> vAppTemplates, NetworkConfigurationForNetworkAndOptions networkConfigurationForNetworkAndOptions,
            @BuildVersion String buildVersion) {
      this.client = client;
      this.successTester = successTester;
      this.vAppTemplates = vAppTemplates;
      this.networkConfigurationForNetworkAndOptions = networkConfigurationForNetworkAndOptions;
      this.buildVersion = buildVersion;
   }
   
   /**
    * per john ellis at bluelock, vCloud Director 1.5 is more strict than earlier versions.
    * <p/>
    * It appears to be 15 characters to match Windows' hostname limitation. Must be alphanumeric, at
    * least one non-number character and hyphens and underscores are the only non-alpha character
    * allowed.
    */
   public static enum ComputerNameValidator  {
      INSTANCE;
      
      private DnsNameValidator validator;

      ComputerNameValidator() {
         this.validator = new  DnsNameValidator(3, 15);
      }
      
      public void validate(@Nullable String t) throws IllegalArgumentException {
         this.validator.validate(t);
      }

   }
   
   public NodeAndInitialCredentials<VApp> createNodeWithGroupEncodedIntoName(String group, String name, Template template) {
      // no sense waiting until failures occur later
      ComputerNameValidator.INSTANCE.validate(name);
      VApp vAppResponse = instantiateVAppFromTemplate(name, template);
      waitForTask(vAppResponse.getTasks().get(0));
      logger.debug("<< instantiated VApp(%s)", vAppResponse.getName());

      // vm data is available after instantiate completes
      vAppResponse = client.getVAppApi().getVApp(vAppResponse.getHref());

      // per above check, we know there is only a single VM
      Vm vm = get(vAppResponse.getChildren(), 0);

      template.getOptions().userMetadata(ComputeServiceConstants.NODE_GROUP_KEY, group);
      VCloudTemplateOptions vOptions = VCloudTemplateOptions.class.cast(template.getOptions());

      // note we cannot do tasks in parallel or VCD will throw "is busy" errors

      // note we must do this before any other customizations as there is a dependency on
      // valid naming conventions before you can perform commands such as updateCPUCount
      logger.trace(">> updating customization vm(%s) name->(%s)", vm.getName(), name);
      waitForTask(updateVmWithNameAndCustomizationScript(vm, name, vOptions.getCustomizationScript()));
      logger.trace("<< updated customization vm(%s)", name);

       ensureVmHasDesiredNetworkConnectionSettings(vAppResponse, vOptions);

      int cpuCount = (int) getCores(template.getHardware());
      logger.trace(">> updating cpuCount(%d) vm(%s)", cpuCount, vm.getName());
      waitForTask(updateCPUCountOfVm(vm, cpuCount));
      logger.trace("<< updated cpuCount vm(%s)", vm.getName());
      int memoryMB = template.getHardware().getRam();
      logger.trace(">> updating memoryMB(%d) vm(%s)", memoryMB, vm.getName());
      waitForTask(updateMemoryMBOfVm(vm, memoryMB));
      logger.trace("<< updated memoryMB vm(%s)", vm.getName());
      logger.trace(">> deploying vApp(%s)", vAppResponse.getName());
      waitForTask(client.getVAppApi().deployVApp(vAppResponse.getHref()));
      logger.trace("<< deployed vApp(%s)", vAppResponse.getName());

      // only after deploy is the password valid
      vAppResponse = client.getVAppApi().getVApp(vAppResponse.getHref());

      logger.trace(">> powering on vApp(%s)", vAppResponse.getName());
      client.getVAppApi().powerOnVApp(vAppResponse.getHref());

      return new NodeAndInitialCredentials<VApp>(vAppResponse, vAppResponse.getHref().toASCIIString(),
               getCredentialsFrom(vAppResponse));

   }
  
   @VisibleForTesting
   protected VApp instantiateVAppFromTemplate(String name, Template template) {
      VCloudTemplateOptions vOptions = VCloudTemplateOptions.class.cast(template.getOptions());
      
      URI templateId = URI.create(template.getImage().getId());

      VAppTemplate vAppTemplate = vAppTemplates.getUnchecked(templateId);

      if (vAppTemplate.getChildren().size() > 1)
         throw new UnsupportedOperationException("we currently do not support multiple vms in a vAppTemplate "
                  + vAppTemplate);

       VmImpl vmTemplate = VmImpl.class.cast(vAppTemplate.getChildren().iterator().next());

      String description = VCloudTemplateOptions.class.cast(template.getOptions()).getDescription();
      if (description == null) {
         Map<String, String> md = metadataAndTagsAsCommaDelimitedValue(template.getOptions());
         description = Joiner.on('\n').withKeyValueSeparator("=").join(md);
      }

      InstantiateVAppTemplateOptions options = InstantiateVAppTemplateOptions.Builder.description(description);

      /*
       * Match up networks in the vApp template with the ones in the options we got passed in, so that
       * the right ones can be wired together.
       * Note that in the end the order of the network interfaces is what's important, not the names. The names
       * might not match up. In the worst case, maybe someone called their vApp networks A and B, but really wants
       * them cross-connected, i.e. wired up to B and A respectively.
       * The only potential issue here is that the networks in the vOptions are stored as a Set, which makes little
       * sense. While the API is what it is, we need to rely on people higher up using sensible underlying
       * datastructures (that conform to the set interface), which preserve their order.
       */

      int vmTemplateNumNetworks = vmTemplate.getNetworkConnectionSection().getConnections().size();

      /*
       * Backwards-compatibility hack: we might get passed in a parent network URI and an empty vOptions.networks list.
       * In that case, move the parent network URI into the vOptions.networks list, and remove the parent URI.
       */
      if (vOptions.getNetworks().size() == 0 && vmTemplateNumNetworks == 1 && vOptions.getParentNetwork() != null) {
         ArrayList<String> netlist = new ArrayList<String>();
         netlist.add(vOptions.getParentNetwork().toASCIIString());
         vOptions.networks(netlist);
         vOptions.parentNetwork(null);
      }

      URI[] vOptionsNetworkIdList = new URI[vOptions.getNetworks().size()];
      NetworkConnection[] vAppTemplateNetworkList =
         new NetworkConnection[vmTemplateNumNetworks];

      //hopefully this preserves the original order, assuming the original underlying datastructure was ordered.
      int i = 0;
      for (String network: vOptions.getNetworks()) {
         try {
            vOptionsNetworkIdList[i] = new URI(network);
         } catch (URISyntaxException e) {
             logger.error(e, "Failed to convert href '" + network + "' to URI. We expect a href to a network to be " +
               "passed in, not a name for example.");
            return null;
         }
         i++;
      }

      //iterate over the NetworkConnectionSection, and put them in order of their connection indices
      // into the vAppTemplateNetworkList.
      for (NetworkConnection netCon: vmTemplate.getNetworkConnectionSection().getConnections()) {
         vAppTemplateNetworkList[netCon.getNetworkConnectionIndex()] = netCon;
      }

      for (i = 0; i < vOptionsNetworkIdList.length; i++) {
         URI parentNetwork = vOptionsNetworkIdList[i];
         NetworkConnection networkConnectionParams = vAppTemplateNetworkList.length > i ? vAppTemplateNetworkList[i] : null;
         //hook 'em up.

         //use network name from vAppTemplate if possible
         String networkName;
         if (networkConnectionParams != null) {
            networkName = networkConnectionParams.getNetwork();
         } else {
            networkName = "jclouds-net-" + String.valueOf(i);
         }

         Network n = new Network(networkName, null); // ignore description, not needed here.
         VCloudTemplateOptions networkTemplateOptions = vOptions.clone(); //we'll modify bits here
         networkTemplateOptions.parentNetwork(parentNetwork);

         NetworkConfig config = networkConfigurationForNetworkAndOptions.apply(n, networkTemplateOptions);
         // note that in VCD 1.5, the network name after instantiation will be the same as the parent
         options.addNetworkConfig(config);
         logger.debug("Connecting vApp network " + n.getName() + " to org network " + parentNetwork + ".");
      }

      // TODO make disk size specifiable
      // disk((long) ((template.getHardware().getVolumes().get(0).getSize()) *
      // 1024 * 1024l));



      options.deploy(false);
      options.powerOn(false);

      URI VDC = URI.create(template.getLocation().getId());

      logger.debug(">> instantiating vApp vDC(%s) template(%s) name(%s) options(%s) ", VDC, templateId, name, options);

      VApp vAppResponse = client.getVAppTemplateApi().createVAppInVDCByInstantiatingTemplate(name, VDC, templateId,
               options);
      return vAppResponse;
   }

   // TODO: filtering on "none" is a hack until we can filter on
   // vAppTemplate.getNetworkConfigSection().getNetworkConfigs() where
   // name = getChildren().NetworkConnectionSection.connection where ipallocationmode == none
   static Predicate<Network> networkWithNoIpAllocation = new Predicate<Network>() {

      @Override
      public boolean apply(Network input) {
         return "none".equals(input.getName());
      }

   };
   
   public void waitForTask(Task task) {
      if (!successTester.apply(task.getHref())) {
         throw new TaskStillRunningException(task);
      }
   }
   /**
    * Naming constraints modifying a VM on a VApp in vCloud Director (at least v1.5) can be more
    * strict than those in a vAppTemplate. For example, while it is possible to instantiate a
    * vAppTemplate with a VM named (incorrectly) {@code Ubuntu_10.04}, you must change the name to a
    * valid (alphanumeric underscore) name before you can update it.
    */
   public Task updateVmWithNameAndCustomizationScript(Vm vm, String name, @Nullable String customizationScript) {
      GuestCustomizationSection guestConfiguration = vm.getGuestCustomizationSection();
      guestConfiguration.setComputerName(name);
      if (customizationScript != null) {
         // In version 1.0.0, the api returns a script that loses newlines, so we cannot append to a
         // customization script.
         // TODO: parameterize whether to overwrite or append existing customization
         if (!buildVersion.startsWith("1.0.0") && !"".endsWith(buildVersion)
                  && guestConfiguration.getCustomizationScript() != null)
            customizationScript = guestConfiguration.getCustomizationScript() + "\n" + customizationScript;

         guestConfiguration.setCustomizationScript(customizationScript);
      }
      return client.getVmApi().updateGuestCustomizationOfVm(guestConfiguration, vm.getHref());
   }

   public void ensureVmHasDesiredNetworkConnectionSettings(VApp vApp, VCloudTemplateOptions vOptions) {
      Network networkToConnect = find(vApp.getNetworkSection().getNetworks(), not(networkWithNoIpAllocation));

      Vm vm = get(vApp.getChildren(), 0);

      NetworkConnectionSection nets = vm.getNetworkConnectionSection();
      checkArgument(nets.getConnections().size() > 0, "no connections on vm %s", vm);

      /*
       * Here we want to build the NetworkConnectionSection.
       * This is not required if:
       * - the user didn't pass in any vCloud specific NetworkConnection settings, and
       * - there exist enough NetworkConnectionSections to cover the networks we need to
       *   wire up to the VM
       *
       * In case of modifying the existing network connection, its important that
       * those optional parameters whose value is not being changed needs to be returned
       * with the existing values or vcloud puts default values for them. When mac address is not
       * modified then it needs to be returned else vclouds changes the adapter type of the NIC to E1000.
       *
       * There are a couple of things that might require changes:
       * - different parameters (e.g. ip address allocation mode)
       * - insufficient NetworkConnection items (we need to add more to trigger the
       *   creation of more NICs)
       * - if the user didn't pass in any vCloud specific NetworkConnections, we might need to
       *   create new ones here.
       * It's easier to just unconditionally rewrite the network connection section,
       * than to write some bug-prone code that checks all of the above conditions.
       */

      /*
       * We also need to add NICs. If we don't do this, we'll get new NICs, but they'll all be E1000s.
       * We really want to replicate the type of any existing adapters.
       *
       * We add the NICs only when it is needed. In case of vapp template having vm with multiple NICs
       * and if we update only the existing NICs then vcloud throws error that primary NIC not found.
       */
      Set<? extends ResourceAllocationSettingData> allVirtualHWItems = vm.getVirtualHardwareSection().getItems();
      Iterable<VCloudNetworkAdapter> existingNics = Iterables.filter(allVirtualHWItems, VCloudNetworkAdapter.class);
      // we want to program all existing nics.
      ArrayList<VCloudNetworkAdapter> nicsToProgram = Lists.newArrayList(existingNics);

      //the first adapter type will be used as default for newly added ones.
      String firstAdapterType = "E1000";
      int nextInstanceID = 1;
      if (nicsToProgram.size() >= 1) {
         firstAdapterType = nicsToProgram.get(0).getResourceSubType();
         nextInstanceID = 1 + Integer.valueOf(nicsToProgram.get(nicsToProgram.size() - 1).getInstanceID());
      }

      int i = 0;
      LinkedHashSet<NetworkConnection> connectionsToProgram = new LinkedHashSet<NetworkConnection>();
      for (String netUuid: vOptions.getNetworks()) {
         NetworkConnection desiredNC = vOptions.getNetworkConnections().get(netUuid);
         String netName;
         String macAddr;
         NetworkConnection vappNC = findNetworkConnectionByIndexOrNull(nets, i);
         if (vappNC != null && vappNC.getNetwork() != null) {
            netName = vappNC.getNetwork();
         } else {
            netName = null;
         }

         if (vappNC != null && vappNC.getMACAddress() != null) {
            macAddr = vappNC.getMACAddress();
         } else {
            macAddr = null;
         }

         if (desiredNC != null
             && desiredNC.getNetworkConnectionIndex() != i) {
            logger.error("Data consistency error: the network '" + netUuid + "'s connection index has been specified "
               + "in the vCloud specific NetworkConnection settings in VCloudTemplateOptions.networkConnections, but "
               + "the connection index does not match the "
               + "position of the network in the TemplateOptions.networks object. Ignoring vCloud specific options for this net.");
            desiredNC = null;
         }
         /*
              Its not yet clear why the mac address in desiredNC is null. This needs to be explored.
              This special handling is needed as the mac address is null and if it is not set, it results in NIC
              type as E1000
          */
         if (desiredNC != null && macAddr != null && desiredNC.getMACAddress() == null){
            NetworkConnection.Builder desiredNCBuilder = desiredNC.toBuilder();
            desiredNCBuilder.MACAddress(macAddr);
            desiredNC = desiredNCBuilder.build();
         }
         if (desiredNC != null && desiredNC.getIpAddressAllocationMode() == null
            || desiredNC.getIpAddressAllocationMode() == IpAddressAllocationMode.NONE) {
            logger.error("Data consistency error: the network '" + netUuid + "'s IP address allocation mode"
               + "has been set to 'none' or null in the vCloud specific NetworkConnection settings in VCloudTemplateOptions.networkConnections. "
               + "This is invalid. Ignoring vCloud specific options for this net.");
            desiredNC = null;
         }

          NetworkConnection ncToAdd = null;
          if (desiredNC == null) {
            // use default settings
            ncToAdd = new NetworkConnection(netName, i, null, null, true, null,
               IpAddressAllocationMode.POOL);
         } else {
            if (netName != null && !netName.equals(desiredNC.getNetwork())) {
               //something's probably wrong.
               logger.warn("vcloud overridden network name '" + desiredNC.getNetwork() + "' doesn't match the vApp's "
                  + " network with index " + i + " name '" + netName + "'");
            }

            if (netName == null && desiredNC.getNetwork() == null) {
               //ok we need to come up with some network name.
               netName = "jclouds-net-" + String.valueOf(i);
               NetworkConnection.Builder ncBuilder = desiredNC.toBuilder();
               ncToAdd = ncBuilder.network(netName).connected(desiredNC.isConnected())
                  .externalIpAddress(desiredNC.getExternalIpAddress())
                  .ipAddress(desiredNC.getIpAddress()).ipAddressAllocationMode(desiredNC.getIpAddressAllocationMode())
                  .MACAddress(desiredNC.getMACAddress()).networkConnectionIndex(desiredNC.getNetworkConnectionIndex()).build();
            } else {
               ncToAdd = desiredNC;
            }
         }
         connectionsToProgram.add(ncToAdd);

         //OK, we've now setup the network connection. Now we want to check if we need to add a new NIC for it.
         if (nicsToProgram.size() < connectionsToProgram.size()) {
            VCloudNetworkAdapter.Builder nicBuilder = VCloudNetworkAdapter.builder();
            //interesting values
            nicBuilder.addressOnParent(String.valueOf(i));
            nicBuilder.automaticAllocation(true);
            nicBuilder.connection(ncToAdd.getNetwork());
            nicBuilder.ipAddressingMode(ncToAdd.getIpAddressAllocationMode().toString());
            nicBuilder.elementName("Network adapter " + String.valueOf(i));
            nicBuilder.instanceID(String.valueOf(nextInstanceID));
            nextInstanceID += 1;
            nicBuilder.resourceSubType(firstAdapterType);
            nicBuilder.resourceType(ResourceAllocationSettingData.ResourceType.ETHERNET_ADAPTER);

            VCloudNetworkAdapter newNic = nicBuilder.build();
            nicsToProgram.add(newNic);
         }
         i++;
      }

      // Add new nics only if they are needed
      if (nicsToProgram.size() < connectionsToProgram.size()) {
         logger.debug("Programming NICs: %s", nicsToProgram);
         Task t = client.getVmApi().updateNetworkCardsOfVm(nicsToProgram, vm.getHref());
         waitForTask(t);
      }

      // update the NetworkConnectionSection.
      Builder builder = nets.toBuilder();
      builder.connections(connectionsToProgram);
      logger.trace(">> updating networkConnection vm(%s)", vm.getName());
      logger.debug("New NetworkConnectionSection for VM %s: %s", vm.getName(), builder.build().toString());
      waitForTask(client.getVmApi().updateNetworkConnectionOfVm(builder.build(), vm.getHref()));
      logger.trace("<< updated networkConnection vm(%s)", vm.getName());
   }

   private NetworkConnection findWithPoolAllocationOrFirst(NetworkConnectionSection net) {
      return find(net.getConnections(), new Predicate<NetworkConnection>() {

         @Override
         public boolean apply(NetworkConnection input) {
            return input.getIpAddressAllocationMode() == IpAddressAllocationMode.POOL;
         }

      }, get(net.getConnections(), 0));
   }

    private NetworkConnection findNetworkConnectionByIndexOrNull(NetworkConnectionSection net, final int index) {
        return find(net.getConnections(), new Predicate<NetworkConnection>() {

            @Override
            public boolean apply(NetworkConnection input) {
                return input.getNetworkConnectionIndex() == index;
            }

        }, null);
    }

   public Task updateCPUCountOfVm(Vm vm, int cpuCount) {
      return client.getVmApi().updateCPUCountOfVm(cpuCount, vm.getHref());
   }

   public Task updateMemoryMBOfVm(Vm vm, int memoryInMB) {
      return client.getVmApi().updateMemoryMBOfVm(memoryInMB, vm.getHref());
   }
}
