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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.stratos.cli.utils.CliConstants;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

public class RestClient implements GenericRestClient{

    private String url;
    private String username;
    private String password;

    RestClient(String url, String username, String password) {
        this.setUrl(url);
        this.setUsername(username);
        this.setPassword(password);
    }

    /**
     * Handle http post request. Return String
     *
     * @param resourcePath
     *              This should be REST endpoint
     * @param jsonParamString
     *              The json string which should be executed from the post request
     * @param userName
     *              User name for basic auth
     * @param passWord
     *              Password for basic auth
     * @return The HttpResponse
     * @throws org.apache.http.client.ClientProtocolException and IOException
     *             if any errors occur when executing the request
     */
    public HttpResponse doPost(DefaultHttpClient httpClient, String resourcePath, String jsonParamString, String userName, String passWord) throws Exception{
        try {

            //DefaultHttpClient httpClient = new DefaultHttpClient();

            HttpPost postRequest = new HttpPost(resourcePath);

            StringEntity input = new StringEntity(jsonParamString);
            input.setContentType("application/json");
            postRequest.setEntity(input);

            String userPass = userName + ":" + passWord;
            String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userPass.getBytes("UTF-8"));
            postRequest.addHeader("Authorization", basicAuth);

            httpClient = (DefaultHttpClient) WebClientWrapper.wrapClient(httpClient);

            HttpParams params = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, 300000);
            HttpConnectionParams.setSoTimeout(params, 300000);

            HttpResponse response = httpClient.execute(postRequest);

            /*
            int responseCode = response.getStatusLine().getStatusCode();

            if (responseCode == CliConstants.RESPONSE_AUTHORIZATION_FAIL) {
                return "" + CliConstants.RESPONSE_AUTHORIZATION_FAIL;
            } else if (responseCode == CliConstants.RESPONSE_NO_CONTENT) {
                return "" + CliConstants.RESPONSE_NO_CONTENT;
            } else if (responseCode == CliConstants.RESPONSE_INTERNAL_SERVER_ERROR) {
                return "" + CliConstants.RESPONSE_INTERNAL_SERVER_ERROR;
            } else if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

            String output;
            String result = "";
            while ((output = br.readLine()) != null) {
                result += output;
            }
            */

            //httpClient.getConnectionManager().shutdown();
            //return result;
            return response;

        } catch (ClientProtocolException e) {
            throw new ClientProtocolException();
        } catch (ConnectException e) {
            throw new ConnectException();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Handle http get request. Return String
     *
     * @param resourcePath
     *              This should be REST endpoint
     * @param userName
     *              User name for basic auth
     * @param passWord
     *              Password for basic auth
     * @return The HttpResponse
     * @throws org.apache.http.client.ClientProtocolException and IOException
     *             if any errors occur when executing the request
     */
    public HttpResponse doGet(DefaultHttpClient httpClient, String resourcePath, String userName, String passWord) {
        try {
            //DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet(resourcePath);
            getRequest.addHeader("Content-Type", "application/json");

            String userPass = userName + ":" + passWord;
            String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userPass.getBytes("UTF-8"));
            getRequest.addHeader("Authorization", basicAuth);

            httpClient = (DefaultHttpClient) WebClientWrapper.wrapClient(httpClient);

            HttpParams params = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, 300000);
            HttpConnectionParams.setSoTimeout(params, 300000);

            HttpResponse response = httpClient.execute(getRequest);

            /*
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

            String output;
            String result = "";
            while ((output = br.readLine()) != null) {
                result += output;
            }
            */

            //httpClient.getConnectionManager().shutdown();
            return response;

        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void doDelete() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void doPut() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
