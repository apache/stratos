if (typeof CARBON == "undefined" || CARBON) {
    /**
     * The CARBON global namespace object. If CARBON is already defined, the
     * existing CARBON object will not be overwirrten so that defined
     * namespaces are preserved
     */
    var CARBON = {};
}

var pageLoaded = false;

jQuery(document).ready(function() {
    pageLoaded = true;
});

/**
 * Display the Warning Message inside a jQuery UI's dialog widget.
 * @method showWarningDialog
 * @param {String} message to display
 * @return {Boolean}
 */
CARBON.showWarningDialog = function(message, callback, closeCallback) {
    var strDialog = "<div id='dialog' title='WSO2 Carbon'><div id='messagebox-warning'><p>" +
                    message + "</p></div></div>";
    //var strDialog = "<div id='dialog' title='WSO2 Carbon'><div id='messagebox'><img src='img/warning.gif'/><p>" +
    //                message + "</p></div></div>";
 	var func = function() {   
    	    jQuery("#dcontainer").html(strDialog);
    
	    jQuery("#dialog").dialog({
	        close:function() {
	            jQuery(this).dialog('destroy').remove();
	            jQuery("#dcontainer").empty();
	            if (closeCallback && typeof closeCallback == "function") {
	                closeCallback();
	            }
	            return false;
	        },
	        buttons:{
	            "OK":function() {
	                jQuery(this).dialog("destroy").remove();
	                jQuery("#dcontainer").empty();
	                if(callback && typeof callback == "function")
	                    callback();
	                return false;
	            }
	        },
	        height:160,
	        width:450,
	        minHeight:160,
	        minWidth:330,
	        modal:true
	    });
	};
    if (!pageLoaded) {
        jQuery(document).ready(func);
    } else {
        func();
    }

};

/**
 * Display the Error Message inside a jQuery UI's dialog widget.
 * @method showErrorDialog
 * @param {String} message to display
 * @return {Boolean}
 */
CARBON.showErrorDialog = function(message, callback, closeCallback) {
    var strDialog = "<div id='dialog' title='WSO2 Carbon'><div id='messagebox-error'><p>" +
                    message + "</p></div></div>";
    //var strDialog = "<div id='dialog' title='WSO2 Carbon'><div id='messagebox'><img src='img/error.gif'/><p>" +
    //                message + "</p></div></div>";
    var func = function() {   
            jQuery("#dcontainer").html(strDialog);

	    jQuery("#dialog").dialog({
	        close:function() {
	            jQuery(this).dialog('destroy').remove();
	            jQuery("#dcontainer").empty();
	            if (closeCallback && typeof closeCallback == "function") {
	                closeCallback();
	            }
	            return false;
	        },
	        buttons:{
	            "OK":function() {
	                jQuery(this).dialog("destroy").remove();
	                jQuery("#dcontainer").empty();
	                if(callback && typeof callback == "function")
	                    callback();
	                return false;
	            }
	        },
	        height:200,
	        width:490,
	        minHeight:160,
	        minWidth:330,
	        modal:true
	    });
    };
    if (!pageLoaded) {
        jQuery(document).ready(func);
    } else {
        func();
    }

};

/**
 * Display the Info Message inside a jQuery UI's dialog widget.
 * @method showInfoDialog
 * @param {String} message to display
 * @return {Boolean}
 */
