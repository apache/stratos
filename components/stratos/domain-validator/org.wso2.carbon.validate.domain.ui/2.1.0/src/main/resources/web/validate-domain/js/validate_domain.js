function validateWithText(domain, status) {
	sessionAwareStatusAwareFunction(function() {
    if(domain == "") {
        CARBON.showWarningDialog(org_wso2_carbon_validate_domain_ui_jsi18n["domain.empty"]);
        return false;
    }

    var reason = checkDomain(domain);
    if (reason != "") {
        CARBON.showWarningDialog(reason);
        return false;
    }

    var continueDiv = document.getElementById("with-text-continue");
    var msgDiv = document.getElementById("with-text-msg");
    var keyInput = document.getElementById("with-text-success-key");

    var busyTextPlaceHolder = document.getElementById("busyText");
    busyTextPlaceHolder.innerHTML = "<img src=\"images/ajax-loader.gif\"/>";

    new Ajax.Request('../validate-domain/validate_with_text_ajaxprocessor.jsp',
    {
        method:'post',
        parameters: {domain: domain, status: status},

        onSuccess: function(transport) {
            busyTextPlaceHolder.innerHTML = "";
            var returnValue = transport.responseText;
            if (returnValue.search(/----false----/) >= 0) {
                msgDiv.style.color = "#f00";
                msgDiv.innerHTML = "<img src='images/wrong.gif'/> " + org_wso2_carbon_validate_domain_ui_jsi18n["validation.fail.with.text"];
                continueDiv.style.visibility = "hidden";
            } else if (returnValue.search(/----unavailable----/) >= 0) {
                msgDiv.style.color = "#f00";
                msgDiv.innerHTML = "<img src='images/wrong.gif'/> " + org_wso2_carbon_validate_domain_ui_jsi18n["domain.unavailable"];
                continueDiv.style.visibility = "hidden";
            } else if (returnValue != "") {
                msgDiv.style.color = "#058000";
                msgDiv.innerHTML = "<img src='images/right.gif'/> " + org_wso2_carbon_validate_domain_ui_jsi18n["validation.success.with.text"];
                continueDiv.style.visibility = "visible";
                keyInput.value = returnValue;
            }
        },

        onFailure: function(transport){
            busyTextPlaceHolder.innerHTML = "";
        }
    });
	}, status, org_wso2_carbon_validate_domain_ui_jsi18n["session.timed.out"]);
}

function validateWithDNS(domain, status) {
	sessionAwareStatusAwareFunction(function() {
    if(domain == "") {
        CARBON.showWarningDialog(org_wso2_carbon_validate_domain_ui_jsi18n["domain.empty"]);
        return false;
    }

    var reason = checkDomain(domain);
    if (reason != "") {
        CARBON.showWarningDialog(reason);
        return false;
    }

    var continueDiv = document.getElementById("with-dns-continue");
    var msgDiv = document.getElementById("with-dns-msg");
    var keyInput = document.getElementById("with-dns-success-key");

    var busyDNSPlaceHolder = document.getElementById("busyDNS");
    busyDNSPlaceHolder.innerHTML = "<img src=\"images/ajax-loader.gif\"/>";

    new Ajax.Request('../validate-domain/validate_with_dns_ajaxprocessor.jsp',
    {
        method:'post',
        parameters: {domain: domain, status: status},

        onSuccess: function(transport) {
            busyDNSPlaceHolder.innerHTML = "";
            var returnValue = transport.responseText;
            if (returnValue.search(/----false----/) >= 0) {
                msgDiv.style.color = "#f00";
                msgDiv.innerHTML = "<img src='images/wrong.gif'/> " + org_wso2_carbon_validate_domain_ui_jsi18n["validation.fail.with.dns"];
                continueDiv.style.visibility = "hidden";
            } else if (returnValue.search(/----unavailable----/) >= 0) {
                msgDiv.style.color = "#f00";
                msgDiv.innerHTML = "<img src='images/wrong.gif'/> " + org_wso2_carbon_validate_domain_ui_jsi18n["domain.unavailable"];
                continueDiv.style.visibility = "hidden";
            } else if (returnValue != "") {
                msgDiv.style.color = "#058000";
                msgDiv.innerHTML = "<img src='images/right.gif'/> " + org_wso2_carbon_validate_domain_ui_jsi18n["validation.success.with.dns"];
                continueDiv.style.visibility = "visible";
                keyInput.value = returnValue;
            }
        },

        onFailure: function(transport){
            busyDNSPlaceHolder.innerHTML = "";
        }
    });
	}, status, org_wso2_carbon_validate_domain_ui_jsi18n["session.timed.out"]);
}


function gotoRegister() {
    window.location.href = '../tenant-register/success_register.jsp';
}

function checkDomain(domain)
{
    var error = "";
    var lastIndexOfDot = domain.lastIndexOf(".");
    var indexOfDot = domain.indexOf(".");
    var extension = domain.substring(lastIndexOfDot, domain.length);

    var illegalChars = /([^a-zA-Z0-9\._\-])/; // allow only letters and numbers . - _and period
    if (extension.indexOf("-trial") >= 0 || extension.indexOf("-unverified") >= 0) {
        // we are not allowing to create a domain with -trial or -unverified is in the extension
        error = "The domain name you entered is not valid. Please enter a valid domain name.";
    }
    else if (lastIndexOfDot <= 0) {
        // since this is domain validation, this expects a valid domain with an extension.
        error = "Invalid domain: " + domain + ". You should have an extension to your domain.";
    }
    else if (indexOfDot == 0) {
        error = "Invalid domain, starting with '.'";
    }
    else if (illegalChars.test(domain)) {
        error = "The domain only allows letters, numbers, '.', '-' and '_'. <br />";
    }
    return error;
}

function sessionAwareStatusAwareFunction(func, status, msg) {
	if (status == "logged_in") {
		sessionAwareFunction(func, msg);
	} else {
		func();
	}
}

function submitValidateWithDNSForm(status) {
	sessionAwareStatusAwareFunction(function() {
		$('validateWithDNSForm').submit();
	}, status, org_wso2_carbon_validate_domain_ui_jsi18n["session.timed.out"]);
}

function submitValidateWithTextForm(status) {
	sessionAwareStatusAwareFunction(function() {
		$('validateWithTextForm').submit();
	}, status, org_wso2_carbon_validate_domain_ui_jsi18n["session.timed.out"]);
}