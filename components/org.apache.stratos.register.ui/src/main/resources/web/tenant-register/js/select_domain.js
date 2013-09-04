/**
*  Licensed to the Apache Software Foundation (ASF) under one
*  or more contributor license agreements.  See the NOTICE file
*  distributed with this work for additional information
*  regarding copyright ownership.  The ASF licenses this file
*  to you under the Apache License, Version 2.0 (the
*  "License"); you may not use this file except in compliance
*  with the License.  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

function checkDomainAvailability(isPublicCloud) {
    var domain = document.getElementById('domain');

    var reason = validateEmpty(domain, "Domain");
    if (reason == "") {
        reason += checkDomain(domain, isPublicCloud);
    }

    if(reason != "") {
        CARBON.showWarningDialog(reason);
        return false;
    }

    var busyCheck = document.getElementById("busyCheck");
    busyCheck.innerHTML = "<img src=\"images/ajax-loader.gif\"/>";
    
    var domain_confirmation_div = document.getElementById("domain-confirmation-msg");
    
    new Ajax.Request('../tenant-register/domain_availability_ajaxprocessor.jsp',
    {
        method:'post',
        parameters: {domain: domain.value},

        onSuccess: function(transport) {
            busyCheck.innerHTML = "";
            var returnValue = transport.responseText;
            if (returnValue.search(/----DomainAvailable----/) == -1) {
                domain_confirmation_div.style.color = "#f00";
                domain_confirmation_div.innerHTML = jsi18n["domain.unavailable"]; 
                result = false;
            } else {
                domain_confirmation_div.style.color = "#058000";
                domain_confirmation_div.innerHTML = jsi18n["domain.available"];
                result = true;
            }
        },

        onFailure: function(transport){
            busyCheck.innerHTML = "";
        }
    });
}

function clearDomainConfirmationMsg() {
    var domain_confirmation_div = document.getElementById("domain-confirmation-msg");
    domain_confirmation_div.innerHTML = "";
}


function checkDomain(fld, isPublicCloudSetup)
{
    var error = "";
    var domain = fld.value;
    var lastIndexOfDot = domain.lastIndexOf(".");
    var indexOfDot = domain.indexOf(".");
    var extension = domain.substring(lastIndexOfDot, domain.length);

    var illegalChars = /([^a-zA-Z0-9\._\-])/; // allow only letters and numbers . - _and period
    if (extension.indexOf("-trial") >= 0 || extension.indexOf("-unverified") >= 0) {
        // we are not allowing to create a domain with -trial or -unverified is in the extension
        error = "The domain name you entered is not valid. Please enter a valid domain name.";
    }
    else if (isPublicCloudSetup && (lastIndexOfDot <= 0)) {
        error = "Invalid domain: " + domain + ". You should have an extension to your domain.";
    }
    else if (indexOfDot == 0) {
        error = "Invalid domain, starting with '.'";
    }
    else if (illegalChars.test(fld.value)) {
        error = "The domain only allows letters, numbers, '.', '-' and '_'. <br />";
    } else {
        fld.style.background = 'White';
    }
    return error;
}
