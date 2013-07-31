
function getDomainFromUserName() {
    var tenantDomain = "";
    var userName = document.getElementById("txtUserName").value;
    if (userName != null) {
        atIndex = userName.lastIndexOf('@');
        if (atIndex != -1) {
            tenantDomain = userName.substring(atIndex + 1, userName.length);
        }
    }
    return tenantDomain;
}


function getTenantAwareUserName() {
    var userName = document.getElementById("txtUserName").value;
    if (userName != null) {
        atIndex = userName.lastIndexOf('@');
        if (atIndex != -1) {
            userName = userName.substring(0, atIndex);
        }
    }
    return userName;
}



