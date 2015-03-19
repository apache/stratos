/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.message.filter.topology;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.message.filter.MessageFilter;

import java.util.Collection;

/**
 * A filter to discard topology events which are not in a given service name list.
 */
public class TopologyServiceFilter extends MessageFilter {

    private static final Log log = LogFactory.getLog(TopologyServiceFilter.class);

    public static final String TOPOLOGY_SERVICE_FILTER_SERVICE_NAME = "service-name";

    private static volatile TopologyServiceFilter instance;

    public TopologyServiceFilter() {
        super(StratosConstants.TOPOLOGY_SERVICE_FILTER);
    }

    /**
     * Returns true if service is excluded else returns false.
     *
     * @param serviceName service name
     * @return
     */
    public static boolean apply(String serviceName) {
        boolean excluded = false;
        if (getInstance().isActive()) {
            if (StringUtils.isNotBlank(serviceName) && getInstance().serviceExcluded(serviceName)) {
                excluded = true;
            }
            if (excluded && log.isDebugEnabled()) {
                log.debug(String.format("Service is excluded: [lb-cluster] %s", serviceName));
            }
        }
        return excluded;
    }

    public static TopologyServiceFilter getInstance() {
        if (instance == null) {
            synchronized (TopologyServiceFilter.class) {
                if (instance == null) {
                    instance = new TopologyServiceFilter();
                    if (log.isDebugEnabled()) {
                        log.debug("Topology service filter instance created");
                    }
                }
            }
        }
        return instance;
    }

    private boolean serviceExcluded(String value) {
        return excluded(TOPOLOGY_SERVICE_FILTER_SERVICE_NAME, value);
    }

    private Collection<String> getIncludedServiceNames() {
        return getIncludedPropertyValues(TOPOLOGY_SERVICE_FILTER_SERVICE_NAME);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TOPOLOGY_SERVICE_FILTER_SERVICE_NAME + "=");
        for (String clusterId : getInstance().getIncludedServiceNames()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(clusterId);
        }
        return sb.toString();
    }
}
