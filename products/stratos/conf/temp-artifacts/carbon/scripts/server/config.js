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

(function (server) {
    var process = require('process'),
        configDir = 'file:///' + process.getProperty('carbon.config.dir.path').replace(/[\\]/g, '/').replace(/^[\/]/g, '') + '/';
    server.loadConfig = function (path) {
        var content,
            index = path.lastIndexOf('.'),
            ext = (index !== -1 && index < path.length) ? path.substring(index + 1) : '',
            file = new File(configDir + path);
        if (!file.isExists()) {
            throw new Error('Specified config file does not exists : ' + path);
        }
        if (file.isDirectory()) {
            throw new Error('Specified config file is a directory : ' + path);
        }
        file.open('r');
        content = file.readAll();
        file.close();
        switch (ext) {
            case 'xml' :
                return new XML(content);
            case 'json' :
                return parse(content);
            case 'properties' :
            default :
                return content;

        }
    };

    server.home = 'file:///' + require('process').getProperty('carbon.home').replace(/[\\]/g, '/').replace(/^[\/]/g, '');

}(server));
