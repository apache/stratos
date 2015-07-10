Why vcloud api forked?
=====================

We have implemented support for vCloud 1.5 in jclouds

Custom changes
==============

diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/pom.xml b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/pom.xml
index 4eb34f9..8357aec 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/pom.xml
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/pom.xml
@@ -23,15 +23,16 @@
     <groupId>org.apache.jclouds</groupId>
     <artifactId>jclouds-project</artifactId>
     <version>1.8.1</version>
-    <relativePath>../../project/pom.xml</relativePath>
   </parent>
-  <groupId>org.apache.jclouds.api</groupId>
+  <groupId>org.apache.stratos</groupId>
   <artifactId>vcloud</artifactId>
+  <version>1.8.1-stratos</version>
   <name>jclouds vcloud api</name>
   <description>jclouds components to access an implementation of VMWare vCloud</description>
   <packaging>bundle</packaging>
 
   <properties>
+    <jclouds.version>1.8.1</jclouds.version>
     <test.vcloud.endpoint>FIXME_ENDPOINT</test.vcloud.endpoint>
     <test.vcloud.api-version>1.0</test.vcloud.api-version>
     <test.vcloud.build-version />
@@ -40,9 +41,9 @@
     <test.vcloud.template />
     <jclouds.osgi.export>org.jclouds.vcloud*;version="${project.version}"</jclouds.osgi.export>
     <jclouds.osgi.import>
-      org.jclouds.compute.internal;version="${project.version}",
-      org.jclouds.rest.internal;version="${project.version}",
-      org.jclouds*;version="${project.version}",
+      org.jclouds.compute.internal;version="${jclouds.version}",
+      org.jclouds.rest.internal;version="${jclouds.version}",
+      org.jclouds*;version="${jclouds.version}",
       *
     </jclouds.osgi.import>
   </properties>
@@ -56,37 +57,37 @@
     <dependency>
       <groupId>org.apache.jclouds</groupId>
       <artifactId>jclouds-core</artifactId>
-      <version>${project.version}</version>
+      <version>${jclouds.version}</version>
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
       <artifactId>jclouds-sshj</artifactId>
-      <version>${project.version}</version>
+      <version>${jclouds.version}</version>
       <scope>test</scope>
     </dependency>
     <dependency>
       <groupId>org.apache.jclouds.driver</groupId>
       <artifactId>jclouds-log4j</artifactId>
-      <version>${project.version}</version>
+      <version>${jclouds.version}</version>
       <scope>test</scope>
     </dependency>
   </dependencies>
@@ -124,5 +125,7 @@
     </profile>
   </profiles>
 
-
+  <scm>
+    <tag>4.1.0-rc4</tag>
+  </scm>
 </project>
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/VCloudMediaType.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/VCloudMediaType.java
index f698b3e..67701fb 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/VCloudMediaType.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/VCloudMediaType.java
@@ -209,4 +209,14 @@ public interface VCloudMediaType {
     */
    public static final MediaType RASDITEM_XML_TYPE = new MediaType("application", "vnd.vmware.vcloud.rasdItem+xml");
 
+   /**
+    * "application/vnd.vmware.vcloud.session+xml"
+    */
+   public static final String SESSION_XML = "application/vnd.vmware.vcloud.session+xml";
+
+   /**
+    * "application/vnd.vmware.vcloud.session+xml"
+    */
+   public static final MediaType SESSION_XML_TYPE = new MediaType("application", "vnd.vmware.vcloud.session+xml");
+
 }
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/VCloudVersionsApi.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/VCloudVersionsApi.java
index 01ec045..b6c46f9 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/VCloudVersionsApi.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/VCloudVersionsApi.java
@@ -23,9 +23,12 @@ import java.util.SortedMap;
 import javax.ws.rs.GET;
 import javax.ws.rs.Path;
 
+import org.jclouds.rest.annotations.RequestFilters;
 import org.jclouds.rest.annotations.XMLResponseParser;
+import org.jclouds.vcloud.http.filters.VCloudSupportedVersions;
 import org.jclouds.vcloud.xml.SupportedVersionsHandler;
 