CARBON.showInfoDialog = function(message, callback, closeCallback) {
    var strDialog = "<div id='dialog' title='WSO2 Carbon'><div id='messagebox-info'><p>" +
                     message + "</p></div></div>";
    //var strDialog = "<div id='dialog' title='WSO2 Carbon'><div id='messagebox'><img src='img/info.gif'/><p>" +
    //                message + "</p></div></div>";
    var func = function() {   
	    jQuery("#dcontainer").html(strDialog);
	
	    jQuery("#dialog").dialog({
	        close:function() {
	            jQuery(this).dialog('destroy').remove();
	            jQuery("#dcontainer").empty();
	            if (closeCallback && typeof closeCallback == "function") {
	                closeCallback();
	            }
	            return false;
	        },
	        buttons:{
	            "OK":function() {
	                jQuery(this).dialog("destroy").remove();
	                jQuery("#dcontainer").empty();
	                if(callback && typeof callback == "function")
	                    callback();
	                return false;
	            }
	        },
	        height:160,
	        width:450,
	        minHeight:160,
	        minWidth:330,
	        modal:true
	    });
       };
    if (!pageLoaded) {
        jQuery(document).ready(func);
    } else {
        func();
    }

};

/**
 * Display the Confirmation dialog.
 * @method showConfirmationDialog
 * @param {String} message to display
 * @param {Function} handleYes callback function to execute after user press Yes button
 * @param {Function} handleNo callback function to execute after user press No button
 * @return {Boolean} It's prefer to return boolean always from your callback functions to maintain consistency.
 */
CARBON.showConfirmationDialog = function(message, handleYes, handleNo, closeCallback){
    /* This function always assume that your second parameter is handleYes function and third parameter is handleNo function.
     * If you are not going to provide handleYes function and want to give handleNo callback please pass null as the second
     * parameter.
     */
    var strDialog = "<div id='dialog' title='WSO2 Carbon'><div id='messagebox-confirm'><p>" +
                    message + "</p></div></div>";

    handleYes = handleYes || function(){return true};

    handleNo = handleNo || function(){return false};
    var func = function() {   
	    jQuery("#dcontainer").html(strDialog);
	
	    jQuery("#dialog").dialog({
	        close:function() {
	            jQuery(this).dialog('destroy').remove();
	            jQuery("#dcontainer").empty();
	            if (closeCallback && typeof closeCallback == "function") {
	                closeCallback();
	            }
	            return false;
	        },
	        buttons:{
	            "Yes":function() {
	                jQuery(this).dialog("destroy").remove();
	                jQuery("#dcontainer").empty();
	                handleYes();
	            },
	            "No":function(){
	                jQuery(this).dialog("destroy").remove();
	                jQuery("#dcontainer").empty();
	                handleNo();
	            }
	        },
	        height:160,
	        width:450,
	        minHeight:160,
	        minWidth:330,
	        modal:true
	    });
    };
    if (!pageLoaded) {
        jQuery(document).ready(func);
    } else {
        func();
    }
    return false;
}

/**
 * Display the Info Message inside a jQuery UI's dialog widget.
 * @method showPopupDialog
 * @param {String} message to display
 * @return {Boolean}
 */
CARBON.showPopupDialog = function(message, title, windowHight, okButton, callback, windowWidth) {
    var strDialog = "<div id='dialog' title='" + title + "'><div id='popupDialog'></div>" + message + "</div>";
    var requiredWidth = 750;
    if (windowWidth) {
        requiredWidth = windowWidth;
    }
    var func = function() { 
    jQuery("#dcontainer").html(strDialog);
    if (okButton) {
        jQuery("#dialog").dialog({
            close:function() {
                jQuery(this).dialog('destroy').remove();
                jQuery("#dcontainer").empty();
                return false;
            },
            buttons:{
                "OK":function() {
                    if (callback && typeof callback == "function")
                        callback();
                    jQuery(this).dialog("destroy").remove();
                    jQuery("#dcontainer").empty();
                    return false;
                }
            },
            height:windowHight,
            width:requiredWidth,
            minHeight:windowHight,
            minWidth:requiredWidth,
            modal:true
        });
    } else {
        jQuery("#dialog").dialog({
            close:function() {
                jQuery(this).dialog('destroy').remove();
                jQuery("#dcontainer").empty();
                return false;
            },
            height:windowHight,
            width:requiredWidth,
            minHeight:windowHight,
            minWidth:requiredWidth,
            modal:true
        });
    }
	
	jQuery('.ui-dialog-titlebar-close').click(function(){
				jQuery('#dialog').dialog("destroy").remove();
                jQuery("#dcontainer").empty();
				jQuery("#dcontainer").html('');
		});
	
    };
    if (!pageLoaded) {
        jQuery(document).ready(func);
    } else {
        func();
    }
};

