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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.apache.stratos.cartridge.mgt.ui.CartridgeAdminClient" %>
<%@ page import="org.apache.stratos.adc.mgt.dto.xsd.PolicyDefinition" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.ResourceBundle" %>
<jsp:include page="../dialog/display_messages.jsp"/>


<%
    response.setHeader("Cache-Control", "no-cache");
    String cartridgeType = request.getParameter("cartridgeType");
    String multiTenantValue = request.getParameter("multiTenant");
    String cartridgeProvider = request.getParameter("cartridgeProvider");
    boolean multiTenant = Boolean.valueOf(multiTenantValue);
    String item = request.getParameter("item");
    
    PolicyDefinition[] policyDefinitions = null;

    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
          (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    CartridgeAdminClient client;
    
    boolean internalRepoFeatureEnabled = false;

    try{
        client = new CartridgeAdminClient(cookie, backendServerURL, configContext,request.getLocale());
        policyDefinitions = client.getPolicyDefinitions();
        
        if (policyDefinitions == null || policyDefinitions.length == 0) {
        	throw new IllegalStateException("Policy Definitions not found.");
        }
        internalRepoFeatureEnabled = client.isFeatureEnabled("feature.internalrepo.enabled");
    }catch (Exception e) {
        response.setStatus(500);
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
        %>
            <jsp:include page="../admin/error.jsp"/>
        <%
        return;
    }

%>
<fmt:bundle basename="org.apache.stratos.cartridge.mgt.ui.i18n.Resources">
<carbon:breadcrumb
    label="subscribe"
    resourceBundle="org.apache.stratos.cartridge.mgt.ui.i18n.Resources"
    topPage="true"
    request="<%=request%>"/>
<div id="middle">
<%if(cartridgeType != null) {%>
    <h2><fmt:message key="subscribe.to"><fmt:param value="<%= cartridgeType%>"/></fmt:message></h2>
<%} else {%>
	<h2><fmt:message key="subscribe.new.cartridge"/></h2>
    <p>&nbsp;</p>
<%} %>
<div id="workArea">
    <form id="subscribeForm" name="subscribeToCartridge">
        <table id="subscribeToCartridgeTbl" width="100%" class="styledLeft">
	        <thead>
	        <tr>
	            <th>
	                <fmt:message key="cartridge.information"/>
	            </th>
	        </tr>
	        </thead>
            <tbody>
            <tr>
            <td class="nopadding">
           	<table class="normal-nopadding" cellspacing="0">
            <tbody>
                <tr>
                	<!-- Keep 30% width to give more space for inputs -->
                    <td style="width: 30%"><label>Cartridge Type</label><span class="required">*</span></td>
                    <td>
                        <span><%=cartridgeType%></span>
                        <input name="cartridge_type" type="hidden" value="<%=cartridgeType%>"/>
                        <input name="cartridge_provider" type="hidden" value="<%=cartridgeProvider%>"/>
                    </td>
                </tr>
                <tr>
                    <td><label for="cartridge_alias">Alias</label><span class="required">*</span></td>
                    <td><input id="cartridge_alias" name="cartridge_alias" type="text" style="width: 400px" maxlength="300" /></td>
                </tr>
                <%if((cartridgeProvider != null && !cartridgeProvider.equalsIgnoreCase("data")) && !multiTenant) {%>
                    <tr>
                    	<td><label for="policy">Policy</label><span class="required">*</span></td>
						<td style="border: 0;"><select name="policy" id="policy">
								<%
									for (PolicyDefinition policy : policyDefinitions) {
												
								%>
								<option value="<%=policy.getName()%>" <%if (policy.getDefaultPolicy()) {%>selected="selected"<%}%>>
									<%=policy.getDescription()%>
								</option>
								<%
									}
								%>
						</select></td>
                    </tr>
                <%}%>
                <%if((cartridgeProvider != null && !cartridgeProvider.equalsIgnoreCase("data"))) {%>
               		<tr>
                        <td colspan="2" class="middle-header"><fmt:message key="repository.information"/></td>
                    </tr>
                	<%if(internalRepoFeatureEnabled) {%>
                	<tr>
                        <td><label for="internalRepo" id="internalRepoLabel">Use Internal Repository</label><span class="required">*</span></td>
                        <td>
                        	<input name="internalRepo" type="radio" value="yes" /> Yes
							<input name="internalRepo" type="radio" value="no" checked="checked" /> No
						</td>
                    </tr>
                    <%} %>
                    <tr class="repoDetails">
                        <td><label for="repoType" id="repoTypeLabel">Repository Type</label><span class="required">*</span></td>
                        <td>
                        	<input name="repoType" type="radio" value="public" checked="checked" /> Public
							<input name="repoType" type="radio" value="private" /> Private
						</td>
                    </tr>
                    <tr class="repoDetails">
                        <td><label for="repo_url" id="repoLabel">GIT repository URL: (http:// or https://)</label><span class="required">*</span></td>
                        <td><input id="repo_url" name="repo_url" type="text" style="width: 400px" maxlength="1000" />
                        </td>
                    </tr>
                    <tr class="repoDetails repoCredentials">
                        <td><label for="repo_username" id="repo_usernameLabel">GIT Repository Username</label><span class="required" style="display: none;">*</span></td>
                        <td><input id="repo_username" name="repo_username" type="text" style="width: 400px" maxlength="500" autocomplete="off" /></td>
                    </tr>
                    <tr class="repoDetails repoCredentials">
                        <td><label for="repo_password" id="repo_passwordLabel">GIT Repository Password</label><span class="required" style="display: none;">*</span></td>
                        <td><input id="repo_password" name="repo_password" type="password" style="width: 400px" maxlength="500" autocomplete="off" /></td>
                    </tr>
                    <tr class="repoDetails" id="testConnectionRow">
                    	<td></td>
                    	<td>
                    		<input id="testGitButton" type="button" class="button" value="Test Connection"
                        		onclick="postToBEService('test_git_ajaxprocessor.jsp', jQuery('#subscribeForm').serialize(), null, '#testGitButton', validateRepository);">
                       	</td>
                    </tr>
                    <tr class="dataCartridgeRow" style="display: none;">
                        <td colspan="2" class="middle-header"><fmt:message key="cartridge.connect.information"/></td>
                    </tr>
                    <tr class="dataCartridgeRow" style="display: none;">
                        <td><label for="other_cartridge_type">Connect Data Cartridge</label><span class="required">*</span></td>
                        <td><select id="other_cartridge_type" name="other_cartridge_type" ><option value="mysql">mysql</option></select></td>
                    </tr>
                    <tr class="dataCartridgeRow" style="display: none;">
                        <td><label for="other_alias">Data Cartridge Alias</label><span class="required">*</span></td>
                        <td><input id="other_alias" name="other_alias" type="text" style="width: 400px" maxlength="300" /></td>
                    </tr>
                <%}%>
            </tbody>
            </table>
            </td>
        </tr>
        <tr id="buttonRow" class="buttonRow">
            <td>
                <input id="subscribeButton" type="submit" class="button" value="Subscribe" />
                <input id="subscribeCancelButton" type="button" class="button" value="Cancel" onclick="cancelSubscribe('<%=item%>');" />
                <%if(!"data".equalsIgnoreCase(cartridgeProvider) && !multiTenant){%>
                    <a id="showConnect" onclick="showConnectCartridge();" style="cursor: pointer;">Connect another cartridge...</a>
                    <a id="hideConnect" onclick="hideConnectCartridge();" style="cursor: pointer; display: none;">Hide connecting cartridge fields...</a>
                <%}%>
            </td>
        </tr>
        </tbody>
        </table>
    </form>

    <p>&nbsp;</p>
    </div>

</div>

<script type="text/javascript">
	jQuery(document).ready(
		function() {
			if (jQuery("#internalRepoLabel").length > 0) {
				jQuery('input:radio[name="internalRepo"]').click(
					function() {
						var $this = jQuery(this);
						if ($this.val() == "no") {
							jQuery(".repoDetails").show();
						} else {
							jQuery(".repoDetails").hide();
							jQuery("#repo_url").val("");
							jQuery("#repo_username").val("");
							jQuery("#repo_password").val("");
						}
					})
			};
	
			jQuery('input:radio[name="repoType"]').click(
				function() {
					var $this = jQuery(this);
					if ($this.val() == "private") {
						jQuery(".repoCredentials .required").show();
					} else {
						jQuery(".repoCredentials .required").hide();
					}
				})
			
			setStratosFormSubmitFunction("#subscribeForm", validate, "subscribe_ajaxprocessor.jsp", "subscribed_cartridges.jsp", "#subscribeButton");
		});

	function validate() {
		if (jQuery("#cartridge_alias").val().length == 0) {
			CARBON.showErrorDialog('Please enter an alias for the cartridge.');
			return false;
		}
		if (!validateRepository()) {
			return false;
		}
		if (jQuery(".dataCartridgeRow").length > 0 && 
				jQuery(".dataCartridgeRow").css("display") != "none" && jQuery("#other_alias").val().length == 0) {
			CARBON.showErrorDialog('Please enter an alias for the data cartridge.');
			return false;
		}
		return true;
	}

	function validateRepository() {
		var repoRequired = false;
		if (jQuery("#internalRepoLabel").length > 0) {
			if (jQuery('input[name="internalRepo"]:checked').val() == "no") {
				repoRequired = true;
				if (jQuery("#repo_url").val().length == 0) {
					CARBON.showWarningDialog('Please enter GIT repository URL.');
					return false;
				}
			}
		} else {
			// Some cartridges do not have a repo URL
			if (jQuery("#repo_url").length > 0) {
				repoRequired = true;
				if (jQuery("#repo_url").val().length == 0) {
					CARBON.showWarningDialog('Please enter GIT repository URL.');
					return false;
				}
			}
		}

		if (repoRequired && jQuery("#repoTypeLabel").length > 0
				&& jQuery('input[name="repoType"]:checked').val() == "private") {
			if (jQuery("#repo_username").val().length == 0) {
				CARBON.showWarningDialog('Please enter GIT repository username.');
				return false;
			}
			if (jQuery("#repo_password").val().length == 0) {
				CARBON.showWarningDialog('Please enter GIT repository password.');
				return false;
			}
		}
		return true;
	}

	function showConnectCartridge() {
		jQuery(".dataCartridgeRow").show();
		jQuery("#showConnect").hide();
		jQuery("#hideConnect").show();
	}

	function hideConnectCartridge() {
		jQuery(".dataCartridgeRow").hide();
		jQuery("#showConnect").show();
		jQuery("#hideConnect").hide();
	}
	
	function cancelSubscribe(item) {
		location.href = "available_cartridges.jsp?item=" + item;
	}
</script>
<script type="text/javascript" src="js/jquery.blockUI-1.33.js"></script>
<script type="text/javascript" src="js/common.js"></script>
</fmt:bundle>
