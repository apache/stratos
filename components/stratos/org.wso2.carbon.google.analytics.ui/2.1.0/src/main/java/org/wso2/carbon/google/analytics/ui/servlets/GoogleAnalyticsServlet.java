/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.google.analytics.ui.servlets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.util.CommonUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

/**
 * GoogleAnalyticsServlet use for sending a request to a remote URL in where a js snippet which
 * is  required to process google analytics is hosted.Then this servlet get back the content of
 * that js snippet and set it as the response.Then by using a javascript,this servlet response will
 * be embedded into another html/jsp page and make use of the js snippet content from that html/jsp
 * page.
 */
public class GoogleAnalyticsServlet extends HttpServlet {
    private static final Log log = LogFactory.getLog(GoogleAnalyticsServlet.class);
    private static String GOOGLE_ANALYTICS_URL = "GoogleAnalyticsURL";
    private static String googleAnalyticsURL = "googleAnalyticUrl";

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String url = null;
        try {
            //Read Google analytic url from stratos.xml
            url = CommonUtil.loadStratosConfiguration().getGoogleAnalyticsURL();
            if (url != null && !(url.equals(""))) {
                //Create a new connection to above read google analytic url and read content of url.
                URL googleAnalyticUrl = new URL(url);
                URLConnection connection = googleAnalyticUrl.openConnection();
                //Set the input stream of created URL connection to a reader
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream()));
                String inputLine;
                //Set servlet response as 'text/javascript'
                response.setContentType("text/javascript");
                PrintWriter out = response.getWriter();
                //Read the input stream which has fetched to buffered reader,line by line and then
                //write those content into the servlet response.
                while ((inputLine = in.readLine()) != null) {
                    out.write(inputLine);
                }
                in.close();
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            String msg = "Failed to get content from the url." + url + e.getMessage();
            log.error(msg, e);
            response.setStatus(500);
        }


    }

}