+@RequestFilters(VCloudSupportedVersions.class)
 public interface VCloudVersionsApi extends Closeable {
 
    @GET
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/domain/ReferenceType.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/domain/ReferenceType.java
index 108c139..d36343d 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/domain/ReferenceType.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/domain/ReferenceType.java
@@ -45,4 +45,8 @@ public interface ReferenceType extends Comparable<ReferenceType> {
     */
    String getType();
 
+   /**
+    * @return relationship to the referenced object.
+    */
+   String getRelationship();
 }
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/domain/internal/CatalogImpl.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/domain/internal/CatalogImpl.java
index 37dfb10..03d1532 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/domain/internal/CatalogImpl.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/domain/internal/CatalogImpl.java
@@ -182,4 +182,8 @@ public class CatalogImpl extends LinkedHashMap<String, ReferenceType> implements
       return (this == o) ? 0 : getHref().compareTo(o.getHref());
    }
 
+   @Override
+   public String getRelationship() {
+      throw new UnsupportedOperationException();
+   }
 }
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/domain/internal/ReferenceTypeImpl.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/domain/internal/ReferenceTypeImpl.java
index 4519eba..fa128f9 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/domain/internal/ReferenceTypeImpl.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/domain/internal/ReferenceTypeImpl.java
@@ -32,11 +32,20 @@ public class ReferenceTypeImpl implements ReferenceType {
    private final String name;
    private final String type;
    private final URI href;
+   private final String relationship;
 
    public ReferenceTypeImpl(String name, String type, URI href) {
       this.name = name;
       this.type = type;
       this.href = href;
+	  this.relationship = null;
+   }
+
+   public ReferenceTypeImpl(String name, String type, URI href, String relationship) {
+      this.name = name;
+      this.type = type;
+      this.href = href;
+      this.relationship = relationship;
    }
 
    @Override
@@ -80,6 +89,11 @@ public class ReferenceTypeImpl implements ReferenceType {
    }
 
    protected ToStringHelper string() {
-      return Objects.toStringHelper("").omitNullValues().add("href", href).add("name", name).add("type", type);
+      return Objects.toStringHelper("").omitNullValues().add("href", href).add("name", name).add("type", type).add("relationship", relationship);
+   }
+
+   @Override
+   public String getRelationship() {
+      return relationship;
    }
 }
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/filters/AddVCloudAuthorizationAndCookieToRequest.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/filters/AddVCloudAuthorizationAndCookieToRequest.java
index 9d2953f..1d3ba5d 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/filters/AddVCloudAuthorizationAndCookieToRequest.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/filters/AddVCloudAuthorizationAndCookieToRequest.java
@@ -22,6 +22,7 @@ import javax.inject.Singleton;
 import org.jclouds.http.HttpException;
 import org.jclouds.http.HttpRequest;
 import org.jclouds.http.HttpRequestFilter;
+import org.jclouds.rest.annotations.ApiVersion;
 import org.jclouds.vcloud.VCloudToken;
 
 import com.google.common.base.Supplier;
