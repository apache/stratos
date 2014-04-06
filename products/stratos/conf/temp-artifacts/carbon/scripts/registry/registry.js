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

var registry = {};

(function (registry) {
    var ActionConstants = Packages.org.wso2.carbon.registry.core.ActionConstants,
        AccessControlConstants = Packages.org.wso2.carbon.registry.core.utils.AccessControlConstants;

    registry.Registry = function (server, auth) {
        var osgi = require('registry-osgi.js').registry,
            o = new osgi.Registry(server, auth);
        o.prototype = this;
        return o;
    };

    registry.actions = {};

    registry.actions.GET = ActionConstants.GET;

    registry.actions.PUT = ActionConstants.PUT;

    registry.actions.DELETE = ActionConstants.DELETE;

    registry.actions.AUTHORIZE = AccessControlConstants.AUTHORIZE;

}(registry));
