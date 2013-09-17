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

function createXmlHttpRequest() {
    var request;

    // Lets try using ActiveX to instantiate the XMLHttpRequest
    // object
    try {
        request = new ActiveXObject("Microsoft.XMLHTTP");
    } catch(ex1) {
        try {
            request = new ActiveXObject("Msxml2.XMLHTTP");
        } catch(ex2) {
            request = null;
        }
    }

    // If the previous didn't work, lets check if the browser natively support XMLHttpRequest
    if (!request && typeof XMLHttpRequest != "undefined") {
        //The browser does, so lets instantiate the object
        request = new XMLHttpRequest();
    }
    function removeCarriageReturns(string) {
        return string.replace(/\n/g, "");
    }

    return request;
}
function getUsageReportData() {

    var xmlHttpReq = createXmlHttpRequest();

    // Make sure the XMLHttpRequest object was instantiated
    if (xmlHttpReq) {
        // This is a synchronous POST, hence UI blocking.
        xmlHttpReq.open("GET", "usage_report.jsp", false);
        xmlHttpReq.send(null);

        if (xmlHttpReq.status == 200) {
            return removeCarriageReturns(xmlHttpReq.responseText);
        }

        return false;
    }

    return false;
}
function removeCarriageReturns(string) {
    return string.replace(/\n/g, "");
}
