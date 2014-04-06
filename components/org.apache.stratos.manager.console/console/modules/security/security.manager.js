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
Description: The class is used to manage the security aspects of the app
Created Date: 5/10/2013
Filename: security.manager.js
 */
securityManagementModule=function(){

    var APP_SECURITY_MANAGER='security.manager';
    var provider=require('/modules/security/security.provider.js').securityModule();
    var log=new Log('security.manager');

    function SecurityManager(){
        this.provider=provider;
    }


    /*
    The function is used to perform a security check on a request
    @cb: An optional callback function which will be invoked if a check fails
    @return: True if the check passes,else false
     */
    SecurityManager.prototype.check=function(session,cb){

        var passed=false;

        //Checks whether the request can be handled
        if(this.provider.isPermitted(session)){
            log.debug('passed the security check.');

            this.provider.onSecurityCheckPass();

            passed=true;
        }
        else{
            log.debug('failed the security check.');

            //Check if a user has provided a call back
            if(cb){
                cb();
            }
            else{
                this.provider.onSecurityCheckFail();
            }

        }

        return passed;
    }

    /*
    The function is used to obtain a cached copy of the
    SecurityManager
    @return: A cached copy of the Security Manager from the
            application context
     */
    function cached(){
       //var instance=application.get(APP_SECURITY_MANAGER);

       //Checks if an instance exists
       //if(!instance){
           instance=new SecurityManager();
       //}

       return instance;
    }

    return {
        SecurityManager:SecurityManager,
        cached:cached
    }
}


