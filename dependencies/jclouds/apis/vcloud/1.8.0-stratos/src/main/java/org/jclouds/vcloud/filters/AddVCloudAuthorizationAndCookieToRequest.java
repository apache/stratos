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
package org.jclouds.vcloud.filters;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.http.HttpException;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpRequestFilter;
import org.jclouds.rest.annotations.ApiVersion;
import org.jclouds.vcloud.VCloudToken;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.net.HttpHeaders;

/**
 * Adds the VCloud Token to the request as a cookie
 */
@Singleton
public class AddVCloudAuthorizationAndCookieToRequest implements HttpRequestFilter {
   private Supplier<String> vcloudTokenProvider;
   private final String apiVersion;

   @Inject
   public AddVCloudAuthorizationAndCookieToRequest(@VCloudToken Supplier<String> authTokenProvider,  @ApiVersion final String apiVersion) {
      this.vcloudTokenProvider = authTokenProvider;
      this.apiVersion = apiVersion;
   }

   @Override
   public HttpRequest filter(HttpRequest request) throws HttpException {
       String token = vcloudTokenProvider.get();
       String acceptType = request.getFirstHeaderOrNull(HttpHeaders.ACCEPT) == null
               ? "application/*+xml"
               : request.getFirstHeaderOrNull(HttpHeaders.ACCEPT);
       String version = ";version=" + apiVersion;
       String acceptHeader = acceptType + version;
       return request.toBuilder()
               .replaceHeaders(ImmutableMultimap.of(HttpHeaders.ACCEPT,
                       acceptHeader, "x-vcloud-authorization", token,
                       HttpHeaders.COOKIE, "vcloud-token=" +
                               token)).build();
   }
}
