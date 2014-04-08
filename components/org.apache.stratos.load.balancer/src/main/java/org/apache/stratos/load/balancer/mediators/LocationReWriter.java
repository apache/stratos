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
package org.apache.stratos.load.balancer.mediators;

import org.apache.commons.lang3.StringUtils;
import org.apache.stratos.load.balancer.util.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import java.net.URL;
import java.util.Map;

/**
 * Re-write HTTP Location header with load balancer hostname and port.
 */
public class LocationReWriter extends AbstractMediator {

    public static final String LOCATION = "Location";
    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    @Override
    public boolean mediate(MessageContext messageContext) {
        try {
            // Read transport headers
            Map transportHeaders = (Map) ((Axis2MessageContext) messageContext).getAxis2MessageContext().
                    getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (transportHeaders != null) {
                // Find location header
                String inLocation = (String) transportHeaders.get(LOCATION);
                if(StringUtils.isNotBlank(inLocation)) {
                    URL inLocationUrl = new URL(inLocation);
                    // Find load balancer host name and port
                    String lbHost = (String) messageContext.getProperty(Constants.LB_HOST_NAME);
                    int lbPort = -1;
                    if (HTTP.equals(inLocationUrl.getProtocol())) {
                        lbPort = Integer.valueOf((String) messageContext.getProperty(Constants.LB_HTTP_PORT));
                    } else if (HTTPS.equals(inLocationUrl.getProtocol())) {
                        lbPort = Integer.valueOf((String) messageContext.getProperty(Constants.LB_HTTPS_PORT));
                    } else {
                        log.warn(String.format("An unknown protocol found: %s", inLocationUrl.getProtocol()));
                    }

                    if (lbPort != -1) {
                        // Re-write location header
                        URL outLocationUrl = new URL(inLocationUrl.getProtocol(), lbHost, lbPort, inLocationUrl.getFile());
                        transportHeaders.put(LOCATION, outLocationUrl.toString());
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Location header re-written: %s", outLocationUrl.toString()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could re-write location header", e);
            }
        }
        return true;
    }
}
