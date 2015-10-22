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
var clusterId;
var memberId;
var time = '30 Min';
$(document).ready(function () {
    loadCluster();
});


$('body').on('change', '#cluster-filter', function () {
    var e = document.getElementById("cluster-filter");
    clusterId = e.options[e.selectedIndex].value;
    loadMember(clusterId);
    publish(time);
});

$('body').on('change', '#member-filter', function () {
    var e = document.getElementById("member-filter");
    memberId = e.options[e.selectedIndex].value;
    publish(time);
});



function loadCluster(application) {
    $.ajax({
        url: '/portal/apis/health-stats-clusters',
        dataType: 'json',
        success: function (result) {
            var elem = document.getElementById('cluster-filter');
            var clusterIds = [];
            var records = JSON.parse(JSON.stringify(result));
            records.forEach(function (record, i) {
                clusterIds.push(record.ClusterId);
            });

            var elem = document.getElementById('cluster-filter');
            for (i = 0; i < clusterIds.length; i = i + 1) {
                var option = document.createElement("option");
                option.text = clusterIds[i];
                option.value = clusterIds[i];
                elem.appendChild(option);
            }
            document.getElementById('cluster').appendChild(elem);
        }
    });
    if (clusterId == null) {
        var e = document.getElementById("cluster-filter");
        clusterId = e.options[e.selectedIndex].value;
    }
}

function loadMember(cluster) {
    $.ajax({
        url: '/portal/apis/health-stats-members?clusterId='+cluster,
        dataType: 'json',
        success: function (result) {
            var elem = document.getElementById('member-filter');
            var memberIds = [];
            var records = JSON.parse(JSON.stringify(result));
            records.forEach(function (record, i) {
                memberIds.push(record.MemberId);
            });


            if (elem != null) {
                elem.parentNode.removeChild(elem);
            }

            var memberList = document.createElement('select');
            memberList.id = "member-filter";

            var optionList = "";

            optionList += "<option value= 'All Members'>All Members</option>";
            for (i = 0; i < memberIds.length; i = i + 1) {
                optionList += "<option value='" + memberIds[i] + "'>" + memberIds[i] + "</option>";
            }

            memberList.innerHTML = optionList;
            document.getElementById('member').appendChild(memberList);
        }
    });
    if (memberId == null) {
        var e = document.getElementById("member-filter");
        memberId = e.options[e.selectedIndex].value;
    }
}

function publish(timeInterval) {
    time = timeInterval;
    var data = {clusterId: clusterId, memberId: memberId, timeInterval: time};
    gadgets.Hub.publish("health-stats-filter", data);
    console.log("Publishing filter values: " + JSON.stringify(data));
}

