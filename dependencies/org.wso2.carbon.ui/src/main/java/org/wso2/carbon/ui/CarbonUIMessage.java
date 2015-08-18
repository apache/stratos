/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
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
 */
package org.wso2.carbon.ui;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;

public class CarbonUIMessage implements Serializable {

    private static final long serialVersionUID = 7464385412679479148L;
    
    public static final String ID = "carbonUIMessage";
    public static final String INFO = "info";
    public static final String ERROR = "error";
    public static final String WARNING = "warning";

    private String message;
    private String messageType;
    private Exception exception;
    private boolean showStackTrace = true;

    /**
     * TODO: Make this constructor private
     */
    public CarbonUIMessage(String message, String messageType) {
        this.message = message;
        this.messageType = messageType;
    }

    /**
     * TODO: Make this constructor private
     */
    public CarbonUIMessage(String message, String messageType, Exception exception) {
        this.message = message;
        this.messageType = messageType;
        this.exception = exception;
    }

    /**
     * TODO: Make this constructor private
     */
    public CarbonUIMessage(String message, String messageType, Exception exception,
            boolean showStackTrace) {
        this.message = message;
        this.messageType = messageType;
        this.exception = exception;
        this.showStackTrace = showStackTrace;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getMessage() {
        return message;
    }

    public Exception getException(){
        return exception;
    }

    public void setException(Exception exception){
        this.exception = exception;
    }
    
    public boolean isShowStackTrace() {
        return showStackTrace;
    }

    public void setShowStackTrace(boolean showStackTrace) {
        this.showStackTrace = showStackTrace;
    }

    /**
     * Creates CarbonUIMessage, set it to the session and redirect it to the given url
     *
     * @param message       meesage to be displayed
     * @param messageType   info, warning, error
     * @param request
     * @param response
     * @param urlToRedirect
     */
    public static void sendCarbonUIMessage(String message, String messageType, HttpServletRequest request,
                                           HttpServletResponse response, String urlToRedirect) throws IOException {
        CarbonUIMessage carbonMessage = new CarbonUIMessage(message, messageType);
        request.getSession().setAttribute(CarbonUIMessage.ID, carbonMessage);
        response.sendRedirect(urlToRedirect);
    }

    /***
     * Creates error CarbonUIMessage, set it to the session and redirect it to the given url with the exception object
     * @param message
     * @param messageType
     * @param request
     * @param response
     * @param urlToRedirect
     * @param exception
     * @return
     * @throws IOException
     */
    public static CarbonUIMessage sendCarbonUIMessage(String message, String messageType,
                                                      HttpServletRequest request, HttpServletResponse response,
                                                      String urlToRedirect, Exception exception) throws IOException {
        CarbonUIMessage carbonMessage = new CarbonUIMessage(message, messageType, exception);
        request.getSession().setAttribute(CarbonUIMessage.ID, carbonMessage);
        response.sendRedirect(urlToRedirect);
        return carbonMessage;
    }

    /**
     * Creates CarbonUIMessage, set it to the session
     * @param message
     * @param messageType
     * @param request
     */
    public static void sendCarbonUIMessage(String message, String messageType, HttpServletRequest request) {
        CarbonUIMessage carbonMessage = new CarbonUIMessage(message, messageType);
        request.getSession().setAttribute(CarbonUIMessage.ID, carbonMessage);
    }

    /**
     * Creates error CarbonUIMessage, set it to the session
     * @param message
     * @param messageType
     * @param request
     * @param exception
     */
    public static void sendCarbonUIMessage(String message, String messageType,
                                           HttpServletRequest request, Exception exception) {
        CarbonUIMessage carbonMessage = new CarbonUIMessage(message, messageType, exception);
        request.getSession().setAttribute(CarbonUIMessage.ID, carbonMessage);
    }
}
