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

package org.jclouds.vcloud.http.filters;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jclouds.domain.Credentials;
import org.jclouds.http.HttpException;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpRequest.Builder;
import org.jclouds.http.HttpRequestFilter;
import org.jclouds.http.filters.BasicAuthentication;
import org.jclouds.location.Provider;
import org.jclouds.rest.annotations.ApiVersion;

@Singleton
public class VCloudBasicAuthentication implements HttpRequestFilter
{
  private final Supplier<Credentials> creds;
  private final String apiVersion;

  @Inject
  public VCloudBasicAuthentication(@Provider Supplier<Credentials> creds, @ApiVersion String apiVersion)
  {
    this.creds = ((Supplier)Preconditions.checkNotNull(creds, "creds"));
    this.apiVersion = apiVersion;
  }

  public HttpRequest filter(HttpRequest request) throws HttpException
  {
    Credentials currentCreds = (Credentials)Preconditions.checkNotNull(this.creds.get(), "credential supplier returned null");
    String acceptType = request.getFirstHeaderOrNull("Accept") == null ? "application/*+xml" : request.getFirstHeaderOrNull("Accept");

    String version = ";version=" + this.apiVersion;
    String acceptHeader = acceptType + version;

    request = ((HttpRequest.Builder)request.toBuilder().replaceHeader("Accept", new String[] { acceptHeader })).build();

    return ((HttpRequest.Builder)request.toBuilder().replaceHeader("Authorization", new String[] { BasicAuthentication.basic(currentCreds.identity, currentCreds.credential) })).build();
  }
}
