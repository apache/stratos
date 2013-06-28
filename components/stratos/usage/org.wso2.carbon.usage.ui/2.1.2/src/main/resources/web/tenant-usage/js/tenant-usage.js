function submitAllTenantPaginatedUsage(pageNumber) {
    sessionAwareFunction(function() {
        document.getElementById("requestedPage").value = pageNumber;
        var usageForm = document.getElementById("usageForm");
        usageForm.submit();
    }, jsi18n["session.timed.out"]);
}
