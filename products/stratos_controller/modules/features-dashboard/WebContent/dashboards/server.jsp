<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ WSO2 Inc. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 -->
<%@ page import="org.wso2.stratos.manager.feature.dashbord.ui.beans.Data"%>
<%@ page import="org.wso2.stratos.manager.feature.dashbord.ui.beans.Service"%>
<%@ page import="org.wso2.stratos.manager.feature.dashbord.ui.beans.Link"%>
<%@ page import="org.wso2.stratos.manager.feature.dashbord.ui.beans.Story"%>
<%@ page import="org.wso2.stratos.manager.feature.dashbord.ui.utils.Utils"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.Map"%>

<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<jsp:useBean id="serviceData" class="org.wso2.stratos.manager.feature.dashbord.ui.beans.Data"
             scope="session"></jsp:useBean>
<link type="text/css"
	href="../../../../features-dashboard/dashboards/css/features-dashboard.css"
	rel="stylesheet" />
	<%
		
	%>
<script type="text/javascript">
	function goToFunction(url,serverUrl) {
		var serverUrl = serverUrl+linkSuffix+url;
		window.open(serverUrl);
	}
</script>
<%
	String serviceName = request.getParameter("name");
	Service service = serviceData.getService(serviceName);
	String name1 = service.getName();
	String link = service.getLink();
	Map<String, Story> stories = service.getStories();
	int index = 1;
	for (Map.Entry<String, Story> entry : stories.entrySet()) {
		Story tempStory = entry.getValue();
		String storyName = tempStory.getTitle();
		String storyContent = tempStory.getContent();
		Map<String, Link> links = tempStory.getLinks();
		String divClassName;
		if (index%2 == 0) {
			divClassName = "story col" + 1;
		} else {
			divClassName = "story col" + 2;
		}
		index++;
	
%>
<div class="<%=divClassName%>">
	<div class="story-title"><%=storyName%></div>
	<div class="story-content"><%=storyContent%></div>
	<div class="story-links">
		<%
			for (Map.Entry<String, Link> linksEntry : links.entrySet()) {
					Link tempLink = linksEntry.getValue();
					String linkUrl = tempLink.getUrl();
					String value = tempLink.getDescription();
		%>
		<a href="javascript:goToFunction('<%=linkUrl%>','<%=link%>')" class="blocklink"><%=value%></a>
		<%
			}
		%>
	</div>
</div>
<%
	}
%>

<!-- 		<div class="story col1"> -->

<!-- 			<div class="story-title">Service Hosting</div> -->


<!-- 			<div class="story-content">Different types of Web Services such -->
<!-- 				as Axis2 Services, JAXWS Services, Jar Services or Spring Services -->
<!-- 				can be deployed in Application Server. All configurations such as -->
<!-- 				QoS can be easily configured here.</div> -->
<!-- 			<div class="story-links"> -->
<!-- 				<a href="javascript:generateAsFeatureUrl(0)" class="blocklink">Got -->
<!-- 					to services...</a> <a href="javascript:generateAsFeatureUrl(1)">Read -->
<!-- 					more (docs)...</a> -->
<!-- 			</div> -->
<!-- 		</div> -->

<!-- 		<div class="story col2"> -->
<!-- 			<div class="story-title">Web Applications</div> -->


<!-- 			<div class="story-content">Web Application hosting features in -->
<!-- 				AppServer supports deployment of Tomcat compliant Webapps. Deployed -->
<!-- 				Webapps can be easily managed using the Webapp management facilities -->
<!-- 				available in the management console.</div> -->
<!-- 			<div class="story-links"> -->
<!-- 				<a href="javascript:generateAsFeatureUrl(2)" target="_blank">Go -->
<!-- 					to Web Applications...</a> <a href="javascript:generateAsFeatureUrl(3)" -->
<!-- 					target="_blank">Read more (docs)...</a> -->
<!-- 			</div> -->
<!-- 		</div> -->


<!-- 		<div class="story col1"> -->
<!-- 			<div class="story-title">Message Tracing</div> -->
<!-- 			<div class="story-content">Trace the request and responses to -->
<!-- 				your service. Message Tracing is a vital debugging tool when you -->
<!-- 				have clients from heterogeneous platforms.</div> -->
<!-- 			<div class="story-links"> -->
<!-- 				<a href="javascript:generateAsFeatureUrl(4)">Go to Message -->
<!-- 					Tracing...</a> <a href="javascript:generateAsFeatureUrl(5)">Read -->
<!-- 					more (docs)...</a> -->
<!-- 			</div> -->

<!-- 		</div> -->

<!-- 		<div class="story col2"> -->
<!-- 			<div class="story-title">WSDL2Java Tool</div> -->
<!-- 			<div class="story-content">Use WSDL2Java tool in Web -->
<!-- 				Application Server to convert Web Service WSDL to a set of Java -->
<!-- 				objects.</div> -->
<!-- 			<div class="story-links"> -->
<!-- 				<a href="javascript:generateAsFeatureUrl(6)">Go to WSDL2Java -->
<!-- 					Tool...</a> <a href="javascript:generateAsFeatureUrl(7)">Read more -->
<!-- 					(docs)...</a> -->
<!-- 			</div> -->
<!-- 		</div> -->

<!-- 		<div class="story col1"> -->
<!-- 			<div class="story-title">Java2WSDL Tool</div> -->

<!-- 			<div class="story-content">Use Java2WSDL tool in Web -->
<!-- 				Application Server make it easy to develop a new web service.</div> -->
<!-- 			<div class="story-links"> -->
<!-- 				<a href="javascript:generateAsFeatureUrl(8)">Go to Java2WSDL -->
<!-- 					Tool...</a> <a href="javascript:generateAsFeatureUrl(9)">Read more -->
<!-- 					(docs)...</a> -->
<!-- 			</div> -->
<!-- 		</div> -->