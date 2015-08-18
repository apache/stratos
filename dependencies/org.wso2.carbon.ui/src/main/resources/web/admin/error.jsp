<%--
 Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

 WSO2 Inc. licenses this file to you under the Apache License,
 Version 2.0 (the "License"); you may not use this file except
 in compliance with the License.
 You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.CarbonError" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<%@page import="javax.xml.namespace.QName" %>
<%@page import="java.io.PrintWriter" %>
<%@page import="java.io.StringWriter" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<carbon:breadcrumb
        label="error.occurred"
        resourceBundle="org.wso2.carbon.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>

<%
    //First checks whether there is a CarbonUIMessage in the request
    CarbonUIMessage carbonMessage = (CarbonUIMessage) session.getAttribute(CarbonUIMessage.ID);

    if (carbonMessage == null) {
        carbonMessage = (CarbonUIMessage) request.getAttribute(CarbonUIMessage.ID);
    } else {
        session.removeAttribute(CarbonUIMessage.ID);
    }
    if (carbonMessage != null) {
        session.removeAttribute(CarbonUIMessage.ID);
        Exception e = carbonMessage.getException();
        boolean authFailure = false;
        boolean sessionTimedOut = false;
        if (e != null) {
            Throwable cause = e.getCause();
            if (e instanceof AxisFault) {
                AxisFault axisFault = (AxisFault) e;
                QName name = axisFault.getFaultCode();
                if (name != null && name.getLocalPart() != null && name.getLocalPart().equals(CarbonConstants.AUTHZ_FAULT_CODE)) {
                    authFailure = true;
                }
                if(e.getMessage().toLowerCase().indexOf("session timed out") != -1){
                     sessionTimedOut = true;
                }
            } else if ((cause != null) && (cause instanceof AxisFault)) {
                AxisFault axisFault = (AxisFault) cause;
                QName name = axisFault.getFaultCode();
                if (name != null && name.getLocalPart() != null && name.getLocalPart().equals(CarbonConstants.AUTHZ_FAULT_CODE)) {
                    authFailure = true;
                }
            }
        }

%>
<div id="middle">
    <%
        if (authFailure) {
    %>
    <h2><img src='../dialog/img/error.gif'/> Authorization Failure</h2>
    <% } else if(sessionTimedOut) {
           session.invalidate();
           return;
    } else { %>
    <h2><img src='../dialog/img/error.gif'/> System Error Occurred</h2>
    <%
        }
    %>

    <div id="workArea">
        <table class="styledLeft">
            <tbody>
            <%
                if (e != null) {
                    if (authFailure) {
            %>
            <tr>
                <td><b>Authorization Failure</b></td>
            </tr>
            <tr>
                <td>
                    <%
                        out.write("You are not authorized to perform this action.");
                    %>
                </td>
            </tr>
            <%
            } else {
            %>
            <tr>
                <td><b><%=carbonMessage.getMessage()%></b></td>
            </tr>
            <tr>
                <td>
                    <%
	                    StringWriter sw = new StringWriter();
	                    PrintWriter pw = new PrintWriter(sw);
                    	if(carbonMessage.isShowStackTrace()){
                    	    pw.write("<b>The following error details are available. Please refer logs for more details.</b><br/>");
                            e.printStackTrace(pw);
                            String errorStr = sw.toString();
                            errorStr = errorStr.replaceAll("\n", "<br/>");
                            for (int i = 0; i < errorStr.length(); i++) {
                                out.write(errorStr.charAt(i));
                            }
                        }else{
                        	pw.write("<b>Please refer log for details.</b><br/>");
                        }
                        sw.close();
                        pw.close();
                        out.flush();
            	}
                    %>
                </td>
            </tr>
            <%
            } else {
            %>
            <tr>
                <td><b><%=carbonMessage.getMessage()%>
                </b></td>
            </tr>
            <%
                }
            %>
            </tbody>
        </table>
    </div>
</div>
<%
    }
%>


<%
    String errorMsg = CharacterEncoder.getSafeText(request.getParameter("errorMsg"));
    CarbonError error = null;
    boolean retrievedFromSession = false;
    error = (CarbonError) request.getAttribute(CarbonError.ID);
    if (error == null) {
        //look for error object in session
        error = (CarbonError) request.getSession().getAttribute(CarbonError.ID);
        retrievedFromSession = true;
    }
    if (error != null) {
%>
<p>
    <label>Error occurred</label>
        <%

     ArrayList<String> list = (ArrayList<String>) error.getErrors();
     String[] errors = (String[])list.toArray(new String[list.size()]);
     for(int a = 0;a < errors.length;a++){

    %>
<li><%=errors[a]%>
</li>
<%
    }
%>
</p>
<%
        if (retrievedFromSession) {
            request.getSession().setAttribute(CarbonError.ID, null);
        }
    }
%>