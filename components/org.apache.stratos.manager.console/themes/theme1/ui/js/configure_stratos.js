$(function () {
    $('pre').each(function () {
        var jsonStr = $(this).html();
        jsonStr = jsonStr.replace(/\'/g, '\"');
        var obj = jQuery.parseJSON(jsonStr);
        jsonStr = JSON.stringify(obj, undefined, 2);
        $('textarea', $(this).parent()).val(jsonStr);
        $(this).html(syntaxHighlight(jsonStr));
    });
    $('.js_jsonEdit').click(function () {
        var $container = $(this).parent();
        $(this).hide();
        var preHeight = $('pre', $container).height();
        $('.js_jsonEdit', $container).hide();
        $('pre', $container).hide();
        $('textarea', $container).show().height(preHeight);
        $('.js_jsonCancel', $container).show();
        $('.js_jsonSave', $container).show();
    });
    $('.js_jsonSave').click(function () {
        alert('save - ' + $('textarea', $(this).parent()).val())
    });
    $('.js_jsonCancel').click(function () {
        var $container = $(this).parent();
        $('.js_jsonEdit', $container).show();
        $('pre', $container).show();
        $('textarea', $container).hide();
        $('.js_jsonCancel', $container).hide();
        $('.js_jsonSave', $container).hide();
    });
    $('.js_handle_click').click(function(event){
        event.preventDefault();
        $('#addItemSection').show('fast');
    });
    $('#deployPolicy').click(function(){
        alert('saving ' + $('#policy').val());
    });
});
