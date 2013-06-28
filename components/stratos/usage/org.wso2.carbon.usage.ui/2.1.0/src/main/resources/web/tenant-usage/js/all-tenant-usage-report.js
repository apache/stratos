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
    alert(1);
    var xmlHttpReq = createXmlHttpRequest();
    alert(2);
    // Make sure the XMLHttpRequest object was instantiated
    if (xmlHttpReq) {
        // This is a synchronous POST, hence UI blocking.
        xmlHttpReq.open("GET", "all_tenant_usage_report.jsp", false);
        xmlHttpReq.send(null);
        
        if (xmlHttpReq.status == 200) {
            return removeCarriageReturns(xmlHttpReq.responseText);
        }

        return false;
    }

    return false;
}
function removeCarriageReturns(string) {
	alert(string);
    return string.replace(/\n/g, "");
}
