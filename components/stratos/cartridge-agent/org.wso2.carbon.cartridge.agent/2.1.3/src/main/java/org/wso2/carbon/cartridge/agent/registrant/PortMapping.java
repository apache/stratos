/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.cartridge.agent.registrant;

import java.io.Serializable;

/**
 * This class provides the mapping between the actual port &amp; proxy port of a {@link org.wso2.carbon.cartridge.agent.registrant.Registrant}
 *
 * e.g. if the Registrant is serving HTTP requests on port 9763, and the requests to that port have
 * to be proxied via port 80 on the ELB, the <code>primaryPort</code> has to be specified as 9763
 * &amp; <code>proxyPort</code> has to be specified as 80.
 *
 * @see org.wso2.carbon.cartridge.agent.registrant.Registrant
 */
@SuppressWarnings("unused")
public class PortMapping implements Serializable {

    private static final long serialVersionUID = 8020727939469156788L;
    public static final String PORT_TYPE_HTTP = "http";
    public static final String PORT_TYPE_HTTPS = "https";

    private int primaryPort;
    private int proxyPort;
    private String type;

    public PortMapping() {
    }

    public int getPrimaryPort() {
        return primaryPort;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setPrimaryPort(int primaryPort) {
        this.primaryPort = primaryPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type.equalsIgnoreCase(PORT_TYPE_HTTP) && type.equalsIgnoreCase(PORT_TYPE_HTTPS)) {
            throw new RuntimeException("Invalid port type " + type);
        }
        this.type = type;
    }

    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PortMapping that = (PortMapping) o;
        return (type.equalsIgnoreCase(that.type) &&
               (primaryPort == that.primaryPort) &&
               (proxyPort == that.proxyPort));
    }

    @Override
    public int hashCode() {
        return type.hashCode() +
                Integer.toString(primaryPort).hashCode() +
                Integer.toString(proxyPort).hashCode();
    }
}
