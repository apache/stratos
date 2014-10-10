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
package org.apache.stratos.kubernetes.client.rest;

import java.net.URI;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

public class RestClient {

    private DefaultHttpClient httpClient;
    
    public RestClient() {
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 50
        cm.setDefaultMaxPerRoute(50);
        
        httpClient = new DefaultHttpClient(cm);
    }

	/**
     * Handle http post request. Return String
     *
     * @param  httpClient
     *              This should be httpClient which used to connect to rest endpoint
     * @param resourcePath
     *              This should be REST endpoint
     * @param jsonParamString
     *              The json string which should be executed from the post request
     * @param username
     *              User name for basic auth
     * @param password
     *              Password for basic auth
     * @return The HttpResponse
     * @throws Exception
     *             if any errors occur when executing the request
     */
    public KubernetesResponse doPost(URI resourcePath, String jsonParamString) throws Exception{
        HttpPost postRequest = null;
        try {
            postRequest = new HttpPost(resourcePath);

            StringEntity input = new StringEntity(jsonParamString);
            input.setContentType("application/json");
            postRequest.setEntity(input);

            KubernetesResponse response = httpClient.execute(postRequest, new KubernetesResponseHandler());

            return response;
        } finally {
            releaseConnection(postRequest);
        }
    }

    /**
     * Handle http get request. Return String
     *
     * @param  httpClient
     *              This should be httpClient which used to connect to rest endpoint
     * @param resourcePath
     *              This should be REST endpoint
     * @param username
     *              User name for basic auth
     * @param password
     *              Password for basic auth
     * @return The HttpResponse
     * @throws org.apache.http.client.ClientProtocolException and IOException
     *             if any errors occur when executing the request
     */
    public KubernetesResponse doGet(URI resourcePath) throws Exception{
        HttpGet getRequest = null;
        try {
            getRequest = new HttpGet(resourcePath);
            getRequest.addHeader("Content-Type", "application/json");

            KubernetesResponse response = httpClient.execute(getRequest, new KubernetesResponseHandler());
            return response;
        } finally {
            releaseConnection(getRequest);
        }
    }
    
    public KubernetesResponse doDelete(URI resourcePath) throws Exception {
        HttpDelete httpDelete = null;
        try {
            httpDelete = new HttpDelete(resourcePath);
            httpDelete.addHeader("Content-Type", "application/json");

            KubernetesResponse response = httpClient.execute(httpDelete, new KubernetesResponseHandler());

            return  response;

        } finally {
        	 releaseConnection(httpDelete);
        }
    }

    public KubernetesResponse doPut(URI resourcePath, String jsonParamString) throws Exception {

        HttpPut putRequest = null;
		try {
			putRequest = new HttpPut(resourcePath);

			StringEntity input = new StringEntity(jsonParamString);
			input.setContentType("application/json");
			putRequest.setEntity(input);

			KubernetesResponse response = httpClient.execute(putRequest, new KubernetesResponseHandler());

			return response;
		
		} finally {
			releaseConnection(putRequest);
		}
    }
    
    private void releaseConnection(HttpRequestBase request) {
        if (request != null) {
            request.releaseConnection();
        }
    }

}