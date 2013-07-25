<!--
     ~  Licensed to the Apache Software Foundation (ASF) under one
     ~  or more contributor license agreements.  See the NOTICE file
     ~  distributed with this work for additional information
     ~  regarding copyright ownership.  The ASF licenses this file
     ~  to you under the Apache License, Version 2.0 (the
     ~  "License"); you may not use this file except in compliance
     ~  with the License.  You may obtain a copy of the License at
     ~
     ~    http://www.apache.org/licenses/LICENSE-2.0
     ~
     ~  Unless required by applicable law or agreed to in writing,
     ~  software distributed under the License is distributed on an
     ~  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
     ~  KIND, either express or implied.  See the License for the
     ~  specific language governing permissions and limitations
     ~  under the License.
     ~
 -->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="org.apache.stratos.cartridge.mgt.ui.CartridgeAdminClient"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon"%>
<%@ page import="org.apache.stratos.adc.mgt.dto.xsd.Cartridge"%>
<%@ page import="org.apache.stratos.adc.mgt.dto.xsd.CartridgeWrapper"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="java.util.ArrayList"%>

<jsp:include page="../dialog/display_messages.jsp" />

<%
	response.setHeader("Cache-Control", "no-cache");

    String pageNumber = request.getParameter("pageNumber");
    if (pageNumber == null) {
        pageNumber = "0";
    }
    int pageNumberInt = 0;
    try {
        pageNumberInt = Integer.parseInt(pageNumber);
    } catch (NumberFormatException ignored) {
    }

    String cartridgeSearchString = request.getParameter("cartridgeSearchString");
    if (cartridgeSearchString == null) {
        cartridgeSearchString = "";
    }
    
    // Fix issue when subscribed_cartridges_ajaxprocessor.jsp page return errors continuously 
    String reloadCount = request.getParameter("reload");
    if (reloadCount == null) {
    	reloadCount = "0";
    }
    int reloadCountInt = 0;
    try {
    	reloadCountInt = Integer.parseInt(reloadCount);
    } catch (NumberFormatException ignored) {
    }
    
    if (new Boolean(request.getParameter("loginStatus"))) {
    	// This is a login request
    	// Initialize the reload count to zero
    	reloadCountInt = 0;
    }
%>

<fmt:bundle basename="org.apache.stratos.cartridge.mgt.ui.i18n.Resources">
	<carbon:breadcrumb label="cartrigdes.subscribed.header" resourceBundle="org.apache.stratos.cartridge.mgt.ui.i18n.Resources"
		topPage="false" request="<%=request%>" />

	<script type="text/javascript">
		function searchCartridges() {
			document.searchForm.submit();
		}
	    function showCartridgeInfo(popupID, alias) {
	        CARBON.showPopupDialog(jQuery('#' + popupID).html(), "Cartridge Information for " + alias, 350, null, null, 400);
	    }
	    function unsubscribeCartridge(alias) {
	        if (alias) {
	            CARBON.showConfirmationDialog("<fmt:message key="cartridge.unsubscribe.prompt"></fmt:message>",
	                                          function() {
	                                              //location.href = './call_unsubscribe.jsp?cartridge_alias=' + alias;
	                                              postToBEService('unsubscribe_ajaxprocessor.jsp', 'cartridge_alias=' + alias, 'subscribed_cartridges.jsp');
	                                          }
	                    );
	        }
	    }
	    
	    function syncRepo(alias) {
	        if (alias) {
            	postToBEService('sync_repo_ajaxprocessor.jsp', 'cartridge_alias=' + alias);
	        }
	    }
	    
	    function removeDomain(alias) {
	        if (alias) {
            	postToBEService('remove_domain_ajaxprocessor.jsp', 'cartridge_alias=' + alias, 'subscribed_cartridges.jsp');
	        }
	    }
	</script>

	<div id="middle">
		<h2>
			<fmt:message key="cartrigdes.subscribed.header" />
		</h2>

		<div id="workArea">
			<form action="subscribed_cartridges.jsp" name="searchForm">
				<table class="styledLeft">
					<tr>
						<td>
							<table style="border: 0;">
								<tbody>
									<tr style="border: 0;">
										<td style="border: 0;"><fmt:message key="search.cartrigdes" /> <input type="text"
											name="cartridgeSearchString" value="<%=cartridgeSearchString != null ? cartridgeSearchString : ""%>" />&nbsp;
										</td>
										<td style="border: 0;"><a class="icon-link" href="#" style="background-image: url(images/search.gif);"
											onclick="searchCartridges(); return false;" alt="<fmt:message key="search"/>"> </a></td>
									</tr>
								</tbody>
							</table>
						</td>
					</tr>
				</table>
			</form>

			<p>&nbsp;</p>
			<div id="cartridges"></div>
		</div>
	</div>
	<script type="text/javascript">
		var refresh;
        jQuery(document).ready(function() {
            var refreshTable = function(){
            	jQuery.ajax({
                    url:'subscribed_cartridges_ajaxprocessor.jsp?pageNumber=<%=pageNumber%>&cartridgeSearchString=<%=cartridgeSearchString%>',
                    success:function(data){
                    	jQuery('#cartridges').html(data);
                    },
                    error:function(jqXHR, textStatus, errorThrown) {
                    	reloadPage();
                    }
                })
            }
            // Call this initially
            refreshTable();
            refresh = setInterval(refreshTable, 15000);
        });
        
        function stopRefreshTable() {
            if (refresh) {
                clearInterval(refresh);
            }
        }
        
        function reloadPage() {
        	stopRefreshTable();
        	<%
        	if (reloadCountInt <= 1) {
        	%>
        	// Reload page. If the session is not there, the page should redirect to login page.
        	location.href = 'subscribed_cartridges.jsp?reload=<%=++reloadCountInt%>';
        	<% 
        	}
        	%>
        }
	</script>
	<script type="text/javascript" src="js/jquery.blockUI-1.33.js"></script>
	<script type="text/javascript" src="js/common.js"></script>
</fmt:bundle>
