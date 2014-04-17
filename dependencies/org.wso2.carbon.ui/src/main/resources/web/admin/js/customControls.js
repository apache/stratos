function initSections(initHidden) {
    jQuery(document).ready(
            function() {
    if (initHidden == "hidden") {
        jQuery(".togglebleTitle").next().hide();
        jQuery(".togglebleTitle").addClass("contentHidden");
        jQuery(".togglebleTitle").each(
                function() {
                    jQuery(this).html(jQuery(this).html() + '<img src="../admin/images/arrow-up.png" />');
                }
                );
    } else {
        jQuery(".togglebleTitle").next().show();
        jQuery(".togglebleTitle").removeClass("contentHidden");
        jQuery(".togglebleTitle").each(
                function() {
                    jQuery(this).html(jQuery(this).html() + '<img src="../admin/images/arrow-down.png" />');
                }
                );
    }
    jQuery(".togglebleTitle").click(
            function() {
                if (jQuery(this).next().is(":visible")) {
                    jQuery(this).addClass("contentHidden");
                    jQuery('img', this).remove();
                    jQuery(this).html(jQuery(this).html() + '<img src="../admin/images/arrow-up.png" />');
                } else {
                    jQuery(this).removeClass("contentHidden");
                    jQuery('img', this).remove();
                    jQuery(this).html(jQuery(this).html() + '<img src="../admin/images/arrow-down.png" />');

                }
                jQuery(this).next().toggle("fast");
            }
            );
         });
}
function createPlaceholders() {
    var inputs = jQuery("input[type=text],input[type=email],input[type=tel],input[type=url]");
    inputs.each(
            function() {
                var _this = jQuery(this);
                this.placeholderVal = _this.attr("placeholder");
                _this.val(this.placeholderVal);
                if (this.placeholderVal != "") {
                    _this.addClass("placeholderClass");
                }
            }
            )
            .bind("focus", function() {
        var _this = jQuery(this);
        var val = jQuery.trim(_this.val());
        if (val == this.placeholderVal || val == "") {
            _this.val("");
            _this.removeClass("placeholderClass");
        }
    })
            .bind("blur", function() {
        var _this = jQuery(this);
        var val = jQuery.trim(_this.val());
        if (val == this.placeholderVal || val == "") {
            _this.val(this.placeholderVal);
            _this.addClass("placeholderClass");
        }

    });
}
function initMultipleSelectors(tableId) {
    jQuery(document).ready(
            function() {
                var multiSelectTable = document.getElementById(tableId);
                var leftSelect = jQuery('select', multiSelectTable)[0];
                var rightSelect = jQuery('select', multiSelectTable)[1];
                var toRight_btn = jQuery('input.toRight_btn', multiSelectTable)[0];
                var toLeft_btn = jQuery('input.toLeft_btn', multiSelectTable)[0];

                var addAllLink = jQuery('a.addAllLink', multiSelectTable)[0];
                var removeAllLink = jQuery('a.removeAllLink', multiSelectTable)[0];

                var transfer = function(params) {
                    var fromElm = params.fromElm;
                    var toElm = params.toElm;
                    var all = params.all;
                    
                    var opt = fromElm.options;
                    for (var i = 0; i < opt.length; i++) {
                        if (opt[i].selected || all) {
                            var newElm = document.createElement("option");
                            newElm.value = opt[i].value;
                            newElm.innerHTML = opt[i].innerHTML;
                            if (opt[i].name != undefined) {
                                newElm.value = opt[i].value;
                            }
                            if (opt[i].id != undefined) {
                                newElm.id = opt[i].id;
                            }
                            toElm.appendChild(newElm);
                        }
                    }
                    for (var i = (opt.length - 1); i >= 0; i--) {
                        if (opt[i].selected || all) {
                            fromElm.removeChild(opt[i]);
                        }
                    }
                    sortSelect(toElm);
                };
                jQuery(toRight_btn).click(
                        function() {
                            transfer({fromElm:leftSelect, toElm:rightSelect,all:false});
                        }
                        );

                jQuery(toLeft_btn).click(
                        function() {
                            transfer({fromElm:rightSelect, toElm:leftSelect,all:false});
                        }
                        );
                jQuery(addAllLink).click(
                        function() {
                            transfer({fromElm:leftSelect, toElm:rightSelect,all:true});
                        }
                 );
                jQuery(removeAllLink).click(
                        function() {
                            transfer({fromElm:rightSelect, toElm:leftSelect,all:true});
                        }
                 );


            });
}
function sortSelect(selElem) {
    var tmpAry = new Array();
    for (var i = 0; i < selElem.options.length; i++) {
        tmpAry[i] = new Array();
        tmpAry[i][0] = selElem.options[i].text;
        tmpAry[i][1] = selElem.options[i].value;
    }
    tmpAry.sort();
    while (selElem.options.length > 0) {
        selElem.options[0] = null;
    }
    for (var i = 0; i < tmpAry.length; i++) {
        var op = new Option(tmpAry[i][0], tmpAry[i][1]);
        selElem.options[i] = op;
    }
    return;
}