<%--
 * Copyright 2008 WSO2, Inc. http://www.wso2.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
--%>
<%@ page import="org.apache.axiom.om.OMElement" %>
<%@ page import="org.apache.axiom.om.OMNamespace" %>
<%@ page import="org.apache.axiom.om.impl.llom.util.AXIOMUtil" %>
<%@ page import="org.apache.axiom.om.util.Base64" %>
<%@ page import="org.apache.axiom.soap.SOAP11Constants" %>
<%@ page import="org.apache.axiom.soap.SOAP12Constants"%>
<%@ page import="org.apache.axiom.soap.SOAPEnvelope"%>
<%@ page import="org.apache.axis2.AxisFault"%>
<%@ page import="org.apache.axis2.Constants"%>
<%@ page import="org.apache.axis2.addressing.EndpointReference"%>
<%@ page import="org.apache.axis2.client.Options"%>
<%@ page import="org.apache.axis2.client.ServiceClient"%>
<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.apache.axis2.context.MessageContext"%>
<%@ page import="org.apache.axis2.context.OperationContext"%>
<%@ page import="org.apache.axis2.description.WSDL2Constants"%>
<%@ page import="org.apache.axis2.util.JavaUtils"%>
<%@ page import="org.apache.neethi.PolicyEngine"%>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="org.apache.axis2.transport.http.HTTPConstants"%>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="org.apache.commons.httpclient.Header" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%!

    public static String decode(String s) throws Exception {
        if ("~".equals(s)) return null;
        return new String(Base64.decode(s), "UTF-8");
    }

%><%
    boolean useWSS = false;
    String policy = "<wsp:Policy wsu:Id=\"UTOverTransport\"\n" +
