Why aws-ec2 provider forked?
============================

Supporting associating public IP and updating api version from 2012-06-01 to 2014-02-01

Custom changes
==============

diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/pom.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/pom.xml
index 29a130a..ca0c749 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/pom.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/pom.xml
@@ -23,15 +23,16 @@
     <groupId>org.apache.jclouds</groupId>
     <artifactId>jclouds-project</artifactId>
     <version>1.8.1</version>
-    <relativePath>../../project/pom.xml</relativePath>
   </parent>
-  <groupId>org.apache.jclouds.provider</groupId>
+  <groupId>org.apache.stratos</groupId>
   <artifactId>aws-ec2</artifactId>
+  <version>1.8.1-stratos</version>
   <name>jclouds Amazon EC2 provider</name>
   <description>EC2 implementation targeted to Amazon Web Services</description>
   <packaging>bundle</packaging>
 
   <properties>
+    <jclouds.version>1.8.1</jclouds.version>
     <test.aws.identity>FIXME_IDENTITY</test.aws.identity>
     <test.aws.credential>FIXME_CREDENTIAL</test.aws.credential>
     <test.aws-ec2.endpoint>https://ec2.us-east-1.amazonaws.com</test.aws-ec2.endpoint>
@@ -47,11 +48,11 @@
     <test.aws-ec2.windows-template>hardwareId=m1.small,imageId=us-east-1/ami-0cb76d65</test.aws-ec2.windows-template>
     <jclouds.osgi.export>org.jclouds.aws.ec2*;version="${project.version}"</jclouds.osgi.export>
     <jclouds.osgi.import>
-      org.jclouds.compute.internal;version="${project.version}",
-      org.jclouds.rest.internal;version="${project.version}",
-      org.jclouds.aws;version="${project.version}",
-      org.jclouds.aws*;version="${project.version}",
-      org.jclouds*;version="${project.version}",
+      org.jclouds.compute.internal;version="${jclouds.version}",
+      org.jclouds.rest.internal;version="${jclouds.version}",
+      org.jclouds.aws;version="${jclouds.version}",
+      org.jclouds.aws*;version="${jclouds.version}",
+      org.jclouds*;version="${jclouds.version}",
       *
     </jclouds.osgi.import>
   </properties>
@@ -60,45 +61,45 @@
     <dependency>
       <groupId>org.apache.jclouds.api</groupId>
       <artifactId>ec2</artifactId>
-      <version>${project.version}</version>
+      <version>${jclouds.version}</version>
     </dependency>
     <dependency>
       <groupId>org.apache.jclouds.api</groupId>
       <artifactId>ec2</artifactId>
-      <version>${project.version}</version>
+      <version>${jclouds.version}</version>
       <type>test-jar</type>
       <scope>test</scope>
     </dependency>
     <dependency>
       <groupId>org.apache.jclouds.provider</groupId>
       <artifactId>aws-cloudwatch</artifactId>
-      <version>${project.version}</version>
+      <version>${jclouds.version}</version>
       <scope>test</scope>
     </dependency>
     <dependency>
       <groupId>org.apache.jclouds</groupId>
       <artifactId>jclouds-core</artifactId>
-      <version>${project.version}</version>
+      <version>${jclouds.version}</version>
       <type>test-jar</type>
       <scope>test</scope>
     </dependency>
     <dependency>
       <groupId>org.apache.jclouds</groupId>
       <artifactId>jclouds-compute</artifactId>
-      <version>${project.version}</version>
+      <version>${jclouds.version}</version>
       <type>test-jar</type>
       <scope>test</scope>
     </dependency>
     <dependency>
       <groupId>org.apache.jclouds.driver</groupId>
       <artifactId>jclouds-log4j</artifactId>
-      <version>${project.version}</version>
+      <version>${jclouds.version}</version>
       <scope>test</scope>
     </dependency>
     <dependency>
       <groupId>org.apache.jclouds.driver</groupId>
       <artifactId>jclouds-sshj</artifactId>
-      <version>${project.version}</version>
+      <version>${jclouds.version}</version>
       <scope>test</scope>
     </dependency>
   </dependencies>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/AWSEC2ApiMetadata.java b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/AWSEC2ApiMetadata.java
