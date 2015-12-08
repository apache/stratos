/*
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

var vars;

function readRequestParam(){
    var query = window.location.search.substring(1);
    vars = query.split("&");

    var applicationId = getRequestParam('applicationId');
    var clusterId = getRequestParam('clusterId');

    var data = {applicationId: applicationId, clusterId: clusterId};
    console.log("Publishing request params: " + JSON.stringify(data));
    ues.hub.publish("request-params",data);
}


function getRequestParam(variable) {
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split("=");
        if (pair[0] == variable) {
            return pair[1];
        }
    }
    return null;
}
