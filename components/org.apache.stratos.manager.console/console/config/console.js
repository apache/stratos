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

var config = function () {
    var log = new Log(),
        pinch = require('/modules/pinch.min.js').pinch,
        config = require('/config/console.json'),
        process = require('process'),
        localIP = process.getProperty('server.host'),
        httpPort = process.getProperty('http.port'),
        httpsPort = process.getProperty('https.port');

    pinch(config, /^/, function (path, key, value) {
        if ((typeof value === 'string') && value.indexOf('%https.host%') > -1) {
            return value.replace('%https.host%', 'https://' + localIP + ':' + httpsPort);
        } else if ((typeof value === 'string') && value.indexOf('%http.host%') > -1) {
            return value.replace('%http.host%', 'http://' + localIP + ':' + httpPort);
        }
        return  value;
    });
    return config;
};
