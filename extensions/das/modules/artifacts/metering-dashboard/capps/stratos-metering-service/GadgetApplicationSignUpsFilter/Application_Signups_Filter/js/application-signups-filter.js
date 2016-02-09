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
var applicationId;

$(document).ready(function () {
    loadApplication();
});

gadgets.HubSettings.onConnect = function () {
    gadgets.Hub.subscribe("request-params", function (topic, data) {
        applicationId = data.applicationId;
    });
};

$('body').on('change', '#application-filter', function () {
    var e = document.getElementById("application-filter");
    applicationId = e.options[e.selectedIndex].value;
    publish();
});

function loadApplication() {
    console.log("Getting Application Ids");
    $.ajax({
        url: '/portal/apis/applications',
        dataType: 'json',
        success: function (result) {
            var applicationIds = [];
            var records = JSON.parse(JSON.stringify(result));
            records.forEach(function (record) {
                applicationIds.push(record.ApplicationId);
            });

            var elem = document.getElementById('application-filter');
            for (i = 0; i < applicationIds.length; i = i + 1) {
                var option = document.createElement("option");
                option.text = applicationIds[i];
                option.value = applicationIds[i];
                elem.appendChild(option);
            }

            document.getElementById('application').appendChild(elem);
        }
    });

}

function publish() {
    var data = {applicationId: applicationId};
    gadgets.Hub.publish("application-signups-filter", data);
    console.log("Publishing filter values: " + JSON.stringify(data));
}


