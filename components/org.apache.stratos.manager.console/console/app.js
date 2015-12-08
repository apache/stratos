/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

var caramel = require('caramel'),
    carbon = require('carbon'),
    server = new carbon.server.Server(),
    conf = carbon.server.loadConfig('thrift-client-config.xml'),
    dasConfig = conf. *::['config']. *::['das'],
    dasStatsPublisherEnabled = false;

caramel.configs({
    context: '/console',
    cache: true,
    negotiation: true,
    themer: function () {
        return 'theme0';
    }
});

//checking whether das stats publisher is enabled
for (var i = 0; i < dasConfig.node.length(); i++) {
    dasStatsPublisherEnabled = dasConfig.node[i].statsPublisherEnabled.text();
    if (dasStatsPublisherEnabled == true) {
        break;
    }
}
application.put("dasStatsPublisherEnabled", dasStatsPublisherEnabled);

//reading metering and monitoring dashboard urls from cartridge-config.properties file
var cartridgeConfig = carbon.server.loadConfig("cartridge-config.properties");
var prop = new java.util.Properties();
var inputStream = new java.io.ByteArrayInputStream(new java.lang.String(cartridgeConfig).getBytes());
prop.load(inputStream);
application.put("meteringDashboardUrl", prop.getProperty("das.metering.dashboard.url"));
application.put("monitoringDashboardUrl", prop.getProperty("das.monitoring.dashboard.url"));


