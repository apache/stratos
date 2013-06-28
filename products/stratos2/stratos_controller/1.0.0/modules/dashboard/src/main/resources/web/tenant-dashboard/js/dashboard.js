function execOnPageLoad() {
    // currently not used
}

function onChangeServiceSubscription() {
    sessionAwareFunction(function() {
        
        var cloudServiceForm = document.getElementById("cloudService");
        cloudServiceForm.submit();
        
    }, org_wso2_stratos_manager_dashboard_ui_jsi18n["session.timed.out"]);
}
