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

var engine = require('caramel').engine('handlebars', (function () {
    return {
        partials: function (Handlebars) {
            var theme = caramel.theme();
            var partials = function (file) {
                (function register(prefix, file) {
                    var i, length, name, files;
                    if (file.isDirectory()) {
                        files = file.listFiles();
                        length = files.length;
                        for (i = 0; i < length; i++) {
                            file = files[i];
                            register(prefix ? prefix + '.' + file.getName() : file.getName(), file);
                        }
                    } else {
                        name = file.getName();
                        if (name.substring(name.length - 4) !== '.hbs') {
                            return;
                        }
                        file.open('r');
                        Handlebars.registerPartial(prefix.substring(0, prefix.length - 4), file.readAll());
                        file.close();
                    }
                })('', file);
            };
            //TODO : we don't need to register all partials in the themes dir.
            //Rather register only not overridden partials
            partials(new File(theme.__proto__.resolve.call(theme, 'partials')));
            partials(new File(theme.resolve('partials')));

            Handlebars.registerHelper('isAllowed', function(action, options) {
                if(caramel.meta().request.permissions != undefined && caramel.meta().request.permissions[action]) {
                    return options.fn(this);
                }
                return options.inverse(this);
            });


            Handlebars.registerHelper('ifCond', function (v1, operator, v2, options) {

                switch (operator) {
                    case '!=':
                        return (v1 != v2) ? options.fn(this) : options.inverse(this);
                    case '==':
                        return (v1 == v2) ? options.fn(this) : options.inverse(this);
                    case '===':
                        return (v1 === v2) ? options.fn(this) : options.inverse(this);
                    case '<':
                        return (v1 < v2) ? options.fn(this) : options.inverse(this);
                    case '<=':
                        return (v1 <= v2) ? options.fn(this) : options.inverse(this);
                    case '>':
                        return (v1 > v2) ? options.fn(this) : options.inverse(this);
                    case '>=':
                        return (v1 >= v2) ? options.fn(this) : options.inverse(this);
                    default:
                        return options.inverse(this);
                }
            });

            Handlebars.registerHelper('UnixConvert', function(unixtimestamp){
                var newDate = new Date();
                newDate.setTime(unixtimestamp);
                dateString = newDate.toUTCString();
                return dateString;
            });

            Handlebars.registerHelper('user', function(action, options) {
                if(caramel.meta().session.get("USER_NAME") != undefined ) {
                    return caramel.meta().session.get("USER_NAME");
                }
                return "";
            });

            Handlebars.registerHelper('domain', function(action, options) {
                if(caramel.meta().session.get("TENANT_DOMAIN") != undefined ) {
                    return caramel.meta().session.get("TENANT_DOMAIN");
                }
                return "";
            });
            Handlebars.registerHelper('tenantID', function(action, options) {
                if(caramel.meta().session.get("TENANT_ID") != undefined ) {
                    return caramel.meta().session.get("TENANT_ID");
                }
                return "";
            });

        }
    }
}()));
