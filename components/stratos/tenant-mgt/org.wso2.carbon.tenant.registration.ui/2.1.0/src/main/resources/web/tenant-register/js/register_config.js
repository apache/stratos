function addTenant(isPublicCloud) {
    var domain = document.getElementById('domain');
    var reason = "";
    var addTenantForm = document.getElementById('addTenantForm');
    var adminPassword = "";
    var adminPasswordRepeat = "";
    var email = "";
    var firstname = document.getElementById('admin-firstname');
    var lastname = document.getElementById('admin-lastname');
    var adminName = document.getElementById('admin');
    adminPassword = document.getElementById('admin-password');
    adminPasswordRepeat = document.getElementById('admin-password-repeat');
    email = document.getElementById('admin-email');

    // the domain validation part is moved to the select_domain.js

    var reason = validateEmpty(domain, "Domain");
    if (reason == "") {
        reason += checkDomain(domain, isPublicCloud);
    }
    if (reason == "") {
        reason += validateEmpty(firstname, "First Name");
    }
    if (reason == "") {
        reason += validateIllegal(firstname, "First Name");
    }
    if (reason == "") {
        reason += validateEmpty(lastname, "Last Name");
    }
    if (reason == "") {
        reason += validateIllegal(lastname, "Last Name");
    }
    if (reason == "") {
        reason += validateEmpty(adminName, "Admin Username");
    }
    if (reason == "") {
        reason += validateIllegal(adminName, "Admin Username");
    }
    if (reason == "") {
        reason += validateUsername(adminName);
    }
    if (reason == "") {
        reason += validateEmpty(adminPassword, "Admin Password");
    }
    if (reason == "") {
        reason += validateAdminPassword(adminPassword);
    }
    if (reason == "") {
        reason += validateEmpty(adminPasswordRepeat, "Admin Password (Repeat)");
    }
    if (reason == "") {
        reason += validateEmpty(email, "Email");
    }
    if (reason == "") {
        reason += validateEmail(email);
    }
    if (reason == "") {
        if (adminPassword.value != adminPasswordRepeat.value) {
            reason += jsi18n["password.mismatched"];
        }
        if (adminPassword.value.length < 6) {
            reason += jsi18n["password.length"];
        }
    }
    if (reason != "") {
        CARBON.showWarningDialog(reason);
        document.getElementbyId("submit-button").disabled = false;
        document.getElementById('waitMessage').style.display = 'none';
        return;
    }
    addTenantForm.submit();
}

function validateAdminPassword(fld) {
    var error = "";

    if (fld.value == "") {
        error = org_wso2_carbon_registry_common_ui_jsi18n["no.password"] + "<br />";
    } /* else if ((fld.value.length < 3) || (fld.value.length > 15)) {
     error = org_wso2_carbon_registry_common_ui_jsi18n["wrong.password"] + "<br />";
     } else if (illegalChars.test(fld.value)) {
     error = org_wso2_carbon_registry_common_ui_jsi18n["illegal.password"] + "<br />";
     } else if (!((fld.value.search(/(a-z)+/)) && (fld.value.search(/(0-9)+/)))) {
     error = "The password must contain at least one numeral.<br />";
     } */ else {
        fld.style.background = 'White';
    }
    return error;
}

function activationChanged(cbox, domain) {
    if (!cbox.checked) {
        CARBON.showConfirmationDialog("Are you sure you want to deactivate the domain: " +
                                      domain + ".", function() {
            var submitForm = document.getElementById(domain + "_form");
            submitForm.submit();
        }, function() {
            cbox.checked = "on";
        });
    } else {
        var submitForm = document.getElementById(domain + "_form");
        submitForm.submit();
    }
}

function fillAdminValue() {
    var adminValue = document.getElementById('adminValue');
    var domain = document.getElementById('domain');

    var reason = validateIllegal(domain, "Domain");
    if (reason != "") {
        CARBON.showErrorDialog(reason);
        adminValue.innerHTML = '';
        return;
    }

    if (domain.value == "") {
        adminValue.innerHTML = '' + domain.value;
    }
    else {
        adminValue.innerHTML = '@' + domain.value;
    }
}

function showregistrationfail() {
    var error = "";
    CARBON.showWarningDialog(error);
}

var kaptchaImgUrl;
function showKaptcha(kaptchaImgUrlArg) {
    kaptchaImgUrl = kaptchaImgUrlArg;
    var kaptchaImgDiv = document.getElementById("kaptchaImgDiv");
    kaptchaImgDiv.innerHTML = "<img src='../tenant-register/images/ajax-loader.gif' alt='busy'/>";
    setTimeout("showKaptchaTimely()", 4000);
}

function showKaptchaTimely() {
    var kaptchaImgDiv = document.getElementById("kaptchaImgDiv");
    kaptchaImgDiv.innerHTML = "<img src='" + kaptchaImgUrl + "' alt='If you can not see the captcha " +
                              "image please refresh the page or click the link again.'/>";
}

function activateSubmit(fld) {
    var submitButton = document.getElementById('submit-button');
    submitButton.disabled = !fld;
}