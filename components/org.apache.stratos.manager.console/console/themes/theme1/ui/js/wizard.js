$(function () {
    $('#wizardNext').click(function () {
        var newStep = parseInt($(this).attr('data-step')) + 1;
        location.href = '?step='+newStep;
    });
    $('#wizardBack').click(function () {
        var newStep = parseInt($(this).attr('data-step')) - 1;
        location.href = '?step='+newStep;
    });
    $('pre').each(function () {
        var jsonStr = $(this).html();
        jsonStr = jsonStr.replace(/\'/g, '\"');
        var obj = jQuery.parseJSON(jsonStr);
        jsonStr = JSON.stringify(obj, undefined, 2);
        $('textarea', $(this).parent()).val(jsonStr);
        $(this).html(syntaxHighlight(jsonStr));
    });
});
