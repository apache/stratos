/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.metadata.client.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.stratos.metadata.client.exception.RestClientException;

import java.io.IOException;

public class DefaultRestClient implements RestClient {

    private static Log log = LogFactory.getLog(DefaultRestClient.class);

    private HttpClient httpClient;

    public DefaultRestClient() {
        this.httpClient = new DefaultHttpClient();
    }

    public HttpResponse doPost(String resourcePath, Object payload) throws RestClientException {

        HttpPost post = new HttpPost(resourcePath);
        //TODO set params
        try {
            return httpClient.execute(post);

        } catch (IOException e) {
            String errorMsg = "Error while executing POST statement";
            log.error(errorMsg, e);
            throw new RestClientException(errorMsg, e);
        }
    }

    public HttpResponse doGet(String resourcePath) throws RestClientException {

        HttpGet get = new HttpGet(resourcePath);

        try {
            return httpClient.execute(get);

        } catch (IOException e) {
            String errorMsg = "Error while executing GET statement";
            log.error(errorMsg, e);
            throw new RestClientException(errorMsg, e);
        }
    }

    public HttpResponse doDelete(String resourcePath) throws RestClientException {

        HttpDelete delete = new HttpDelete(resourcePath);

        try {
            return httpClient.execute(delete);

        } catch (IOException e) {
            String errorMsg = "Error while executing DELETE statement";
            log.error(errorMsg, e);
            throw new RestClientException(errorMsg, e);
        }
    }
}
