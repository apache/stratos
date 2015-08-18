/*
 * Copyright 2005,2007 WSO2, Inc. http://wso2.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var previousPanel = null;
function showTooltip(anchorObj, panelContent) {
    var timeout;
    var delay = 250;
    var panel = document.createElement("DIV");
    panel.className = "panelClass";

    if (previousPanel != null) {
        document.getElementById(previousPanel).parentNode.removeChild(document.getElementById(previousPanel));
    }
    previousPanel = 'panelId' + Math.floor(Math.random() * 2000);
    panel.id = previousPanel;

    document.getElementById("workArea").appendChild(panel);
    panel.innerHTML = panelContent;
    var yuiPanel = new YAHOO.widget.Panel(panel, {
        context: [anchorObj, ,"tr","br"],
        draggable: false,
        visible: false,
        close: false,
        constraintoviewport:true,
        underlay: "shadow"
    });

    yuiPanel.align("tr", "br"); // re-align in case page resized
    yuiPanel.show();
    if (timeout) clearTimeout(timeout);
    var timeoutFunc = function() {
        yuiPanel.hide();
    };
    YAHOO.util.Event.addListener(panel, "mouseover", function(e) {
        if (timeout) clearTimeout(timeout);
    });
    YAHOO.util.Event.addListener(anchorObj, "mouseout", function(e) {
        if (timeout) clearTimeout(timeout);
        timeout = setTimeout(timeoutFunc, delay);
    });

    YAHOO.util.Event.addListener(panel, "mouseout", function(e) {
        if (timeout) clearTimeout(timeout);
        timeout = setTimeout(timeoutFunc, delay);
    });
}

function enableDefaultText(textBoxId) {
    YAHOO.util.Event.onDOMReady(
            function(inobj) {
                if (arguments.length > 2) {
                    inobj = arguments[2];
                }
                var txtBox = document.getElementById(inobj);
                if (txtBox.value.replace(/^\s\s*/, '').replace(/\s\s*$/, '') == "" || txtBox.value == txtBox.getAttribute('alt')) {
                    txtBox.value = txtBox.getAttribute('alt');
                    YAHOO.util.Dom.addClass(txtBox, 'defaultText');
                }else{
                    YAHOO.util.Dom.removeClass(txtBox, 'defaultText');                                    
                }
                YAHOO.util.Event.on(txtBox, "focus", function(e) {
                    if (this.value == this.getAttribute('alt')) {
                        this.value = '';
                        YAHOO.util.Dom.removeClass(this, 'defaultText');
                    }
                });
                YAHOO.util.Event.on(txtBox, "blur", function(e) {
                    if (this.value == '') {
                        this.value = this.getAttribute('alt');
                        YAHOO.util.Dom.addClass(this, 'defaultText');
                    }
                });
            },
            textBoxId
            );
}