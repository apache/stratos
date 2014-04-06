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

/*
 Description:The class is used to secure pages by checking if a user is logged before
             accessing a page
 Created Date: 5/10/2013
 Filename: url.security.provider.js
 */

var securityModule = function () {

    var log=new Log('security.provider');

    /*
     The function checks if a user is present in the session
     @return: True if the user is allowed to access the url,else false
     */
    function isPermitted(session) {

        //Obtain the session and check if there is a user
        var user = require('console').server.current(session);
        if (user) {
            return true;
        }

        return false;
    }

    /*
     The function is invoked when the the security check fails
     and redirects the user to the login page
     */
    function onSecurityCheckFail() {
        log.debug('security check failed redirecting...');
        response.sendRedirect('/console/login');
    }

    /*
     The function is invoked when the security check is passed
     */
    function onSecurityCheckPass() {
        //Do nothing for now :)
    }

    return{

        isPermitted: isPermitted,
        onSecurityCheckFail: onSecurityCheckFail,
        onSecurityCheckPass: onSecurityCheckPass

    }
};