@@ -34,19 +35,26 @@ import com.google.common.net.HttpHeaders;
 @Singleton
 public class AddVCloudAuthorizationAndCookieToRequest implements HttpRequestFilter {
    private Supplier<String> vcloudTokenProvider;
+   private final String apiVersion;
 
    @Inject
-   public AddVCloudAuthorizationAndCookieToRequest(@VCloudToken Supplier<String> authTokenProvider) {
+   public AddVCloudAuthorizationAndCookieToRequest(@VCloudToken Supplier<String> authTokenProvider,  @ApiVersion final String apiVersion) {
       this.vcloudTokenProvider = authTokenProvider;
+      this.apiVersion = apiVersion;
    }
 
    @Override
    public HttpRequest filter(HttpRequest request) throws HttpException {
-      String token = vcloudTokenProvider.get();
-      return request
-               .toBuilder()
-               .replaceHeaders(
-                        ImmutableMultimap.of("x-vcloud-authorization", token, HttpHeaders.COOKIE, "vcloud-token="
-                                 + token)).build();
+       String token = vcloudTokenProvider.get();
+       String acceptType = request.getFirstHeaderOrNull(HttpHeaders.ACCEPT) == null
+               ? "application/*+xml"
+               : request.getFirstHeaderOrNull(HttpHeaders.ACCEPT);
+       String version = ";version=" + apiVersion;
+       String acceptHeader = acceptType + version;
+       return request.toBuilder()
+               .replaceHeaders(ImmutableMultimap.of(HttpHeaders.ACCEPT,
+                       acceptHeader, "x-vcloud-authorization", token,
+                       HttpHeaders.COOKIE, "vcloud-token=" +
+                               token)).build();
    }
 }
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/functions/CatalogsInOrg.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/functions/CatalogsInOrg.java
index 15be6c7..917f651 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/functions/CatalogsInOrg.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/functions/CatalogsInOrg.java
@@ -22,6 +22,9 @@ import javax.annotation.Resource;
 import javax.inject.Inject;
 import javax.inject.Singleton;
 
+import com.google.common.base.Predicate;
+import com.google.common.collect.Collections2;
+import com.google.common.collect.ImmutableSet;
 import org.jclouds.logging.Logger;
 import org.jclouds.vcloud.VCloudApi;
 import org.jclouds.vcloud.domain.Catalog;
@@ -30,6 +33,8 @@ import org.jclouds.vcloud.domain.ReferenceType;
 
 import com.google.common.base.Function;
 
+import java.util.Collection;
+
 @Singleton
 public class CatalogsInOrg implements Function<Org, Iterable<Catalog>> {
    @Resource
@@ -44,7 +49,18 @@ public class CatalogsInOrg implements Function<Org, Iterable<Catalog>> {
 
    @Override
    public Iterable<Catalog> apply(final Org org) {
-      return transform(org.getCatalogs().values(), new Function<ReferenceType, Catalog>() {
+       Collection<ReferenceType> filtered = Collections2.filter(
+               org.getCatalogs().values(), new Predicate<ReferenceType>() {
+                   @Override
+                   public boolean apply(ReferenceType type) {
+                       if (type == null) {
+                           return false;
+                       }
+                       return !ImmutableSet.of("add", "remove").contains(type.getRelationship());
+                   }
+               });
+
+       return transform(filtered, new Function<ReferenceType, Catalog>() {
          public Catalog apply(ReferenceType from) {
             return aclient.getCatalogApi().getCatalog(from.getHref());
          }
diff --git a/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/http/filters/VCloudBasicAuthentication.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/http/filters/VCloudBasicAuthentication.java
new file mode 100644
index 0000000..a333874
--- /dev/null
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/http/filters/VCloudBasicAuthentication.java
@@ -0,0 +1,58 @@
+/*
+  * Licensed to the Apache Software Foundation (ASF) under one or more
+  * contributor license agreements.  See the NOTICE file distributed with
+  * this work for additional information regarding copyright ownership.
+  * The ASF licenses this file to You under the Apache License, Version 2.0
+  * (the "License"); you may not use this file except in compliance with
+  * the License.  You may obtain a copy of the License at
+  *
+  *     http://www.apache.org/licenses/LICENSE-2.0
+  *
+  * Unless required by applicable law or agreed to in writing, software
+  * distributed under the License is distributed on an "AS IS" BASIS,
+  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
+  * See the License for the specific language governing permissions and
+  * limitations under the License.
+  */
+
+package org.jclouds.vcloud.http.filters;
+
+import com.google.common.base.Preconditions;
+import com.google.common.base.Supplier;
+import javax.inject.Inject;
+import javax.inject.Singleton;
+import org.jclouds.domain.Credentials;
+import org.jclouds.http.HttpException;
+import org.jclouds.http.HttpRequest;
+import org.jclouds.http.HttpRequest.Builder;
+import org.jclouds.http.HttpRequestFilter;
+import org.jclouds.http.filters.BasicAuthentication;
+import org.jclouds.location.Provider;
+import org.jclouds.rest.annotations.ApiVersion;
+
+@Singleton
+public class VCloudBasicAuthentication implements HttpRequestFilter
+{
+  private final Supplier<Credentials> creds;
+  private final String apiVersion;
+
+  @Inject
+  public VCloudBasicAuthentication(@Provider Supplier<Credentials> creds, @ApiVersion String apiVersion)
+  {
+    this.creds = ((Supplier)Preconditions.checkNotNull(creds, "creds"));
+    this.apiVersion = apiVersion;
+  }
+
+  public HttpRequest filter(HttpRequest request) throws HttpException
+  {
+    Credentials currentCreds = (Credentials)Preconditions.checkNotNull(this.creds.get(), "credential supplier returned null");
+    String acceptType = request.getFirstHeaderOrNull("Accept") == null ? "application/*+xml" : request.getFirstHeaderOrNull("Accept");
+
+    String version = ";version=" + this.apiVersion;
+    String acceptHeader = acceptType + version;
+
+    request = ((HttpRequest.Builder)request.toBuilder().replaceHeader("Accept", new String[] { acceptHeader })).build();
+
+    return ((HttpRequest.Builder)request.toBuilder().replaceHeader("Authorization", new String[] { BasicAuthentication.basic(currentCreds.identity, currentCreds.credential) })).build();
+  }
+}
diff --git a/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/http/filters/VCloudSupportedVersions.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/http/filters/VCloudSupportedVersions.java
new file mode 100644
index 0000000..3769fda
--- /dev/null
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/http/filters/VCloudSupportedVersions.java
@@ -0,0 +1,35 @@
+/*
+  * Licensed to the Apache Software Foundation (ASF) under one or more
+  * contributor license agreements.  See the NOTICE file distributed with
+  * this work for additional information regarding copyright ownership.
+  * The ASF licenses this file to You under the Apache License, Version 2.0
+  * (the "License"); you may not use this file except in compliance with
+  * the License.  You may obtain a copy of the License at
+  *
+  *     http://www.apache.org/licenses/LICENSE-2.0
+  *
+  * Unless required by applicable law or agreed to in writing, software
+  * distributed under the License is distributed on an "AS IS" BASIS,
+  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
+  * See the License for the specific language governing permissions and
+  * limitations under the License.
+  */
+
+package org.jclouds.vcloud.http.filters;
+
+import javax.inject.Singleton;
+
+import org.jclouds.http.HttpException;
+import org.jclouds.http.HttpRequest;
+import org.jclouds.http.HttpRequestFilter;
+
+@Singleton
+public class VCloudSupportedVersions implements HttpRequestFilter
+{
+  @SuppressWarnings("rawtypes")
+public HttpRequest filter(HttpRequest request)
+    throws HttpException
+  {
+    return ((HttpRequest.Builder)request.toBuilder().replaceHeader("Accept", new String[] { "*/*" })).build();
+  }
+}
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/internal/VCloudLoginApi.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/internal/VCloudLoginApi.java
index acf77c5..74a7be4 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/internal/VCloudLoginApi.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/internal/VCloudLoginApi.java
@@ -28,9 +28,10 @@ import org.jclouds.rest.annotations.ResponseParser;
 import org.jclouds.vcloud.VCloudMediaType;
 import org.jclouds.vcloud.domain.VCloudSession;
 import org.jclouds.vcloud.functions.ParseLoginResponseFromHeaders;
+import org.jclouds.vcloud.http.filters.VCloudBasicAuthentication;
 
 @Endpoint(org.jclouds.vcloud.endpoints.VCloudLogin.class)
-@RequestFilters(BasicAuthentication.class)
+@RequestFilters(VCloudBasicAuthentication.class)
 public interface VCloudLoginApi extends Closeable {
 
    /**
@@ -39,6 +40,6 @@ public interface VCloudLoginApi extends Closeable {
     */
    @POST
    @ResponseParser(ParseLoginResponseFromHeaders.class)
-   @Consumes(VCloudMediaType.ORGLIST_XML)
+   @Consumes({VCloudMediaType.SESSION_XML,VCloudMediaType.ORGLIST_XML})
    VCloudSession login();
 }
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/util/Utils.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/util/Utils.java
index 8bd10f1..623ee08 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/util/Utils.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/util/Utils.java
@@ -30,9 +30,10 @@ public class Utils {
    public static ReferenceType newReferenceType(Map<String, String> attributes, String defaultType) {
       String uri = attributes.get("href");
       String type = attributes.get("type");
+	  String relationship = attributes.get("rel");
       // savvis org has null href
       URI href = (uri != null) ? URI.create(uri) : null;
-      return new ReferenceTypeImpl(attributes.get("name"), type != null ? type : defaultType, href);
+      return new ReferenceTypeImpl(attributes.get("name"), type != null ? type : defaultType, href, relationship);
    }
 
    public static ReferenceType newReferenceType(Map<String, String> attributes) {
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/xml/OrgListHandler.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/xml/OrgListHandler.java
index 9088842..5176165 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/main/java/org/jclouds/vcloud/xml/OrgListHandler.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/main/java/org/jclouds/vcloud/xml/OrgListHandler.java
@@ -39,7 +39,7 @@ public class OrgListHandler extends ParseSax.HandlerWithResult<Map<String, Refer
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
       Map<String, String> attributes = SaxUtils.cleanseAttributes(attrs);
-      if (qName.endsWith("Org")) {
+      if (qName.endsWith("Link") || qName.endsWith("Org")) {
          String type = attributes.get("type");
          if (type != null) {
             if (type.indexOf("org+xml") != -1) {
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/VCloudVersionsApiTest.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/VCloudVersionsApiTest.java
index 6095e1d..37ae52b 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/VCloudVersionsApiTest.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/VCloudVersionsApiTest.java
@@ -57,7 +57,7 @@ public class VCloudVersionsApiTest extends BaseRestAnnotationProcessingTest<VClo
 
    @Override
    protected void checkFilters(HttpRequest request) {
-      assertEquals(request.getFilters().size(), 0);
+      assertEquals(request.getFilters().size(), 1);
    }
 
    @Override
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/compute/BaseVCloudComputeServiceExpectTest.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/compute/BaseVCloudComputeServiceExpectTest.java
index f343d6e..e938600 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/compute/BaseVCloudComputeServiceExpectTest.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/compute/BaseVCloudComputeServiceExpectTest.java
@@ -40,7 +40,7 @@ public abstract class BaseVCloudComputeServiceExpectTest extends BaseRestApiExpe
    protected static final String ENDPOINT = "https://zone.myvcloud.com/api";
 
    protected HttpRequest versionsRequest = HttpRequest.builder().method("GET").endpoint(
-            URI.create(ENDPOINT + "/versions")).build();
+            URI.create(ENDPOINT + "/versions")).addHeader(HttpHeaders.ACCEPT, "*/*").build();
 
    protected HttpResponse versionsResponseFromVCD1_5 = HttpResponse.builder().statusCode(200)
             .message("HTTP/1.1 200 OK").payload(payloadFromResourceWithContentType("/versions-vcd15.xml", "text/xml"))
@@ -48,7 +48,7 @@ public abstract class BaseVCloudComputeServiceExpectTest extends BaseRestApiExpe
 
    // initial auth is using basic
    protected HttpRequest version1_0LoginRequest = HttpRequest.builder().method("POST").endpoint(ENDPOINT + "/v1.0/login")
-            .addHeader(HttpHeaders.ACCEPT, VCloudMediaType.ORGLIST_XML)
+            .addHeader(HttpHeaders.ACCEPT, "application/vnd.vmware.vcloud.session+xml;version=1.0")
             .addHeader(HttpHeaders.AUTHORIZATION, "Basic aWRlbnRpdHk6Y3JlZGVudGlhbA==").build();
 
    protected String sessionToken = "AtatAgvJMrwOc9pDQq4RRCRLazThpnTKJDxSVH9oB2I=";
@@ -64,7 +64,7 @@ public abstract class BaseVCloudComputeServiceExpectTest extends BaseRestApiExpe
    protected String orgId = "c076f90a-397a-49fa-89b8-b294c1599cd0";
    
    protected HttpRequest version1_0GetOrgRequest = HttpRequest.builder().method("GET").endpoint(ENDPOINT + "/v1.0/org/" + orgId)
-            .addHeader(HttpHeaders.ACCEPT, VCloudMediaType.ORG_XML)
+            .addHeader(HttpHeaders.ACCEPT, VCloudMediaType.ORG_XML+";version=1.0")
             .addHeader("x-vcloud-authorization", sessionToken)
             .addHeader(HttpHeaders.COOKIE, "vcloud-token=" + sessionToken).build();
    
@@ -76,7 +76,7 @@ public abstract class BaseVCloudComputeServiceExpectTest extends BaseRestApiExpe
    protected String vdcId = "e9cd3387-ac57-4d27-a481-9bee75e0690f";
 
    protected HttpRequest version1_0GetCatalogRequest = HttpRequest.builder().method("GET").endpoint(ENDPOINT + "/v1.0/catalog/" + catalogId)
-            .addHeader(HttpHeaders.ACCEPT, VCloudMediaType.CATALOG_XML)
+            .addHeader(HttpHeaders.ACCEPT, VCloudMediaType.CATALOG_XML +";version=1.0")
             .addHeader("x-vcloud-authorization", sessionToken)
             .addHeader(HttpHeaders.COOKIE, "vcloud-token=" + sessionToken).build();
    
@@ -87,7 +87,7 @@ public abstract class BaseVCloudComputeServiceExpectTest extends BaseRestApiExpe
    protected String catalogItemId = "ceb369f7-1d07-4e32-9dbd-ebb5aa6ca55c";
    
    protected HttpRequest version1_0GetCatalogItemRequest = HttpRequest.builder().method("GET").endpoint(ENDPOINT + "/v1.0/catalogItem/" + catalogItemId)
-            .addHeader(HttpHeaders.ACCEPT, VCloudMediaType.CATALOGITEM_XML)
+            .addHeader(HttpHeaders.ACCEPT, VCloudMediaType.CATALOGITEM_XML +";version=1.0")
             .addHeader("x-vcloud-authorization", sessionToken)
             .addHeader(HttpHeaders.COOKIE, "vcloud-token=" + sessionToken).build();
    
@@ -99,7 +99,7 @@ public abstract class BaseVCloudComputeServiceExpectTest extends BaseRestApiExpe
    protected String templateId = "vappTemplate-51891b97-c5dd-47dc-a687-aabae354f728";
 
    protected HttpRequest version1_0GetVDCRequest = HttpRequest.builder().method("GET").endpoint(ENDPOINT + "/v1.0/vdc/" + vdcId)
-            .addHeader(HttpHeaders.ACCEPT, VCloudMediaType.VDC_XML)
+            .addHeader(HttpHeaders.ACCEPT, VCloudMediaType.VDC_XML +";version=1.0")
             .addHeader("x-vcloud-authorization", sessionToken)
             .addHeader(HttpHeaders.COOKIE, "vcloud-token=" + sessionToken).build();
             
@@ -110,7 +110,7 @@ public abstract class BaseVCloudComputeServiceExpectTest extends BaseRestApiExpe
    protected String networkId = "b466c0c5-8a5c-4335-b703-a2e2e6b5f3e1";
    
    protected HttpRequest version1_0GetVAppTemplateRequest = HttpRequest.builder().method("GET").endpoint(ENDPOINT + "/v1.0/vAppTemplate/" + templateId)
-            .addHeader(HttpHeaders.ACCEPT, VCloudMediaType.VAPPTEMPLATE_XML)
+            .addHeader(HttpHeaders.ACCEPT, VCloudMediaType.VAPPTEMPLATE_XML +";version=1.0")
             .addHeader("x-vcloud-authorization", sessionToken)
             .addHeader(HttpHeaders.COOKIE, "vcloud-token=" + sessionToken).build();
    
@@ -123,7 +123,7 @@ public abstract class BaseVCloudComputeServiceExpectTest extends BaseRestApiExpe
             .build();   
 
    protected HttpRequest version1_0GetOVFForVAppTemplateRequest = HttpRequest.builder().method("GET").endpoint(ENDPOINT + "/v1.0/vAppTemplate/" + templateId + "/ovf")
-            .addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_XML)
+            .addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_XML +";version=1.0")
             .addHeader("x-vcloud-authorization", sessionToken)
             .addHeader(HttpHeaders.COOKIE, "vcloud-token=" + sessionToken).build();
    
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/compute/strategy/InstantiateVAppTemplateWithGroupEncodedIntoNameThenCustomizeDeployAndPowerOnExpectTest.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/compute/strategy/InstantiateVAppTemplateWithGroupEncodedIntoNameThenCustomizeDeployAndPowerOnExpectTest.java
index 9f05f74..2608021 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/compute/strategy/InstantiateVAppTemplateWithGroupEncodedIntoNameThenCustomizeDeployAndPowerOnExpectTest.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/compute/strategy/InstantiateVAppTemplateWithGroupEncodedIntoNameThenCustomizeDeployAndPowerOnExpectTest.java
@@ -82,7 +82,7 @@ public class InstantiateVAppTemplateWithGroupEncodedIntoNameThenCustomizeDeployA
      
       HttpRequest version1_0InstantiateWithNetworkNamedSameAsOrgNetwork = HttpRequest.builder().method("POST")
                                                                            .endpoint(ENDPOINT + "/v1.0/vdc/" + vdcId + "/action/instantiateVAppTemplate")
-                                                                           .addHeader(HttpHeaders.ACCEPT, "application/vnd.vmware.vcloud.vApp+xml")
+                                                                           .addHeader(HttpHeaders.ACCEPT, "application/vnd.vmware.vcloud.vApp+xml;version=1.0")
                                                                            .addHeader("x-vcloud-authorization", sessionToken)
                                                                            .addHeader(HttpHeaders.COOKIE, "vcloud-token=" + sessionToken)
                                                                            .payload(payloadFromStringWithContentType(instantiateXML, "application/vnd.vmware.vcloud.instantiateVAppTemplateParams+xml")).build();
@@ -139,7 +139,7 @@ public class InstantiateVAppTemplateWithGroupEncodedIntoNameThenCustomizeDeployA
      
       HttpRequest version1_0InstantiateWithCustomizedNetwork = HttpRequest.builder().method("POST")
                                                                           .endpoint(ENDPOINT + "/v1.0/vdc/" + vdcId + "/action/instantiateVAppTemplate")
-                                                                          .addHeader(HttpHeaders.ACCEPT, "application/vnd.vmware.vcloud.vApp+xml")
+                                                                          .addHeader(HttpHeaders.ACCEPT, "application/vnd.vmware.vcloud.vApp+xml;version=1.0")
                                                                           .addHeader("x-vcloud-authorization", sessionToken)
                                                                           .addHeader(HttpHeaders.COOKIE, "vcloud-token=" + sessionToken)
                                                                           .payload(payloadFromStringWithContentType(instantiateXML, "application/vnd.vmware.vcloud.instantiateVAppTemplateParams+xml")).build();
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/filters/AddVCloudAuthorizationAndCookieToRequestTest.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/filters/AddVCloudAuthorizationAndCookieToRequestTest.java
index b2e4687..40d1fd5 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/filters/AddVCloudAuthorizationAndCookieToRequestTest.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/filters/AddVCloudAuthorizationAndCookieToRequestTest.java
@@ -32,18 +32,18 @@ public class AddVCloudAuthorizationAndCookieToRequestTest {
 
    @BeforeTest
    void setUp() {
-      filter = new AddVCloudAuthorizationAndCookieToRequest(new Supplier<String>() {
-         public String get() {
-            return "token";
-         }
-      });
+       filter = new AddVCloudAuthorizationAndCookieToRequest(new Supplier<String>() {
+           public String get() {
+               return "token";
+           }
+       }, "1.0");
    }
 
    @Test
    public void testApply() {
       HttpRequest request = HttpRequest.builder().method("GET").endpoint("http://localhost").build();
       request = filter.filter(request);
-      assertEquals(request.getHeaders().size(), 2);
+      assertEquals(request.getHeaders().size(), 3);
       assertEquals(request.getFirstHeaderOrNull(HttpHeaders.COOKIE), "vcloud-token=token");
       assertEquals(request.getFirstHeaderOrNull("x-vcloud-authorization"), "token");
    }
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/internal/VCloudLoginApiTest.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/internal/VCloudLoginApiTest.java
index c2896dc..b97cdf0 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/internal/VCloudLoginApiTest.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/internal/VCloudLoginApiTest.java
@@ -31,6 +31,7 @@ import org.jclouds.rest.internal.BaseRestAnnotationProcessingTest;
 import org.jclouds.rest.internal.GeneratedHttpRequest;
 import org.jclouds.vcloud.endpoints.VCloudLogin;
 import org.jclouds.vcloud.functions.ParseLoginResponseFromHeaders;
+import org.jclouds.vcloud.http.filters.VCloudBasicAuthentication;
 import org.testng.annotations.Test;
 
 import com.google.common.base.Supplier;
@@ -52,7 +53,7 @@ public class VCloudLoginApiTest extends BaseRestAnnotationProcessingTest<VCloudL
       GeneratedHttpRequest request = processor.createRequest(method, ImmutableList.of());
 
       assertEquals(request.getRequestLine(), "POST http://localhost:8080/login HTTP/1.1");
-      assertNonPayloadHeadersEqual(request, HttpHeaders.ACCEPT + ": application/vnd.vmware.vcloud.orgList+xml\n");
+      assertNonPayloadHeadersEqual(request, HttpHeaders.ACCEPT + ": application/vnd.vmware.vcloud.orgList+xml\n" + HttpHeaders.ACCEPT + ": application/vnd.vmware.vcloud.session+xml\n");
       assertPayloadEquals(request, null, null, false);
 
       assertResponseParserClassEquals(method, request, ParseLoginResponseFromHeaders.class);
@@ -65,7 +66,7 @@ public class VCloudLoginApiTest extends BaseRestAnnotationProcessingTest<VCloudL
    @Override
    protected void checkFilters(HttpRequest request) {
       assertEquals(request.getFilters().size(), 1);
-      assertEquals(request.getFilters().get(0).getClass(), BasicAuthentication.class);
+      assertEquals(request.getFilters().get(0).getClass(), VCloudBasicAuthentication.class);
    }
 
    @Override
diff --git a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/xml/ovf/VCloudVirtualHardwareSectionHandlerTest.java b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/xml/ovf/VCloudVirtualHardwareSectionHandlerTest.java
index 8e480de..72be94a 100644
--- a/home/rajkumar/workspace/apache/jclouds/jclouds-jclouds-1.8.1/apis/vcloud/src/test/java/org/jclouds/vcloud/xml/ovf/VCloudVirtualHardwareSectionHandlerTest.java
+++ b/dependencies/jclouds/apis/vcloud/1.8.1-stratos/src/test/java/org/jclouds/vcloud/xml/ovf/VCloudVirtualHardwareSectionHandlerTest.java
@@ -125,7 +125,7 @@ public class VCloudVirtualHardwareSectionHandlerTest extends BaseHandlerTest {
                                           null,
                                           "application/vnd.vmware.vcloud.rasdItem+xml",
                                           URI
-                                                   .create("https://vcenterprise.bluelock.com/api/v1.0/vApp/vm-2087535248/virtualHardwareSection/cpu")))
+                                                   .create("https://vcenterprise.bluelock.com/api/v1.0/vApp/vm-2087535248/virtualHardwareSection/cpu"),"edit"))
                         .build().toString());
 
       assertEquals(
@@ -145,7 +145,7 @@ public class VCloudVirtualHardwareSectionHandlerTest extends BaseHandlerTest {
                                           null,
                                           "application/vnd.vmware.vcloud.rasdItem+xml",
                                           URI
-                                                   .create("https://vcenterprise.bluelock.com/api/v1.0/vApp/vm-2087535248/virtualHardwareSection/memory")))
+                                                   .create("https://vcenterprise.bluelock.com/api/v1.0/vApp/vm-2087535248/virtualHardwareSection/memory"),"edit"))
                         .build().toString());
    }
 }
