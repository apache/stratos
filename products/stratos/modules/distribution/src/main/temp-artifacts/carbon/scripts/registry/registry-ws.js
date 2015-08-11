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

var log = new Log();

var Registry = function (server) {
    this.server = server;
};

var Resource = function (name) {

};

var Collection = function (name) {

};

Registry.prototype.invoke = function (action, payload) {
    var options,
        ws = require('ws'),
        client = new ws.WSRequest(),
        server = this.server;

    options = {
        useSOAP: 1.2,
        useWSA: 1.0,
        action: action,
        HTTPHeaders: [
            { name: 'Cookie', value: server.cookie }
        ]
    };

    try {
        client.open(options, server.url + '/services/WSRegistryService', false);
        client.send(payload);
        return client.responseXML;
    } catch (e) {
        log.error(e.toString());
        throw new Error('Error while invoking action in WSRegistryService : ' +
            action + ', user : ' + server.user.username);
    }
};

Registry.prototype.putResource = function (path, resource) {

};

Registry.prototype.getResource = function (path) {
    var res, payload,
        base64 = require('/modules/base64.js');

    payload =
        <api:getContent xmlns:api="http://api.ws.registry.carbon.wso2.org">
            <api:path>{path}</api:path>
        </api:getContent>;

    res = this.invoke('urn:getContent', payload);
    return base64.decode(String(res.*::['return'].text()));
};
