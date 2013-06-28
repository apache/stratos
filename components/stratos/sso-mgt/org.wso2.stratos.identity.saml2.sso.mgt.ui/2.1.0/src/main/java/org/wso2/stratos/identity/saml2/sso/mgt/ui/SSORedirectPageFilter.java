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

package org.wso2.stratos.identity.saml2.sso.mgt.ui;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * This class is used to filter the requests coming for sso-saml/redirect_ajaxprocessor.jsp
 * and send them through stratos-sso/redirect_ajaxprocessor.jsp. The intention is to apply
 * the stratos look and feel to the redirect jsp.
 */
public class SSORedirectPageFilter implements Filter{

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest)) {
            return;
        }
        String url = ((HttpServletRequest) servletRequest).getRequestURI();
        url = url.replace("sso-saml/redirect_ajaxprocessor.jsp", "stratos-sso/redirect_ajaxprocessor.jsp");
        RequestDispatcher requestDispatcher =
                servletRequest.getRequestDispatcher(url);
        requestDispatcher.forward(servletRequest, servletResponse);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // not required to implement for this class
    }

    public void destroy() {
        // not required to implement for this class
    }
}
