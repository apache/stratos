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

package org.wso2.carbon.ui.util;


import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;

import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.Header;

public class CarbonUIAuthenticationUtil {

    /**
     * Sets the cookie information, i.e. whether remember me cookie is enabled of disabled. If enabled
     * we will send that information in a HTTP header.
     * @param cookie  The remember me cookie.
     * @param serviceClient The service client used in communication.
     */
    public static void setCookieHeaders(Cookie cookie, ServiceClient serviceClient) {

        List<Header> headers = new ArrayList<Header>();
        Header rememberMeHeader = new Header("RememberMeCookieData", cookie.getValue());
        headers.add(rememberMeHeader);

        serviceClient.getOptions().setProperty(HTTPConstants.HTTP_HEADERS, headers);
    }

}
