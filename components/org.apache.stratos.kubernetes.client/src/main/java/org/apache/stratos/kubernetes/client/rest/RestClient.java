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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class RestClient {

    private DefaultHttpClient httpClient;

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
    public HttpResponse doPost(URI resourcePath, String jsonParamString) throws Exception{
        try {
        	httpClient = new DefaultHttpClient();
            HttpPost postRequest = new HttpPost(resourcePath);

            StringEntity input = new StringEntity(jsonParamString);
            input.setContentType("application/json");
            postRequest.setEntity(input);

            HttpResponse response = httpClient.execute(postRequest);

            return response;
        } finally {
        	 httpClient.getConnectionManager().shutdown();
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
    public HttpResponse doGet(URI resourcePath) throws Exception{
        try {
        	httpClient = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet(resourcePath);
            System.out.println(getRequest.getRequestLine().getUri());
            getRequest.addHeader("Content-Type", "application/json");

            HttpResponse response = httpClient.execute(getRequest);

            return response;
        } finally {
        	 httpClient.getConnectionManager().shutdown();
        }
    }

    public HttpResponse doDelete(URI resourcePath) throws Exception {
        try {
        	httpClient = new DefaultHttpClient();
            HttpDelete httpDelete = new HttpDelete(resourcePath);
            httpDelete.addHeader("Content-Type", "application/json");

            HttpResponse response = httpClient.execute(httpDelete);

            return  response;

        } finally {
        	 httpClient.getConnectionManager().shutdown();
        }
    }

    public HttpResponse doPut(URI resourcePath, String jsonParamString) throws Exception {

		try {
			httpClient = new DefaultHttpClient();
			HttpPut putRequest = new HttpPut(resourcePath);

			StringEntity input = new StringEntity(jsonParamString);
			input.setContentType("application/json");
			putRequest.setEntity(input);

			HttpResponse response = httpClient.execute(putRequest);

			return response;
		
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
    }

}