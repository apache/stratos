Why oauth & google-compute-engine forked?
=========================================

This code in Stratos is copied from Jclouds GCE [1]
The jclouds GCE code has 2 directories oauth & google-compute-engine
In Stratos, these two directories are mered into one.

[1] https://github.com/jclouds/jclouds-labs-google/tree/jclouds-labs-google-1.8.1

Custom changes
==============

1) rawDisk can be null for user created private images, hence changing the validation


diff --git a/home/rajkumar/workspace/apache/jclouds-labs/jclouds-labs-google-jclouds-labs-google-1.8.1/google-compute-engine/src/main/java/org
index 9ee7ea9..424aaa1 100644
--- a/home/rajkumar/workspace/apache/jclouds-labs/jclouds-labs-google-jclouds-labs-google-1.8.1/google-compute-engine/src/main/java/org/jcloud
+++ b/dependencies/jclouds/apis/gce/1.8.1-stratos/src/main/java/org/jclouds/googlecomputeengine/domain/Image.java
@@ -49,7 +49,8 @@ public final class Image extends Resource {
                    String sourceType, RawDisk rawDisk, Deprecated deprecated) {
       super(Kind.IMAGE, id, creationTimestamp, selfLink, name, description);
       this.sourceType = checkNotNull(sourceType, "sourceType of %s", name);
-      this.rawDisk = checkNotNull(rawDisk, "rawDisk of %s", name);
+      // rawDisk may be null for user created private images
+      this.rawDisk = rawDisk; // checkNotNull(rawDisk, "rawDisk of %s", name);
       this.deprecated = fromNullable(deprecated);
    }

2) merging google-compute-engine/pom.xml and oauth/pom.xml

diff --git a/home/rajkumar/workspace/apache/jclouds-labs/jclouds-labs-google-jclouds-labs-google-1.8.1/google-compute-engine/pom.xml b/dependencies/jclouds/apis/gce/1.8.1-stratos/pom.xml
index c1231f1..34f8bc7 100644
--- a/home/rajkumar/workspace/apache/jclouds-labs/jclouds-labs-google-jclouds-labs-google-1.8.1/google-compute-engine/pom.xml
+++ b/dependencies/jclouds/apis/gce/1.8.1-stratos/pom.xml
@@ -26,12 +26,15 @@
     </parent>
 
     <!-- TODO: when out of labs, switch to org.jclouds.provider -->
-    <groupId>org.apache.jclouds.labs</groupId>
-    <artifactId>google-compute-engine</artifactId>
+    <groupId>org.apache.stratos</groupId>
+    <artifactId>gce</artifactId>
+    <version>1.8.1-stratos</version>
     <name>jclouds Google Compute Engine provider</name>
     <description>jclouds components to access GoogleCompute</description>
+    <packaging>bundle</packaging>
 
     <properties>
+        <jclouds.version>1.8.1</jclouds.version>
         <test.google-compute-engine.identity>Email associated with the Google API client_id
         </test.google-compute-engine.identity>
         <test.google-compute-engine.credential>Private key (PKCS12 file) associated with the Google API client_id
@@ -39,6 +42,13 @@
         <test.google-compute-engine.api-version>v1</test.google-compute-engine.api-version>
         <test.google-compute-engine.build-version />
         <test.google-compute-engine.template>imageId=debian-7-wheezy-v20131120,locationId=us-central1-a,minRam=2048</test.google-compute-engine.template>
+        <jclouds.osgi.export>org.jclouds.googlecomputeengine*;version="${project.version}"</jclouds.osgi.export>
+        <jclouds.osgi.import>
+          org.jclouds.compute.internal;version="${jclouds.version}",
+          org.jclouds.rest.internal;version="${jclouds.version}",
+          org.jclouds*;version="${jclouds.version}",
+          *
+        </jclouds.osgi.import>
     </properties>
 
     <dependencies>
@@ -48,19 +58,6 @@
             <version>${jclouds.version}</version>
         </dependency>
         <dependency>
-            <groupId>org.apache.jclouds.labs</groupId>
-            <artifactId>oauth</artifactId>
-            <version>${project.version}</version>
-            <type>jar</type>
-        </dependency>
-        <dependency>
-            <groupId>org.apache.jclouds.labs</groupId>
-            <artifactId>oauth</artifactId>
-            <version>${project.version}</version>
-            <type>test-jar</type>
-            <scope>test</scope>
-        </dependency>
-        <dependency>
             <groupId>org.apache.jclouds</groupId>
             <artifactId>jclouds-compute</artifactId>
             <version>${jclouds.version}</version>
