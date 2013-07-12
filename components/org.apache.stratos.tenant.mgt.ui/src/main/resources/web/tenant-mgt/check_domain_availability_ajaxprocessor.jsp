<%@ page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.wso2.carbon.stratos.common.util.CommonUtil" %>
<%@ page import="org.json.JSONException" %>
<%@ page import="org.json.JSONObject" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String tenantDomain = request.getParameter("domain");
    boolean isDomainAvailable = CommonUtil.isDomainNameAvailable(tenantDomain);
    JSONObject jsonObject = new JSONObject();

    if (isDomainAvailable) {
        jsonObject.put("available", "true");

    } else {
        jsonObject.put("available", "false");
    }
    out.println(jsonObject);

%>
