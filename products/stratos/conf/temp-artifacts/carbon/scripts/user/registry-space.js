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

(function (server, registry, user) {

    var Space = function (user, space, options) {
        var serv = new server.Server(options.serverUrl);
        this.registry = new registry.Registry(serv, {
            username: options.username || user,
            domain: options.domain || server.tenantDomain()
        });
        this.prefix = options.path + '/' + user + '/' + space;
        if (!this.registry.exists(this.prefix)) {
            this.registry.put(this.prefix, {
                collection: true
            });
        }
    };
    user.Space = Space;

    Space.prototype.put = function (key, value) {
        value = (!(value instanceof String) && typeof value !== "string") ? stringify(value) : value;
        this.registry.put(this.prefix + '/' + key, {
            content: value
        });
    };

    Space.prototype.get = function (key) {
        var o = this.registry.content(this.prefix + '/' + key);
        return o ? o.toString() : null;
    };

    Space.prototype.remove = function (key) {
        this.registry.remove(this.prefix + '/' + key);
    };

    Space.prototype.find = function (filter) {

    };


}(server, registry, user));
