/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.cli;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

public interface GenericRestClient {

    /**
     * Handle http post request. Return String
     *
     * @param resourcePath
     *              This should be REST endpoint
     * @param jsonParamString
     *              The json string which should be executed from the post request
     * @return The HttpResponse
     * @throws org.apache.http.client.ClientProtocolException and IOException
     *             if any errors occur when executing the request
     */
    public HttpResponse doPost(DefaultHttpClient httpClient, String resourcePath, String jsonParamString) throws Exception;

    /**
     * Handle http get request. Return String
     *
     * @param resourcePath
     *              This should be REST endpoint
     * @return The HttpResponse
     * @throws org.apache.http.client.ClientProtocolException and IOException
     *             if any errors occur when executing the request
     */
    public HttpResponse doGet(DefaultHttpClient httpClient, String resourcePath) throws Exception;

    public HttpResponse doDelete(DefaultHttpClient httpClient, String resourcePath) throws IOException;

    public HttpResponse doPut(DefaultHttpClient httpClient, String resourcePath, String jsonParamString) throws IOException;
}
