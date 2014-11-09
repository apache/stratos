/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.rest.endpoint.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.rest.endpoint.Utils;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/*
* Stratos admin APIs' throw {@link RestAPIException} upon failure. This Class
* maps such exceptions to appropriate JSON output
* */
public class CustomExceptionMapper implements ExceptionMapper<RestAPIException> {
    private static Log log = LogFactory.getLog(CustomExceptionMapper.class);

    public Response toResponse(RestAPIException restAPIException) {
        if(log.isDebugEnabled()){
            log.debug("Error while invoking the admin rest api", restAPIException);
        }
        // if no specific error message specified, spitting out a generaic error message
        String errorMessage = (restAPIException.getMessage() != null)?
                restAPIException.getMessage():"Error while fulfilling the request";
        // if no specific error specified we are throwing the bad request http status code by default
        Response.Status httpStatus= (restAPIException.getHTTPStatusCode() != null)?
                restAPIException.getHTTPStatusCode():Response.Status.BAD_REQUEST;
           
        log.error(errorMessage, restAPIException);
        return Response.status(httpStatus.getStatusCode()).type(MediaType.APPLICATION_JSON).
                entity(Utils.buildMessage(httpStatus.getStatusCode(),errorMessage)).build();
    }
}
