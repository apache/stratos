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
var clusterId;
var time = '30 Min';
$(document).ready(function () {
    loadApplication();

    setTimeout(function () {
        if (applicationId != null) {
            loadCluster(applicationId);
            $('#' + window.frameElement.id).ready(function () {
                $(this).contents().find('body').contents().find('#application-filter').val(applicationId);
                publish(time);
            });
        }
    }, 2000);

    setTimeout(function () {
        if (clusterId != null) {
            $('#' + window.frameElement.id).ready(function () {
                $(this).contents().find('body').contents().find('#cluster-filter').val(clusterId);
                publish(time);
            });
        }
    }, 3000);


});

gadgets.HubSettings.onConnect = function () {
    gadgets.Hub.subscribe("request-params", function (topic, data) {
        applicationId = data.applicationId;
        clusterId = data.clusterId;
        console.log("Application Id: " + applicationId);
        console.log("Cluster Id: " + clusterId);
    });
};

$('body').on('change', '#application-filter', function () {
    var e = document.getElementById("application-filter");
    applicationId = e.options[e.selectedIndex].value;
    loadCluster(applicationId);
    publish(time);
});

$('body').on('change', '#cluster-filter', function () {
    var e = document.getElementById("cluster-filter");
    clusterId = e.options[e.selectedIndex].value;
    publish(time);
});

function loadApplication() {
    console.log("Getting Application Ids");
    $.ajax({
        url: '/portal/apis/applications',
        dataType: 'json',
        success: function (result) {
            console.log(JSON.stringify(result));
            var applicationIds = [];
            var records = JSON.parse(JSON.stringify(result));
            records.forEach(function (record, i) {
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

function loadCluster(application) {
    $.ajax({
        url: '/portal/apis/clusters?applicationId=' + application,
        dataType: 'json',
        success: function (result) {
            var elem = document.getElementById('cluster-filter');
            var clusterAlias = [];
            var records = JSON.parse(JSON.stringify(result));
            records.forEach(function (record, i) {
                clusterAlias.push(record.ClusterAlias);
            });

            if (elem != null) {
                elem.parentNode.removeChild(elem);
            }

            var clusterList = document.createElement('select');
            clusterList.id = "cluster-filter";

            var optionList = "";

            optionList += "<option value= 'All Clusters'>All Clusters</option>";
            for (i = 0; i < clusterAlias.length; i = i + 1) {
                optionList += "<option value='" + clusterAlias[i] + "'>" + clusterAlias[i] + "</option>";
            }

            clusterList.innerHTML = optionList;
            document.getElementById('cluster').appendChild(clusterList);
        }
    });
    if (clusterId == null) {
        var e = document.getElementById("cluster-filter");
        clusterId = e.options[e.selectedIndex].value;
    }

}

function publish(timeInterval) {
    time = timeInterval;
    var data = {applicationId: applicationId, clusterId: clusterId, timeInterval: time};
    gadgets.Hub.publish("member-status-filter", data);
    console.log("Publishing filter values: " + JSON.stringify(data));
}

