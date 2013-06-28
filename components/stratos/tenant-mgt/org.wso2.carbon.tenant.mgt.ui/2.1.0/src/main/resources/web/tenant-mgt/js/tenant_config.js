function addTenant(isUpdating, isPublicCloud) {
    
    var reason = "";
    var addTenantForm = document.getElementById('addTenantForm');
    var adminPassword = document.getElementById('admin-password');
    var adminPasswordRepeat = document.getElementById('admin-password-repeat');
    var email = document.getElementById('admin-email');
    var firstname = document.getElementById('admin-firstname');
    var lastname = document.getElementById('admin-lastname');

    if (isUpdating) {

        // only the given values will be updated, so no need to fill all the values.
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
        if (adminPassword.value != null && adminPassword.value != "") {
            if (adminPassword.value != adminPasswordRepeat.value) {
                reason += jsi18n["password.mismatched"];
            }
        }
    }
    else {
        var domain = document.getElementById('domain');
        var adminName = document.getElementById('admin');

        if (reason == "") {
            reason += validateEmpty(domain, "Domain");
        }
        if (reason == "") {
            reason += validateDomain(domain, isPublicCloud);
        }

        if (reason == "") {
             reason +=domainAvailability(domain);
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
            reason += validateEmpty(adminName, "Admin Name");
        }
        if (reason == "") {
            reason += validateIllegal(adminName, "Admin Name");
        }
        if (reason == "") {
            reason += validateUsername(adminName);
        }
        if (reason == "") {
            reason += validateEmpty(adminPassword, "AdminPassword");
        }
        if (reason == "") {
            reason += validateEmpty(adminPasswordRepeat, "AdminPasswordRepeat");
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
    }

    if (reason != "") {
        CARBON.showErrorDialog(reason);
        return;
    }
    document.getElementById("isUpdating").value=isUpdating;
    addTenantForm.submit();
}
function showSuccessRegisterMessage() {
    var message = "You have registered the Organization Successfully";
    CARBON.showInfoDialog(message);
    return;
}
function showSuccessUpdateMessage() {
    var message = "Your changes saved Successfully!";
    CARBON.showInfoDialog(message);
    return;
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
    var adminValue = $('adminValue');
    var domain = $('domain');

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
function refreshFillAdminValue() {
    //Call this method at loading time
    fillAdminValue();
}

function validateDomain(fld, isPublicCloudSetup) {
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

function domainSelected() {
    var findDomainForm = document.getElementById('findTenantForm');
    findDomainForm.submit();
}


function domainAvailability(domain) {
    var error = "";
    jQuery.ajax({
        type: 'POST',
        url: 'check_domain_availability_ajaxprocessor.jsp?',
        dataType: 'json',
        data: 'domain=' + domain.value,
        success: function(result) {
            var available = result.available;
            if (available == 'false') {
                error = "Sorry!. The Domain is already registered. Please choose a different domain.";
                return error;
            }else{
               error ="";
            }

        },
        error:function (xhr, ajaxOptions, thrownError) {
             error = "Error in checking domain availability";

        },
        async: false        
    });

    return error;
}

function activateDeactivate(domain, isActive) {
    if (isActive == 'true') {
        CARBON.showConfirmationDialog("Are you sure you want to deactivate the domain: " +
                domain + "?", function() {
            var submitForm = document.getElementById("activateTenantForm");
            submitForm.submit();
        }, function() {
        });
    } else {
        var submitForm = document.getElementById("activateTenantForm");
        submitForm.submit();
    }
}
