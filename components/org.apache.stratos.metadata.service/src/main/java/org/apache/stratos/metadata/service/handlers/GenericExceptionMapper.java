/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.metadata.service.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadata.service.Utils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/*
 * This class maps any exception thrown by the server, which is not mapped by a
 * specifi exception mapper
 * in to an appropriate format
 */
public class GenericExceptionMapper implements ExceptionMapper<WebApplicationException> {
    private static Log log = LogFactory.getLog(GenericExceptionMapper.class);

    @Override
    public Response toResponse(WebApplicationException webApplicationException) {
        if (log.isDebugEnabled()) {
            log.debug("Internal erver error", webApplicationException);
        }
        // if no specific error message specified, spitting out a generaic error
        // message
        String errorMessage =
                (webApplicationException.getMessage() != null)
                        ? webApplicationException.getMessage()
                        : "Internal server error";
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Utils.buildMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        errorMessage)).build();
    }
}