index 7cb4c83..e22c442 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/AWSEC2ApiMetadata.java
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/AWSEC2ApiMetadata.java
@@ -59,7 +59,7 @@ public final class AWSEC2ApiMetadata extends BaseHttpApiMetadata<AWSEC2Api> {
    public static final class Builder extends BaseHttpApiMetadata.Builder<AWSEC2Api, Builder> {
       public Builder() {
          id("aws-ec2")
-         .version("2012-06-01")
+         .version("2014-02-01")
          .name("Amazon-specific EC2 API")
          .identityName("Access Key ID")
          .credentialName("Secret Access Key")
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/binders/BindLaunchSpecificationToFormParams.java b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/binders/BindLaunchSpecificationToFormParams.java
index 6326a18..0029c86 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/binders/BindLaunchSpecificationToFormParams.java
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/binders/BindLaunchSpecificationToFormParams.java
@@ -61,8 +61,17 @@ public class BindLaunchSpecificationToFormParams implements Binder, Function<Lau
       if (launchSpec.getSecurityGroupIds().size() > 0)
          options.withSecurityGroupIds(launchSpec.getSecurityGroupIds());
       options.asType(checkNotNull(launchSpec.getInstanceType(), "instanceType"));
-      if (launchSpec.getSubnetId() != null)
-         options.withSubnetId(launchSpec.getSubnetId());
+       if (launchSpec.getSubnetId() != null){
+           if (Boolean.TRUE.equals(launchSpec.isPublicIpAddressAssociated()))  {
+               options.associatePublicIpAddressAndSubnetId(launchSpec.getSubnetId());
+               if (launchSpec.getSecurityGroupIds().size() > 0){
+                   options.withSecurityGroupIdsForNetworkInterface(launchSpec.getSecurityGroupIds());
+               }
+           }
+           else{
+               options.withSubnetId(launchSpec.getSubnetId());
+           }
+       }
       if (launchSpec.getKernelId() != null)
          options.withKernelId(launchSpec.getKernelId());
       if (launchSpec.getKeyName() != null)
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/compute/AWSEC2TemplateOptions.java b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/compute/AWSEC2TemplateOptions.java
index 1e75329..84c8568 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/compute/AWSEC2TemplateOptions.java
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/compute/AWSEC2TemplateOptions.java
@@ -75,6 +75,8 @@ public class AWSEC2TemplateOptions extends EC2TemplateOptions implements Cloneab
             eTo.iamInstanceProfileName(getIAMInstanceProfileName());
          if (isMonitoringEnabled())
             eTo.enableMonitoring();
+      	 if (isPublicIpAddressAssociated())
+            eTo.associatePublicIpAddress();
          if (!shouldAutomaticallyCreatePlacementGroup())
             eTo.noPlacementGroup();
          if (getPlacementGroup() != null)
@@ -89,6 +91,7 @@ public class AWSEC2TemplateOptions extends EC2TemplateOptions implements Cloneab
    }
 
    private boolean monitoringEnabled;
+   private boolean publicIpAddressAssociated;
    private String placementGroup = null;
    private boolean noPlacementGroup;
    private String subnetId;
@@ -106,7 +109,8 @@ public class AWSEC2TemplateOptions extends EC2TemplateOptions implements Cloneab
          return false;
       AWSEC2TemplateOptions that = AWSEC2TemplateOptions.class.cast(o);
       return super.equals(that) && equal(this.monitoringEnabled, that.monitoringEnabled)
-               && equal(this.placementGroup, that.placementGroup)
+			   && equal(this.publicIpAddressAssociated, that.publicIpAddressAssociated)               
+			   && equal(this.placementGroup, that.placementGroup)
                && equal(this.noPlacementGroup, that.noPlacementGroup) && equal(this.subnetId, that.subnetId)
                && equal(this.spotPrice, that.spotPrice) && equal(this.spotOptions, that.spotOptions)
                && equal(this.groupIds, that.groupIds) && equal(this.iamInstanceProfileArn, that.iamInstanceProfileArn)
@@ -115,7 +119,7 @@ public class AWSEC2TemplateOptions extends EC2TemplateOptions implements Cloneab
 
    @Override
    public int hashCode() {
-      return Objects.hashCode(super.hashCode(), monitoringEnabled, placementGroup, noPlacementGroup, subnetId,
+      return Objects.hashCode(super.hashCode(), monitoringEnabled, publicIpAddressAssociated, placementGroup, noPlacementGroup, subnetId,
                spotPrice, spotOptions, groupIds, iamInstanceProfileArn, iamInstanceProfileName);
    }
 
@@ -124,6 +128,8 @@ public class AWSEC2TemplateOptions extends EC2TemplateOptions implements Cloneab
       ToStringHelper toString = super.string();
       if (monitoringEnabled)
          toString.add("monitoringEnabled", monitoringEnabled);
+	  if (publicIpAddressAssociated)
+         toString.add("publicIpAddressAssociated", publicIpAddressAssociated);
       toString.add("placementGroup", placementGroup);
       if (noPlacementGroup)
          toString.add("noPlacementGroup", noPlacementGroup);
@@ -151,6 +157,15 @@ public class AWSEC2TemplateOptions extends EC2TemplateOptions implements Cloneab
    }
 
    /**
+    * Associate a public Ip Address
+    *
+    */
+   public AWSEC2TemplateOptions associatePublicIpAddress() {
+      this.publicIpAddressAssociated = true;
+      return this;
+   }
+   
+	/**
     * Specifies the keypair used to run instances with
     */
    public AWSEC2TemplateOptions placementGroup(String placementGroup) {
@@ -383,6 +398,14 @@ public class AWSEC2TemplateOptions extends EC2TemplateOptions implements Cloneab
          return options.enableMonitoring();
       }
 
+
+      /**
+       * @see AWSEC2TemplateOptions#associatePublicIpAddress
+       */
+      public static AWSEC2TemplateOptions associatePublicIpAddress() {
+         AWSEC2TemplateOptions options = new AWSEC2TemplateOptions();
+         return options.associatePublicIpAddress();
+      }
       // methods that only facilitate returning the correct object type
       /**
        * @see TemplateOptions#inboundPorts
@@ -748,7 +771,12 @@ public class AWSEC2TemplateOptions extends EC2TemplateOptions implements Cloneab
       return monitoringEnabled;
    }
 
-   /**
+    /**
+     * @return true (default is false) if we are supposed to associate a public ip address
+     */
+   public boolean isPublicIpAddressAssociated() { return publicIpAddressAssociated; }
+   
+	/**
     * @return subnetId to use when running the instance or null.
     */
    public String getSubnetId() {
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/compute/strategy/AWSEC2CreateNodesInGroupThenAddToSet.java b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/compute/strategy/AWSEC2CreateNodesInGroupThenAddToSet.java
index dee0268..a46958c 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/compute/strategy/AWSEC2CreateNodesInGroupThenAddToSet.java
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/compute/strategy/AWSEC2CreateNodesInGroupThenAddToSet.java
@@ -93,7 +93,8 @@ public class AWSEC2CreateNodesInGroupThenAddToSet extends EC2CreateNodesInGroupT
          AWSEC2TemplateOptions awsOptions = AWSEC2TemplateOptions.class.cast(template.getOptions());
          LaunchSpecification spec = AWSRunInstancesOptions.class.cast(instanceOptions).getLaunchSpecificationBuilder()
                .imageId(template.getImage().getProviderId()).availabilityZone(zone).subnetId(awsOptions.getSubnetId())
-               .iamInstanceProfileArn(awsOptions.getIAMInstanceProfileArn())
+				.publicIpAddressAssociated(awsOptions.isPublicIpAddressAssociated())               
+				.iamInstanceProfileArn(awsOptions.getIAMInstanceProfileArn())
                .iamInstanceProfileName(awsOptions.getIAMInstanceProfileName()).build();
          RequestSpotInstancesOptions options = awsOptions.getSpotOptions();
          if (logger.isDebugEnabled())
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/compute/strategy/CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.java b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/compute/strategy/CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.java
index 78b9573..096d959 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/compute/strategy/CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.java
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/compute/strategy/CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions.java
@@ -177,10 +177,21 @@ public class CreateKeyPairPlacementAndSecurityGroupsAsNeededAndReturnRunOptions
       if (awsTemplateOptions.getGroupIds().size() > 0)
          awsInstanceOptions.withSecurityGroupIds(awsTemplateOptions.getGroupIds());
       String subnetId = awsTemplateOptions.getSubnetId();
+      boolean associatePublicIpAddress = awsTemplateOptions.isPublicIpAddressAssociated();
       if (subnetId != null) {
-         AWSRunInstancesOptions.class.cast(instanceOptions).withSubnetId(subnetId);
+          if(associatePublicIpAddress){
+              AWSRunInstancesOptions.class.cast(instanceOptions).associatePublicIpAddressAndSubnetId(subnetId);
+              if (awsTemplateOptions.getGroupIds().size() > 0)
+                  awsInstanceOptions.withSecurityGroupIdsForNetworkInterface(awsTemplateOptions.getGroupIds());
+          }else{
+              AWSRunInstancesOptions.class.cast(instanceOptions).withSubnetId(subnetId);
+              if (awsTemplateOptions.getGroupIds().size() > 0)
+                  awsInstanceOptions.withSecurityGroupIds(awsTemplateOptions.getGroupIds());
+          }
       } else {
-         super.addSecurityGroups(region, group, template, instanceOptions);
+          if (awsTemplateOptions.getGroupIds().size() > 0)
+              awsInstanceOptions.withSecurityGroupIds(awsTemplateOptions.getGroupIds());
+          super.addSecurityGroups(region, group, template, instanceOptions);
       }
    }
 }
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/domain/AWSRunningInstance.java b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/domain/AWSRunningInstance.java
index f3ffc0b..449cc0e 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/domain/AWSRunningInstance.java
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/domain/AWSRunningInstance.java
@@ -63,6 +63,7 @@ public class AWSRunningInstance extends RunningInstance {
       private Set<String> productCodes = Sets.newLinkedHashSet();
       private String subnetId;
       private String spotInstanceRequestId;
+	  private boolean associatedPublicIpAddress;
       private String vpcId;
       private Hypervisor hypervisor;
       private Map<String, String> securityGroupIdToNames = Maps.newLinkedHashMap();
@@ -107,6 +108,10 @@ public class AWSRunningInstance extends RunningInstance {
          return this;
       }
 
+      public Builder associatedPublicIpAddress(boolean associatedPublicIpAddress) {
+         this.associatedPublicIpAddress = associatedPublicIpAddress;
+         return this;
+      }
       public Builder spotInstanceRequestId(String spotInstanceRequestId) {
          this.spotInstanceRequestId = spotInstanceRequestId;
          return this;
@@ -149,7 +154,7 @@ public class AWSRunningInstance extends RunningInstance {
                instanceState, rawState, instanceType, ipAddress, kernelId, keyName, launchTime, availabilityZone,
                virtualizationType, platform, privateDnsName, privateIpAddress, ramdiskId, reason, rootDeviceType,
                rootDeviceName, ebsBlockDevices, monitoringState, placementGroup, productCodes, subnetId,
-               spotInstanceRequestId, vpcId, hypervisor, tags, iamInstanceProfile);
+               spotInstanceRequestId, vpcId, hypervisor, tags, iamInstanceProfile, associatedPublicIpAddress);
       }
       
       @Override
@@ -158,7 +163,7 @@ public class AWSRunningInstance extends RunningInstance {
          if (in instanceof AWSRunningInstance) {
             AWSRunningInstance awsIn = AWSRunningInstance.class.cast(in);
             monitoringState(awsIn.monitoringState).placementGroup(awsIn.placementGroup)
-                  .productCodes(awsIn.productCodes).subnetId(awsIn.subnetId)
+                  .productCodes(awsIn.productCodes).subnetId(awsIn.subnetId).associatedPublicIpAddress(awsIn.associatedPublicIpAddress)
                   .spotInstanceRequestId(awsIn.spotInstanceRequestId).vpcId(awsIn.vpcId).hypervisor(awsIn.hypervisor)
                   .securityGroupIdToNames(awsIn.securityGroupIdToNames);
             if (awsIn.getIAMInstanceProfile().isPresent()) {
@@ -182,6 +187,7 @@ public class AWSRunningInstance extends RunningInstance {
    private final Set<String> productCodes;
    @Nullable
    private final String subnetId;
+   private final boolean associatedPublicIpAddress;
    @Nullable
    private final String spotInstanceRequestId;
    @Nullable
@@ -197,11 +203,12 @@ public class AWSRunningInstance extends RunningInstance {
             String privateIpAddress, String ramdiskId, String reason, RootDeviceType rootDeviceType,
             String rootDeviceName, Map<String, BlockDevice> ebsBlockDevices, MonitoringState monitoringState,
             String placementGroup, Iterable<String> productCodes, String subnetId, String spotInstanceRequestId,
-            String vpcId, Hypervisor hypervisor, Map<String, String> tags, Optional<IAMInstanceProfile> iamInstanceProfile) {
+            String vpcId, Hypervisor hypervisor, Map<String, String> tags, Optional<IAMInstanceProfile> iamInstanceProfile, boolean associatedPublicIpAddress) {
       super(region, securityGroupIdToNames.values(), amiLaunchIndex, dnsName, imageId, instanceId, instanceState,
                rawState, instanceType, ipAddress, kernelId, keyName, launchTime, availabilityZone, virtualizationType,
                platform, privateDnsName, privateIpAddress, ramdiskId, reason, rootDeviceType, rootDeviceName,
                ebsBlockDevices, tags);
+	  this.associatedPublicIpAddress = associatedPublicIpAddress;
       this.monitoringState = checkNotNull(monitoringState, "monitoringState");
       this.placementGroup = placementGroup;
       this.productCodes = ImmutableSet.copyOf(checkNotNull(productCodes, "productCodes"));
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/domain/LaunchSpecification.java b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/domain/LaunchSpecification.java
index 2972dd5..51a6012 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/domain/LaunchSpecification.java
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/domain/LaunchSpecification.java
@@ -59,6 +59,7 @@ public class LaunchSpecification {
       protected String subnetId;
       protected String ramdiskId;
       protected Boolean monitoringEnabled;
+      protected Boolean publicIpAddressAssociated;
       protected ImmutableSet.Builder<BlockDeviceMapping> blockDeviceMappings = ImmutableSet
             .builder();
       protected ImmutableSet.Builder<String> securityGroupIds = ImmutableSet.builder();
@@ -77,6 +78,7 @@ public class LaunchSpecification {
          subnetId = null;
          ramdiskId = null;
          monitoringEnabled = false;
+		 publicIpAddressAssociated = false;
          blockDeviceMappings = ImmutableSet.builder();
          securityGroupIds = ImmutableSet.builder();
          securityGroupNames = ImmutableSet.builder();
@@ -106,6 +108,11 @@ public class LaunchSpecification {
          return this;
       }
 
+      public Builder publicIpAddressAssociated(Boolean publicIpAddressAssociated) {
+         this.publicIpAddressAssociated = publicIpAddressAssociated;
+         return this;
+      }
+
       public Builder instanceType(String instanceType) {
          this.instanceType = instanceType;
          return this;
@@ -232,7 +239,7 @@ public class LaunchSpecification {
             iamInstanceProfile = Optional.absent();
          }
          return new LaunchSpecification(instanceType, imageId, kernelId, ramdiskId, availabilityZone, subnetId,
-               keyName, securityGroupIdToNames.build(), blockDeviceMappings.build(), monitoringEnabled,
+               keyName, securityGroupIdToNames.build(), blockDeviceMappings.build(), monitoringEnabled, publicIpAddressAssociated,
                securityGroupIds.build(), securityGroupNames.build(), userData, iamInstanceProfile);
       }
 
@@ -243,7 +250,8 @@ public class LaunchSpecification {
                .keyName(in.getKeyName()).securityGroupIdToNames(in.getSecurityGroupIdToNames())
                .securityGroupIds(in.getSecurityGroupIds()).securityGroupNames(in.getSecurityGroupNames())
                .blockDeviceMappings(in.getBlockDeviceMappings()).monitoringEnabled(in.isMonitoringEnabled())
-               .userData(in.getUserData());
+			   .publicIpAddressAssociated(in.publicIpAddressAssociated).userData(in.getUserData())               
+			   .userData(in.getUserData());
          if (in.getIAMInstanceProfile().isPresent()) {
             builder.iamInstanceProfileArn(in.getIAMInstanceProfile().get().getArn().orNull());
             builder.iamInstanceProfileName(in.getIAMInstanceProfile().get().getName().orNull());
@@ -264,14 +272,16 @@ public class LaunchSpecification {
    protected final Set<String> securityGroupIds;
    protected final Set<String> securityGroupNames;
    protected final Boolean monitoringEnabled;
+   protected final Boolean publicIpAddressAssociated;
    protected final byte[] userData;
    protected final Optional<IAMInstanceProfileRequest> iamInstanceProfile;
 
    public LaunchSpecification(String instanceType, String imageId, String kernelId, String ramdiskId,
          String availabilityZone, String subnetId, String keyName, Map<String, String> securityGroupIdToNames,
-         Iterable<? extends BlockDeviceMapping> blockDeviceMappings, Boolean monitoringEnabled,
+         Iterable<? extends BlockDeviceMapping> blockDeviceMappings, Boolean monitoringEnabled, Boolean publicIpAddressAssociated,
          Set<String> securityGroupIds, Set<String> securityGroupNames, byte[] userData,
          Optional<IAMInstanceProfileRequest> iamInstanceProfile) {
+	  this.publicIpAddressAssociated = publicIpAddressAssociated;
       this.instanceType = checkNotNull(instanceType, "instanceType");
       this.imageId = checkNotNull(imageId, "imageId");
       this.kernelId = kernelId;
@@ -307,6 +317,11 @@ public class LaunchSpecification {
    }
 
    /**
+    * Public ip address associated
+    */
+   public Boolean isPublicIpAddressAssociated() { return publicIpAddressAssociated; }
+
+   /**
     * The instance type.
     */
    public String getInstanceType() {
@@ -456,6 +471,11 @@ public class LaunchSpecification {
             return false;
       } else if (!monitoringEnabled.equals(other.monitoringEnabled))
          return false;
+      if (publicIpAddressAssociated == null) {
+         if (other.publicIpAddressAssociated != null)
+            return false;
+      } else if (!publicIpAddressAssociated.equals(other.publicIpAddressAssociated))
+           return false;
       if (ramdiskId == null) {
          if (other.ramdiskId != null)
             return false;
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/options/AWSRunInstancesOptions.java b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/options/AWSRunInstancesOptions.java
index 1ae0b47..258977f 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/options/AWSRunInstancesOptions.java
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/options/AWSRunInstancesOptions.java
@@ -70,6 +70,17 @@ public class AWSRunInstancesOptions extends RunInstancesOptions {
    }
 
    /**
+    * Associate public ip for the instance
+    */
+   public AWSRunInstancesOptions associatePublicIpAddressAndSubnetId(String subnetId) {
+      formParameters.put("NetworkInterface.0.DeviceIndex", "0");
+      formParameters.put("NetworkInterface.0.AssociatePublicIpAddress", "true");
+      formParameters.put("NetworkInterface.0.SubnetId", checkNotNull(subnetId, "subnetId"));
+      launchSpecificationBuilder.publicIpAddressAssociated(true);
+      return this;
+   }
+
+   /**
     * Specifies the subnet ID within which to launch the instance(s) for Amazon Virtual Private
     * Cloud.
     */
@@ -88,10 +99,20 @@ public class AWSRunInstancesOptions extends RunInstancesOptions {
       return this;
    }
 
+   public AWSRunInstancesOptions withSecurityGroupIdsForNetworkInterface(Iterable<String> securityGroupIds) {
+      launchSpecificationBuilder.securityGroupIds(securityGroupIds);
+      indexFormValuesWithPrefix("NetworkInterface.0.SecurityGroupId", securityGroupIds);
+      return this;
+   }
+
    public AWSRunInstancesOptions withSecurityGroupIds(String... securityGroupIds) {
       return withSecurityGroupIds(ImmutableSet.copyOf(securityGroupIds));
    }
 
+   public AWSRunInstancesOptions withSecurityGroupIdsForNetworkInterface(String... securityGroupIds) {
+      return withSecurityGroupIdsForNetworkInterface(ImmutableSet.copyOf(securityGroupIds));
+   }
+
    /**
     * Amazon resource name (ARN) of the IAM Instance Profile (IIP) to associate with the instances.
     * 
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/xml/LaunchSpecificationHandler.java b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/xml/LaunchSpecificationHandler.java
index 68d37d5..11ada98 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/main/java/org/jclouds/aws/ec2/xml/LaunchSpecificationHandler.java
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/main/java/org/jclouds/aws/ec2/xml/LaunchSpecificationHandler.java
@@ -123,6 +123,10 @@ public class LaunchSpecificationHandler extends HandlerForGeneratedRequestWithRe
          String monitoringEnabled = currentOrNull();
          if (monitoringEnabled != null)
             builder.monitoringEnabled(Boolean.valueOf(monitoringEnabled));
+      } else if (qName.equals("publicIpAddressAssociated")) {
+         String publicIpAddressAssociated = currentOrNull();
+         if (publicIpAddressAssociated != null)
+            builder.publicIpAddressAssociated(Boolean.valueOf(publicIpAddressAssociated));
       }
       currentText = new StringBuilder();
    }
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/java/org/jclouds/aws/ec2/features/PlacementGroupApiExpectTest.java b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/java/org/jclouds/aws/ec2/features/PlacementGroupApiExpectTest.java
index 77d51de..02c0d51 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/java/org/jclouds/aws/ec2/features/PlacementGroupApiExpectTest.java
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/java/org/jclouds/aws/ec2/features/PlacementGroupApiExpectTest.java
@@ -38,11 +38,11 @@ public class PlacementGroupApiExpectTest extends BaseAWSEC2ComputeServiceExpectT
            .addFormParam("Action", "DescribePlacementGroups")
            .addFormParam("Filter.1.Name", "strategy")
            .addFormParam("Filter.1.Value.1", "cluster")
-           .addFormParam("Signature", "SaA7Un1BE3m9jIEKyjXNdQPzFh/QAJSCebvKXiwUEK0%3D")
+           .addFormParam("Signature", "mAMdRgaHRw8LAAF2hzTC79yNHmuOwH7S8D%2BiTDi30nU%3D")
            .addFormParam("SignatureMethod", "HmacSHA256")
            .addFormParam("SignatureVersion", "2")
            .addFormParam("Timestamp", "2012-04-16T15%3A54%3A08.897Z")
-           .addFormParam("Version", "2012-06-01")
+           .addFormParam("Version", "2014-02-01")
            .addFormParam("AWSAccessKeyId", "identity").build();
 
    public void testFilterWhenResponseIs2xx() {
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/java/org/jclouds/aws/ec2/features/SpotInstanceApiExpectTest.java b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/java/org/jclouds/aws/ec2/features/SpotInstanceApiExpectTest.java
index 7460505..2a41c4e 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/java/org/jclouds/aws/ec2/features/SpotInstanceApiExpectTest.java
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/java/org/jclouds/aws/ec2/features/SpotInstanceApiExpectTest.java
@@ -38,11 +38,11 @@ public class SpotInstanceApiExpectTest extends BaseAWSEC2ComputeServiceExpectTes
            .addFormParam("Action", "DescribeSpotInstanceRequests")
            .addFormParam("Filter.1.Name", "instance-id")
            .addFormParam("Filter.1.Value.1", "i-ef308e8e")
-           .addFormParam("Signature", "wQtGpumMCDEzvlldKepCKeEjD9iE7eAyiRBlQztcJMA%3D")
+           .addFormParam("Signature", "M4wqA0OGm%2BNgKatZdB80udvU3gsTFKTGyvNA7Qf9isg%3D")
            .addFormParam("SignatureMethod", "HmacSHA256")
            .addFormParam("SignatureVersion", "2")
            .addFormParam("Timestamp", "2012-04-16T15%3A54%3A08.897Z")
-           .addFormParam("Version", "2012-06-01")
+           .addFormParam("Version", "2014-02-01")
            .addFormParam("AWSAccessKeyId", "identity").build();
 
    public void testFilterWhenResponseIs2xx() {
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_instances_1.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_instances_1.xml
index 5d6bf68..889c30f 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_instances_1.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_instances_1.xml
@@ -1,4 +1,4 @@
-<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>b3e1c7ee-1f34-4582-9493-695c9425c679</requestId>
     <reservationSet>
         <item>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_instances_2.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_instances_2.xml
index 380a5bf..38a2504 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_instances_2.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_instances_2.xml
@@ -1,4 +1,4 @@
-<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>b2238f71-750f-4eed-8f5c-eb4e6f66b687</requestId>
     <reservationSet>
         <item>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_instances_3.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_instances_3.xml
index 1e15ce9..b03ea7d 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_instances_3.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_instances_3.xml
@@ -1,5 +1,5 @@
 <?xml version="1.0" encoding="UTF-8"?>
-<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>440faed2-0331-488d-a04d-d8c9aba85307</requestId>
     <reservationSet/>
 </DescribeInstancesResponse>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_instances_latest.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_instances_latest.xml
index 9d606b8..1664ac9 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_instances_latest.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_instances_latest.xml
@@ -1,5 +1,5 @@
 <?xml version="1.0" encoding="UTF-8"?>
-<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>a03c1896-0543-485f-a732-ebc83873a3ca</requestId>
     <reservationSet>
         <item>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_instances_pending.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_instances_pending.xml
index 0013106..2793e54 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_instances_pending.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_instances_pending.xml
@@ -1,5 +1,5 @@
 <?xml version="1.0" encoding="UTF-8"?>
-<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>dcd37ecf-e5b6-462b-99a8-112427b3e3a2</requestId>
     <reservationSet>
         <item>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_spot_instance.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_spot_instance.xml
index 964b246..084bcca 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_spot_instance.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_spot_instance.xml
@@ -1,5 +1,5 @@
 <?xml version="1.0" encoding="UTF-8"?>
-<DescribeSpotInstanceRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<DescribeSpotInstanceRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>d9da716a-5cd4-492e-83b9-6777ac16d6cf</requestId>
     <spotInstanceRequestSet>
         <item>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_spot_instance_requests.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_spot_instance_requests.xml
index e6598ee..7e4dea8 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_spot_instance_requests.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_spot_instance_requests.xml
@@ -1,4 +1,4 @@
-<DescribeSpotInstanceRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<DescribeSpotInstanceRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>7c4dd2bd-106d-4cd3-987c-35ee819180a6</requestId>
     <spotInstanceRequestSet>
         <item>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_spot_instance_tags.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_spot_instance_tags.xml
index 2b1a1a1..75930bc 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_spot_instance_tags.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_spot_instance_tags.xml
@@ -1,5 +1,5 @@
 <?xml version="1.0" encoding="UTF-8"?>
-<DescribeSpotInstanceRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<DescribeSpotInstanceRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>f2247378-7df0-4725-b55f-8ef58b557dcd</requestId>
     <spotInstanceRequestSet>
         <item>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_spot_instances_1.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_spot_instances_1.xml
index 5cb54ba..8791115 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_spot_instances_1.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_spot_instances_1.xml
@@ -1,5 +1,5 @@
 <?xml version="1.0" encoding="UTF-8"?>
-<DescribeSpotInstanceRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<DescribeSpotInstanceRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>f2247378-7df0-4725-b55f-8ef58b557dcd</requestId>
     <spotInstanceRequestSet>
         <item>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_spot_price_history.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_spot_price_history.xml
index c25a536..e3a37ba 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/describe_spot_price_history.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/describe_spot_price_history.xml
@@ -1,5 +1,5 @@
 <?xml version="1.0" encoding="UTF-8"?>
-<DescribeSpotPriceHistoryResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<DescribeSpotPriceHistoryResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>99777a75-2a2b-4296-a305-650c442d2d63</requestId>
     <spotPriceHistorySet>
         <item>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/request_spot_instances-ebs.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/request_spot_instances-ebs.xml
index 7f64608..b8ae440 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/request_spot_instances-ebs.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/request_spot_instances-ebs.xml
@@ -1,5 +1,5 @@
 <?xml version="1.0" encoding="UTF-8"?>
-<RequestSpotInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<RequestSpotInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>02401e8e-a4f5-4285-8ea8-6d742fbaadd8</requestId>
     <spotInstanceRequestSet>
         <item>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/request_spot_instances.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/request_spot_instances.xml
index deca9e5..977168a 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/request_spot_instances.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/request_spot_instances.xml
@@ -1,5 +1,5 @@
 <?xml version="1.0" encoding="UTF-8"?>
-<RequestSpotInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<RequestSpotInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>2ffc645f-6835-4d23-bd18-f6f53c253067</requestId>
     <spotInstanceRequestSet>
         <item>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/run_instances_1.xml b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/run_instances_1.xml
index 50ff562..0ea2ece 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/providers/aws-ec2/src/test/resources/run_instances_1.xml
+++ b/dependencies/jclouds/provider/aws-ec2/1.8.1-stratos/src/test/resources/run_instances_1.xml
@@ -1,5 +1,5 @@
 <?xml version="1.0" encoding="UTF-8"?>
-<RunInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2012-06-01/">
+<RunInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2014-02-01/">
     <requestId>7faf9500-67ef-484b-9fa5-73b6df638bc8</requestId>
     <reservationId>r-d3b815bc</reservationId>
     <ownerId>993194456877</ownerId>