/**
 * Display the Input dialog.
 * @method showInputDialog
 * @param {String} message to display
 * @param {Function} handleOk callback function to execute after user press OK button.
 * @param {Function} handleCancel callback function to execute after user press Cancel button
 * @param {Function} closeCallback callback function to execute after user close the dialog button.
 * @return {Boolean} It's prefer to return boolean always from your callback functions to maintain consistency.
 *
 * handleOk function signature
 * ---------------------------
 * function(inputText){
 *  //logic
 * }
 */
CARBON.showInputDialog = function(message, handleOk, handleCancel, closeCallback){
    var strInput = "<div style='margin:20px;'><p>"+message+ "</p><br/>"+
                   "<input type='text' id='carbon-ui-dialog-input' size='40' name='carbon-dialog-inputval'></div>";
    var strDialog = "<div id='dialog' title='WSO2 Carbon'>" + strInput + "</div>";
    var func = function() {   
	    jQuery("#dcontainer").html(strDialog);
	    jQuery("#dialog").dialog({
	        close:function() {
	            jQuery(this).dialog('destroy').remove();
	            jQuery("#dcontainer").empty();
	            if (closeCallback && typeof closeCallback == "function") {
	                closeCallback();
	            }
	            return false;
	        },
	        buttons:{
	            "OK":function() {
	                var inputVal = jQuery('input[name=carbon-dialog-inputval]').fieldValue();
	                handleOk(inputVal);
	                jQuery(this).dialog("destroy").remove();
	                jQuery("#dcontainer").empty();
	                return false;
	            },
	            "Cancel":function(){
	                jQuery(this).dialog("destroy").remove();
	                jQuery("#dcontainer").empty();
	                handleCancel();
	            }
	        },
	        height:160,
	        width:450,
	        minHeight:160,
	        minWidth:330,
	        modal:true
	    });
    };
    if (!pageLoaded) {
        jQuery(document).ready(func);
    } else {
        func();
    }
}
/**
 * Display the loading dialog.
 * @method showLoadingDialog
 * @param {String} message to display
 * @param {Function} handleRemoveMessage callback function to triger the removal of the message.
 * handleOk function signature
 * ---------------------------
 * function(inputText){
 *  //logic
 * }
 */
CARBON.showLoadingDialog = function(message, handleRemoveMessage){
    //var strInput = "<div id='dcontainer' style='margin:20px;'><p><img src='../admin/images/loading.gif' />"+message+ "</p><br/></div>";


    var func = function() {
        var windowHeight = 20;
        var windowWidth = 100 + message.length*7;
        var strDialog = '<div class="ui-dialog-overlay" style="border-width: 0pt; margin: 0pt; padding: 0pt; position: absolute; top: 0pt; left: 0pt; width: ' + jQuery(document).width() + 'px; height: ' + jQuery(document).height() + 'px; z-index: 1001;">' +
                        '<div class="loadingDialogBox" style="background-color:#fff;border-radious:5px; -moz-border-radious:5px;possition:absolute;margin-top:' + (( jQuery(window).height() - windowHeight ) / 2+jQuery(window).scrollTop()) + 'px;margin-left:' + (( jQuery(window).width() - windowWidth ) / 2+jQuery(window).scrollLeft()) + 'px;height:'+windowHeight+'px;width:'+windowWidth+'px;">' + message + '</div>' +
                        '</div>';
        jQuery("#dcontainer").html(strDialog);

    };
    if (!pageLoaded) {
        jQuery(document).ready(func);
    } else {
        func();
    }
}
CARBON.closeWindow = function(){
jQuery("#dialog").dialog("destroy").remove();
}
