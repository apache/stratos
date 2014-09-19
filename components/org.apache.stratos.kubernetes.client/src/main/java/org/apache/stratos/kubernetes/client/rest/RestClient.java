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

import java.io.IOException;
import java.net.ConnectException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

@SuppressWarnings("deprecation")
public class RestClient {

    private String baseURL;
    private DefaultHttpClient httpClient;

    private final int TIME_OUT_PARAM = 6000000;

    public RestClient(String baseURL) {
        this.baseURL = baseURL;
    }

    public String getBaseURL() {
		return baseURL;
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
     * @throws org.apache.http.client.ClientProtocolException and IOException
     *             if any errors occur when executing the request
     */
    public HttpResponse doPost(String resourcePath, String jsonParamString) throws Exception{
        try {
        	httpClient = new DefaultHttpClient();
            HttpPost postRequest = new HttpPost(baseURL+resourcePath);

            StringEntity input = new StringEntity(jsonParamString);
            input.setContentType("application/json");
            postRequest.setEntity(input);

            HttpResponse response = httpClient.execute(postRequest);

            return response;
        } catch (ClientProtocolException e) {
            throw new ClientProtocolException();
        } catch (ConnectException e) {
            throw new ConnectException();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
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
    public HttpResponse doGet(String resourcePath) throws Exception{
        try {
        	httpClient = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet(baseURL+resourcePath);
            getRequest.addHeader("Content-Type", "application/json");

            HttpParams params = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, TIME_OUT_PARAM);
            HttpConnectionParams.setSoTimeout(params, TIME_OUT_PARAM);

            HttpResponse response = httpClient.execute(getRequest);

            return response;
        } catch (ClientProtocolException e) {
            throw new ClientProtocolException();
        } catch (ConnectException e) {
            throw new ConnectException();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
        	 httpClient.getConnectionManager().shutdown();
        }
    }

    public HttpResponse doDelete(String resourcePath) throws Exception {
        try {
        	httpClient = new DefaultHttpClient();
            HttpDelete httpDelete = new HttpDelete(baseURL+resourcePath);
            httpDelete.addHeader("Content-Type", "application/json");

            HttpParams params = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, TIME_OUT_PARAM);
            HttpConnectionParams.setSoTimeout(params, TIME_OUT_PARAM);

            HttpResponse response = httpClient.execute(httpDelete);

            return  response;

        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
        	 httpClient.getConnectionManager().shutdown();
        }
    }

    public void doPut() {
        // Not implemented
    }

}