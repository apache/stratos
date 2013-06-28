<%@ page import="org.wso2.carbon.validate.domain.ui.clients.ValidateDomainClient" %>
<%@ page import="org.wso2.carbon.validate.domain.ui.utils.Util" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String domain = request.getParameter("domain");
    String validationSuccessKey;

    try {
        ValidateDomainClient client = Util.getValidateDomainClient(request, config, session);
        validationSuccessKey = client.validateByTextInRoot(domain);
    } catch (Exception e) {
        validationSuccessKey = null;
    }

    if (validationSuccessKey == null || validationSuccessKey.equals("") ||
            validationSuccessKey.equals("false") || validationSuccessKey.equals("null")) {
        validationSuccessKey = "----false----";
    }
    else if (validationSuccessKey.equals("unavailable")) {
        validationSuccessKey = "----unavailable----";
    }
%><%=validationSuccessKey%>