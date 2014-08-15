/*
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

package org.apache.stratos.cartridge.agent.data.publisher.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.data.publisher.DataPublisher;
import org.apache.stratos.cartridge.agent.data.publisher.DataPublisherConfiguration;
import org.wso2.carbon.databridge.commons.StreamDefinition;

public abstract class LogPublisher extends DataPublisher {

    private static final Log log = LogFactory.getLog(LogPublisher.class);

    protected String memberId;
    protected String filePath;
    protected String tenantId;
    protected String alias;
    protected Long datetime;
    protected String serverName;

    public LogPublisher (DataPublisherConfiguration dataPublisherConfig, StreamDefinition streamDefinition, String filePath, String memberId, String tenantId, String alias, Long datetime) {

        super(dataPublisherConfig, streamDefinition);
        this.filePath = filePath;
        this.memberId = memberId;
        this.tenantId = tenantId;
        this.alias = alias;
        this.datetime = datetime;
    }

    public void start () {

    }

    public void stop () {

    }
}
