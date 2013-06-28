/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Following jquery function sends an AJAX request to the googleAnalyticsServlet and get back the
 * response from the servlet and append it into html/jsp file 'body' section,once the html/jsp
 * file is ready
 */
$('document').ready(function() {
    $.ajax({
               type: "GET",
               url: "/google-analytics",
               data: null,
               success: function(response) {
                   if (response != "googleAnalyticUrl") {
                       $("body").append(response);
                   }
               }
           });
});
