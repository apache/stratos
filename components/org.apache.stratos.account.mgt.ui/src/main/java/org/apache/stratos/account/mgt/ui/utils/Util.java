/*
 *     Licensed to the Apache Software Foundation (ASF) under one
 *     or more contributor license agreements.  See the NOTICE file
 *     distributed with this work for additional information
 *     regarding copyright ownership.  The ASF licenses this file
 *     to you under the Apache License, Version 2.0 (the
 *     "License"); you may not use this file except in compliance
 *     with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing,
 *     software distributed under the License is distributed on an
 *     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *     KIND, either express or implied.  See the License for the
 *     specific language governing permissions and limitations
 *     under the License.
 */


package org.apache.stratos.account.mgt.ui.utils;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.account.mgt.stub.beans.xsd.AccountInfoBean;
import org.apache.stratos.account.mgt.ui.clients.AccountMgtClient;
import org.apache.stratos.account.mgt.ui.clients.UsagePlanClient;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.Iterator;

public class Util {
    private static final Log log = LogFactory.getLog(Util.class);
    
    public static HttpServletRequest readIntermediateData(HttpServletRequest request,String data){
        try{
            XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(data));
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            OMElement documentElement =  builder.getDocumentElement();
            Iterator it = documentElement.getChildElements();
            while(it.hasNext()){
                OMElement element = (OMElement)it.next();
                if ("admin".equals(element.getLocalName())) {
                    request.setAttribute("admin",element.getText());
                } else if ("email".equals(element.getLocalName())) {
                    request.setAttribute("email",element.getText());
                } else if ("tenantDomain".equals(element.getLocalName())
                           && request.getAttribute("tenantDomain") == null) {
                    request.setAttribute("tenantDomain",element.getText());
                } else if ("confirmationKey".equals(element.getLocalName())) {
                    request.setAttribute("confirmationKey",element.getText());
                }
            }
        }catch(Exception e){
            log.error("Error parsing xml",e);
        }
        return request;
    }

    public static boolean updateFullname(
            HttpServletRequest request,
            ServletConfig config,
            HttpSession session) throws Exception {

        AccountInfoBean accountInfoBean = new AccountInfoBean();
        String firstname = "", lastname = "";
        try {
            firstname = request.getParameter("firstname");
            lastname = request.getParameter("lastname");
            accountInfoBean.setFirstname(firstname);
            accountInfoBean.setLastname(lastname);
            AccountMgtClient client = new AccountMgtClient(config, session);
            return client.updateFullname(accountInfoBean);
        } catch (Exception e) {
            String msg = "Failed to update tenant with firstname: " + firstname;
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }
    public static String getUsagePlanName(ServletConfig config,
                                           HttpSession session) throws Exception {
        try{
            String tenantDomain=(String)session.getAttribute("tenantDomain");
            UsagePlanClient client=new UsagePlanClient(config, session);
            return client.getUsagePlanName(tenantDomain);         
        }
        catch (Exception e){
            String msg = "Failed to get usage plan for tenant";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    public static boolean updateUsagePlan( HttpServletRequest request, ServletConfig config,
                                           HttpSession session) throws Exception {
        boolean updated = false;
        String tenantDomain="";
        try{
            tenantDomain=(String)session.getAttribute("tenantDomain");
            String usagePlanName=(String)request.getParameter("selectedUsagePlan");
            UsagePlanClient client=new UsagePlanClient(config, session);
            updated = client.updateUsagePlan(usagePlanName);
        }
       catch (Exception e){
            String msg = "Failed to update the usage plan for tenant: " + tenantDomain;
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        return updated;
    }
    
}
