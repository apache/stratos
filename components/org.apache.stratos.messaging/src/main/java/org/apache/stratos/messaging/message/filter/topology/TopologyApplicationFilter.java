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
 * A filter to discard topology events which are not in a given application id list.
 */
public class TopologyApplicationFilter extends MessageFilter {

    private static final Log log = LogFactory.getLog(TopologyServiceFilter.class);

    public static final String TOPOLOGY_APPLICATION_FILTER_APPLICATION_ID = "application-id";

    private static volatile TopologyApplicationFilter instance;

    public TopologyApplicationFilter() {
        super(StratosConstants.TOPOLOGY_APPLICATION_FILTER);
    }

    /**
     * Returns true if application is excluded else returns false.
     *
     * @param applicationId
     * @return
     */
    public static boolean apply(String applicationId) {
        boolean excluded = false;
        if (getInstance().isActive()) {
            if (StringUtils.isNotBlank(applicationId) && getInstance().applicationExcluded(applicationId)) {
                excluded = true;
            }
            if (excluded && log.isInfoEnabled()) {
                log.info(String.format("Application is excluded: [application-id] %s", applicationId));
            }
        }
        return excluded;
    }

    public static TopologyApplicationFilter getInstance() {
        if (instance == null) {
            synchronized (TopologyApplicationFilter.class) {
                if (instance == null) {
                    instance = new TopologyApplicationFilter();
                    if (log.isDebugEnabled()) {
                        log.debug("Topology application filter instance created");
                    }
                }
            }
        }
        return instance;
    }

    private boolean applicationExcluded(String value) {
        return excluded(TOPOLOGY_APPLICATION_FILTER_APPLICATION_ID, value);
    }

    private Collection<String> getIncludedApplicationIds() {
        return getIncludedPropertyValues(TOPOLOGY_APPLICATION_FILTER_APPLICATION_ID);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TOPOLOGY_APPLICATION_FILTER_APPLICATION_ID + "=");
        for (String applicationId : TopologyApplicationFilter.getInstance().getIncludedApplicationIds()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(applicationId);
        }
        return sb.toString();
    }
}