"            xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"\n" +
"            xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
"    <wsp:ExactlyOne>\n" +
"        <wsp:All>\n" +
"            <sp:TransportBinding xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
"                <wsp:Policy>\n" +
"                    <sp:TransportToken>\n" +
"                        <wsp:Policy>\n" +
"                            <sp:HttpsToken RequireClientCertificate=\"false\"/>\n" +
"                        </wsp:Policy>\n" +
"                    </sp:TransportToken>\n" +
"                    <sp:AlgorithmSuite>\n" +
"                        <wsp:Policy>\n" +
"                            <sp:Basic256/>\n" +
"                        </wsp:Policy>\n" +
"                    </sp:AlgorithmSuite>\n" +
"                    <sp:Layout>\n" +
"                        <wsp:Policy>\n" +
"                            <sp:Lax/>\n" +
"                        </wsp:Policy>\n" +
"                    </sp:Layout>\n" +
"                    <sp:IncludeTimestamp/>\n" +
"                </wsp:Policy>\n" +
"            </sp:TransportBinding>\n" +
"            <sp:SignedSupportingTokens\n" +
"                    xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
"                <wsp:Policy>\n" +
"                    <sp:UsernameToken\n" +
"                            sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient\"/>\n" +
"                </wsp:Policy>\n" +
"            </sp:SignedSupportingTokens>\n" +
"        </wsp:All>\n" +
"    </wsp:ExactlyOne>\n" +
"</wsp:Policy>";

    // Extract and decode all the parameters used to call WSRequest
    String uri, pattern, username, password, payload;
    try {
        uri = decode(request.getParameter("uri"));
		pattern = decode(request.getParameter("pattern"));
        username = decode(request.getParameter("username"));
        password = decode(request.getParameter("password"));
        payload = decode(request.getParameter("payload"));
    } catch (Exception e) {
    %>
    location.href = '../error.jsp?errorMsg=<%=e.getMessage()%>';
    <%
        return;
    }

    Options opts = new Options();
    //stops automatic retrying of the SC
    /*HttpMethodParams methodParams = new HttpMethodParams();
       methodParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
       opts.setProperty(HTTPConstants.HTTP_METHOD_PARAMS, methodParams);*/
    opts.setProperty(HTTPConstants.SO_TIMEOUT, 60 * 1000);
    opts.setProperty(HTTPConstants.CONNECTION_TIMEOUT, 60 * 1000);

    opts.setTo(new EndpointReference(uri));
    String optionsString = CharacterEncoder.getSafeText(request.getParameter("options"));
    if(optionsString != null) {
        String[] options = optionsString.split(",");
        for(String option : options){
            String decoded;
            try {
                decoded = decode(option);
            } catch (Exception e) {
            %>
            location.href = '../error.jsp?errorMsg=<%=e.getMessage()%>'
            <%
            return;
            }
            String optionName = decoded.split(":")[0];
            String optionValue = decoded.substring(optionName.length() + 1);

            if ("action".equals(optionName)) {
                opts.setAction(optionValue);
            } else if ("useBinding".equals(optionName)) {
                if (optionValue.equalsIgnoreCase("SOAP 1.1")) {
                    opts.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
                } else if (optionValue.equalsIgnoreCase("SOAP 1.2")) {
                    opts.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
                } else if (optionValue.equalsIgnoreCase("HTTP")) {
                    opts.setProperty(Constants.Configuration.ENABLE_REST, Constants.VALUE_TRUE);
                }
            } else if ("useSOAP".equals(optionName)) {
                if (optionValue.equalsIgnoreCase("1.1")) {
                    opts.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
                } else if ((optionValue.equalsIgnoreCase("1.2")) || (optionValue.equalsIgnoreCase("true"))) {
                    opts.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
                } else if (optionValue.equalsIgnoreCase("false")) {
                    opts.setProperty(Constants.Configuration.ENABLE_REST, Constants.VALUE_TRUE);
                }
            } else if ("HTTPInputSerialization".equals(optionName)) {
                opts.setProperty(WSDL2Constants.ATTR_WHTTP_INPUT_SERIALIZATION, optionValue);
                opts.setProperty(Constants.Configuration.MESSAGE_TYPE, optionValue);
            } else if ("HTTPQueryParameterSeparator".equals(optionName)) {
                opts.setProperty(WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, optionValue);
            } else if ("HTTPLocation".equals(optionName)) {
                opts.setProperty(WSDL2Constants.ATTR_WHTTP_LOCATION, optionValue);
            } else if ("HTTPMethod".equals(optionName)) {
                if (optionValue.equalsIgnoreCase("GET")) {
                    opts.setProperty(Constants.Configuration.HTTP_METHOD,
                                     Constants.Configuration.HTTP_METHOD_GET);
                } else if (optionValue.equalsIgnoreCase("POST")) {
                    opts.setProperty(Constants.Configuration.HTTP_METHOD,
                                     Constants.Configuration.HTTP_METHOD_POST);
                } else if (optionValue.equalsIgnoreCase("PUT")) {
                    opts.setProperty(Constants.Configuration.HTTP_METHOD,
                                     Constants.Configuration.HTTP_METHOD_PUT);
                } else if (optionValue.equalsIgnoreCase("DELETE")) {
                    opts.setProperty(Constants.Configuration.HTTP_METHOD,
                                     Constants.Configuration.HTTP_METHOD_DELETE);
                }
            } else if ("HTTPLocationIgnoreUncited".equals(optionName)) {
                opts.setProperty(WSDL2Constants.ATTR_WHTTP_IGNORE_UNCITED,
                                JavaUtils.isTrueExplicitly(optionValue));

            } else if ("useWSS".equals(optionName) && JavaUtils.isTrueExplicitly(optionValue)) {
                opts.setUserName(username);
                opts.setPassword(password);
                useWSS = true;
            }
        }
    }

    // Parse
    OMElement payloadElement = null;
    if (payload != null) {
        try {
            payloadElement = AXIOMUtil.stringToOM(payload);
        } catch (Exception e) {
            throw new Error("INVALID_INPUT_EXCEPTION. Invalid input was : " + payload);
        }
    }

    //creating service client
    ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
            .getAttribute(CarbonConstants.CLIENT_CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    opts.setManageSession(true);
    opts.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, request.getHeader("Cookie"));
    ServiceClient sc = new ServiceClient(configContext, null);
    sc.setOptions(opts);

    String body;
    try {
        if (useWSS) {
            sc.engageModule("rampart");
            sc.getServiceContext()
                    .setProperty("rampartPolicy", PolicyEngine.getPolicy(AXIOMUtil.stringToOM(policy)));
        }
        //invoke service
		if(WSDL2Constants.MEP_URI_IN_ONLY.equals(pattern)) {
			sc.fireAndForget(payloadElement);
			body = "<success details=\"in-only operation\"/>";
		} else if(WSDL2Constants.MEP_URI_ROBUST_IN_ONLY.equals(pattern)) {
			sc.sendRobust(payloadElement);
			body = "<success details=\"robust in-only operation\"/>";
		} else {
			OMElement responseMsg = sc.sendReceive(payloadElement);
        	body = responseMsg != null ? responseMsg.toString() : "<success details=\"empty response\"/>";
		}

    } catch (Exception exception) {
        OperationContext operationContext = sc.getLastOperationContext();
        if (operationContext != null) {
            MessageContext messageContext =
                    operationContext.getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN);
            if (messageContext != null) {
                SOAPEnvelope envelope = messageContext.getEnvelope();
                if (envelope != null) {
                    OMElement bodyElement = envelope.getBody().getFirstElement();
                    if(bodyElement != null) {
                        if ("Exception".equals(bodyElement.getLocalName())) {
                            OMNamespace ns = bodyElement.declareNamespace("http://wso2.org/ns/TryitProxy", "http");
                            bodyElement.addAttribute("h:status", "unknown error", ns);
                        }
                        body = bodyElement.toString();
                    } else {
                        body = "<TryitProxyError h:status='unknown error' xmlns:h='http://wso2.org/ns/TryitProxy'/>";
                    }
                } else body = "<TryitProxyError h:status='SOAP envelope error' xmlns:h='http://wso2.org/ns/TryitProxy'>" + exception.toString() + "</TryitProxyError>";
            }  else body = "<TryitProxyError h:status='messageContext error' xmlns:h='http://wso2.org/ns/TryitProxy'>" + exception.toString() + "</TryitProxyError>";
        } else body = "<TryitProxyError h:status='exception' xmlns:h='http://wso2.org/ns/TryitProxy'>" + exception.toString() + "</TryitProxyError>";
    } finally {
        sc.cleanupTransport();
    }

    /*
    // If there is a SOAP fault, we need to serialize that as the body.
    // If there is an HTTP error code, we need to report it using a similar structure.
    if (httpstatus != 20x && soapVer == "0") { // http error
        String httpStatus = "400";
        String httpStatusText = "Test HTTP error";
        body = "<error http:status='" + httpStatus + "' http:statusText='" + httpStatusText + "' xmlns:http='http://wso2.org/ns/WSRequest'>" + body + "</error>";
    }
    */

%>
<%= body %>

