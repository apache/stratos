var applicationId;
var clusterId;
var vars;
$(document).ready(function () {
    var query = window.location.search.substring(1);
    vars = query.split("&");

    applicationId = getRequestParam('applicationId');
    clusterId = getRequestParam('clusterId');

    setTimeout(function () {
        var data = {applicationId: applicationId, clusterId: clusterId};
        console.log("Publishing request params: " + JSON.stringify(data));
        ues.hub.publish("request-params",data);
    }, 2000);

});

function getRequestParam(variable) {
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split("=");
        if (pair[0] == variable) {
            return pair[1];
        }
    }
    return null;
}
