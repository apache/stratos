package org.apache.stratos.rest.endpoint.handlers;/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.StatusResponseBean;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/*
*Converts the badRequestException errors to appropriate json output messages. Introduced to
* converts the JAXBExceptions due to wrong input formats
**/
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {
    private static Log log = LogFactory.getLog(BadRequestExceptionMapper.class);

    public Response toResponse(BadRequestException badRequestException) {
        if (log.isDebugEnabled()) {
            log.debug("Error in input format", badRequestException);
        }
        String errorMsg = badRequestException.getMessage() != null ? badRequestException.getMessage() : "please check" +
                "your input format";
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).
                entity(new StatusResponseBean(Response.Status.BAD_REQUEST.getStatusCode(), errorMsg)).build();
    }
}
