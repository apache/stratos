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
//Javascript for collapsible menu
var oneYear = 1000 * 60 * 60 * 24 * 365;
var cookie_date = new Date();  // current date & time
cookie_date.setTime(cookie_date.getTime() + oneYear);
var onMenuSlide = new YAHOO.util.CustomEvent("onMenuSlide");

function nextObject(obj) {
    var n = obj;
    do n = n.nextSibling;
    while (n && n.nodeType != 1);
    return n;
}
function mainMenuCollapse(img) {
    var theLi = img.parentNode;
    var nextLi = nextObject(theLi);
    var attributes = "";
    if (nextLi.style.display == "none") {
        //remember the state
        document.cookie = img.id + "=visible;path=/;expires=" + cookie_date.toGMTString();
        var ver = getInternetExplorerVersion();

        if (ver > -1)
        {
            if (ver >= 8.0) {
                attributes = {
                    opacity: { to: 1 }
                };
                var anim = new YAHOO.util.Anim(nextLi, attributes);
                anim.animate();
                nextLi.style.display = "";
                nextLi.style.height = "auto";
            } else {
                nextLi.style.display = "";
            }
        } else
        {
            attributes = {
                opacity: { to: 1 }
            };
            var anim = new YAHOO.util.Anim(nextLi, attributes);
            anim.animate();
            nextLi.style.display = "";
            nextLi.style.height = "auto";
        }
        img.src = "../admin/images/up-arrow.gif";

    } else {
        //remember the state
        document.cookie = img.id + "=none;path=/;expires=" + cookie_date.toGMTString();
        var ver = getInternetExplorerVersion();

        if (ver > -1)
        {
            if (ver >= 8.0) {
                attributes = {
                    opacity: { to: 0 }
                };
                anim = new YAHOO.util.Anim(nextLi, attributes);
                anim.duration = 0.3;
                anim.onComplete.subscribe(hideTreeItem, nextLi);

                anim.animate();
            } else {
                nextLi.style.display = "none";
            }
        } else {
            attributes = {
                opacity: { to: 0 }
            };
            anim = new YAHOO.util.Anim(nextLi, attributes);
            anim.duration = 0.3;
            anim.onComplete.subscribe(hideTreeItem, nextLi);

            anim.animate();
        }
        img.src = "../admin/images/down-arrow.gif";

    }
}

function hideTreeItem(state, opts, item) {
    item.style.display = "none";
}

//YAHOO.util.Event.onDOMReady(setMainMenus);
function setMainMenus() {
    var els = YAHOO.util.Dom.getElementsByClassName('mMenuHeaders', 'img');
       
    for (var i = 0; i < els.length; i++) {
        var theLi = els[i].parentNode;
        var nextLi = nextObject(theLi);
        var cookieName = els[i].id;
        var nextLiState = get_cookie(cookieName);
        if(nextLiState == "visible"){
            nextLiState = "";
        }
        if (nextLiState != null) {
            if (get_cookie('menuPanelType') == null || get_cookie('menuPanelType') == "main") { //Set the menu to main

                if(theLi.id == "region4_monitor_menu"){
                    nextLi.style.display = "none";        
                }else if(theLi.id == "region1_configure_menu"){
                    nextLi.style.display = "none";
                }else if(theLi.id == "region5_tools_menu"){
                    nextLi.style.display = "none";
                }else if(theLi.id == "region3_extensions_menu"){
                    nextLi.style.display = "none";
                } else{
                    nextLi.style.display = nextLiState;
                }
            }else if(get_cookie('menuPanelType') == "monitor"){
                if(theLi.id == "region4_monitor_menu"){
                    nextLi.style.display = "";
                }
            }else if(get_cookie('menuPanelType') == "main"){
                if(theLi.id == "region1_configure_menu"){
                    nextLi.style.display = "";
                }
            }else if(get_cookie('menuPanelType') == "tools"){
                if(theLi.id == "region5_tools_menu"){
                    nextLi.style.display = "";
                }
            }else if(get_cookie('menuPanelType') == "extensions"){
                if(theLi.id == "region3_extensions_menu"){
                    nextLi.style.display = "";
                }
            }

        }
        if (nextLiState == "none") {
            els[i].src = "../admin/images/down-arrow.gif";
        } else {
            els[i].src = "../admin/images/up-arrow.gif";
        }
    }
}

function get_cookie(check_name) {
    // first we'll split this cookie up into name/value pairs
    // note: document.cookie only returns name=value, not the other components
    var a_all_cookies = document.cookie.split(';');
    var a_temp_cookie = '';
    var cookie_name = '';
    var cookie_value = '';
    var b_cookie_found = false; // set boolean t/f default f

    for (i = 0; i < a_all_cookies.length; i++)
    {
        a_temp_cookie = a_all_cookies[i].split('=');
        cookie_name = a_temp_cookie[0].replace(/^\s+|\s+$/g, '');
        if (cookie_name == check_name)
        {
            b_cookie_found = true;
            if (a_temp_cookie.length > 1)
            {
                cookie_value = unescape(a_temp_cookie[1].replace(/^\s+|\s+$/g, ''));
            }
            return cookie_value;
            break;
        }
        a_temp_cookie = null;
        cookie_name = '';
    }
    if (!b_cookie_found)
    {
        return null;
    }
}
function getInternetExplorerVersion()
    // Returns the version of Internet Explorer or a -1
    // (indicating the use of another browser).
{
    var rv = -1; // Return value assumes failure.
    if (navigator.appName == 'Microsoft Internet Explorer')
    {
        var ua = navigator.userAgent;
        var re = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
        if (re.exec(ua) != null)
            rv = parseFloat(RegExp.$1);
    }
    return rv;
}
YAHOO.util.Event.onAvailable('menu-panel-button_dummy',
        function() {
        //disable left menu depending on the browser version
	    if (/Firefox[\/\s](\d+\.\d+)/.test(navigator.userAgent)){ //test for Firefox/x.x or Firefox x.x (ignoring remaining digits);
	 	var ffversion=new Number(RegExp.$1); // capture x.x portion and store as a number 	
		 if (parseFloat(ffversion)<3.5){
		  	document.getElementById('vertical-menu-container').style.display = "none";
	        	return;	 	
		 }
	    }
	    if(document.getElementById('loginbox') != null){
	        document.getElementById('vertical-menu-container').style.display = "none";
	        return;
	    }

            
            document.getElementById('vertical-menu-container').style.display = "";
            var menuSliderTxt1 = "<span>Main</span>";
            var menuSliderTxt2 = "<span>Monitor</span>";
            var menuSliderTxt3 = "<span>Configure</span>";
            var menuSliderTxt4 = "<span>Tools</span>";
            var menuSliderTxt5 = "<span>Extensions</span>";
            if(getInternetExplorerVersion()!=-1){
                menuSliderTxt1 = '<span class="ie">Main</span>';
                menuSliderTxt2 = '<span class="ie">Monitor</span>';
                menuSliderTxt3 = '<span class="ie">Configure</span>';
                menuSliderTxt4 = '<span class="ie">Tools</span>';
                menuSliderTxt5 = '<span class="ie">Extensions</span>';
            }
            var menuSlider0 = document.getElementById('menu-panel-button0');
            var menuSlider1 = document.getElementById('menu-panel-button1');
            var menuSlider2 = document.getElementById('menu-panel-button2');
            var menuSlider3 = document.getElementById('menu-panel-button3');
            var menuSlider4 = document.getElementById('menu-panel-button4');
            var menuSlider5 = document.getElementById('menu-panel-button5');
            var menuPanel = document.getElementById('menu-panel');
            
            if(document.getElementById('region4_monitor_menu') == null){
            	menuSlider2.style.display = "none";
            }
            if(document.getElementById('region1_configure_menu') == null){
            	menuSlider3.style.display = "none";		
            }
            if(document.getElementById('region5_tools_menu') == null){
            	menuSlider4.style.display = "none";
            }
            if(document.getElementById('region3_extensions_menu') == null){
                menuSlider5.style.display = "none";
            }

            menuSlider1.innerHTML = menuSliderTxt1;
            menuSlider2.innerHTML = menuSliderTxt2;
            menuSlider3.innerHTML = menuSliderTxt3;
            menuSlider4.innerHTML = menuSliderTxt4;
            menuSlider5.innerHTML = menuSliderTxt5;
            if (get_cookie('menuPanel') == null || get_cookie('menuPanel') == "visible") {
                menuPanel.style.display = "";

                YAHOO.util.Dom.removeClass(menuSlider0, 'hiddenToShow');
                YAHOO.util.Dom.addClass(menuSlider0, 'showToHidden');
            } else {
                menuPanel.style.display = "none";

                YAHOO.util.Dom.removeClass(menuSlider0, 'showToHidden');
                YAHOO.util.Dom.addClass(menuSlider0, 'hiddenToShow');
            }
             if (get_cookie('menuPanelType') == null || get_cookie('menuPanelType') == "main") { //Set the menu to main
                //show the main section
                var elmsX = YAHOO.util.Selector.query("#menu-table ul.main > li");
                 for(var i=0;i<elmsX.length;i++){
                     elmsX[i].style.display = "";
                 }
                 hideSection("region1_configure_menu");
                 hideSection("region4_monitor_menu");
                 hideSection("region5_tools_menu");
                 hideSection("region3_extensions_menu");
                 selectTab(menuSlider1);

             }else if(get_cookie('menuPanelType') == "monitor"){
                //hide the config section
                 var elmsX = YAHOO.util.Selector.query("#menu-table ul.main > li");
                 for(var i=0;i<elmsX.length;i++){
                     elmsX[i].style.display = "none";
                 }
                 showSection("region4_monitor_menu");
                 hideSection("region1_configure_menu");
                 hideSection("region5_tools_menu");
                 hideSection("region3_extensions_menu");
                 selectTab(menuSlider2);
             }else if(get_cookie('menuPanelType') == "config"){
                //hide the config section
                 var elmsX = YAHOO.util.Selector.query("#menu-table ul.main > li");
                 for(var i=0;i<elmsX.length;i++){
                     elmsX[i].style.display = "none";
                 }
                 showSection("region1_configure_menu");
                 hideSection("region4_monitor_menu");
                 hideSection("region5_tools_menu");
                 hideSection("region3_extensions_menu");
                 selectTab(menuSlider3);
             }else if(get_cookie('menuPanelType') == "tools"){
                //hide the config section
                 var elmsX = YAHOO.util.Selector.query("#menu-table ul.main > li");
                 for(var i=0;i<elmsX.length;i++){
                     elmsX[i].style.display = "none";
                 }
                 showSection("region5_tools_menu");
                 hideSection("region1_configure_menu");
                 hideSection("region4_monitor_menu");
                 hideSection("region3_extensions_menu");
                 selectTab(menuSlider4);                 
             }else if(get_cookie('menuPanelType') == "extensions"){
                //hide the config section
                 var elmsX = YAHOO.util.Selector.query("#menu-table ul.main > li");
                 for(var i=0;i<elmsX.length;i++){
                     elmsX[i].style.display = "none";
                 }
                 showSection("region3_extensions_menu");
                 hideSection("region1_configure_menu");
                 hideSection("region4_monitor_menu");
                 hideSection("region5_tools_menu");
                 selectTab(menuSlider5);
             }
            YAHOO.util.Event.on(menuSlider0, "click", function(e) { //arrow click
                if (menuPanel.style.display == "") {
                    menuPanel.style.display = "none";
                    YAHOO.util.Dom.removeClass(menuSlider0, 'showToHidden');
                    YAHOO.util.Dom.addClass(menuSlider0, 'hiddenToShow');
                    document.cookie = "menuPanel=none;path=/;expires=" + cookie_date.toGMTString();
                    onMenuSlide.fire('invisible');
                } else {
                    menuPanel.style.display = "";
                    YAHOO.util.Dom.removeClass(menuSlider0, 'hiddenToShow');
                    YAHOO.util.Dom.addClass(menuSlider0, 'showToHidden');
                    document.cookie = "menuPanel=visible;path=/;expires=" + cookie_date.toGMTString();
                    onMenuSlide.fire('visible');
                }
            });
            YAHOO.util.Event.on(menuSlider1, "click", function(e) {    //Handle click for main menu
                menuPanel.style.display = "";
                YAHOO.util.Dom.removeClass(menuSlider0, 'hiddenToShow');
                YAHOO.util.Dom.addClass(menuSlider0, 'showToHidden');
                document.cookie = "menuPanel=visible;path=/;expires=" + cookie_date.toGMTString();
                document.cookie = "menuPanelType=main;path=/;expires=" + cookie_date.toGMTString();
                onMenuSlide.fire('visible');

                //show the main section
                var elmsX = YAHOO.util.Selector.query("#menu-table ul.main > li");
                 for(var i=0;i<elmsX.length;i++){
                     elmsX[i].style.display = "";
                 }
                hideSection('region4_monitor_menu');
                hideSection('region1_configure_menu');
                hideSection('region5_tools_menu');
                hideSection('region3_extensions_menu');

                setMainMenus();
                selectTab(menuSlider1);

            });
            YAHOO.util.Event.on(menuSlider2, "click", function(e) {     //Handle click for Monitor menu
                menuPanel.style.display = "";
                YAHOO.util.Dom.removeClass(menuSlider0, 'hiddenToShow');
                YAHOO.util.Dom.addClass(menuSlider0, 'showToHidden');
                document.cookie = "menuPanel=visible;path=/;expires=" + cookie_date.toGMTString();
                document.cookie = "menuPanelType=monitor;path=/;expires=" + cookie_date.toGMTString();
                onMenuSlide.fire('visible');

                //hide the config section
                 var elmsX = YAHOO.util.Selector.query("#menu-table ul.main > li");
                 for(var i=0;i<elmsX.length;i++){
                     elmsX[i].style.display = "none";
                 }
                showSection('region4_monitor_menu');

                setMainMenus();
                selectTab(menuSlider2);

            });
            YAHOO.util.Event.on(menuSlider3, "click", function(e) {     //Handle click for config menu
                menuPanel.style.display = "";
                YAHOO.util.Dom.removeClass(menuSlider0, 'hiddenToShow');
                YAHOO.util.Dom.addClass(menuSlider0, 'showToHidden');
                document.cookie = "menuPanel=visible;path=/;expires=" + cookie_date.toGMTString();
                document.cookie = "menuPanelType=config;path=/;expires=" + cookie_date.toGMTString();
                onMenuSlide.fire('visible');

                //hide the config section
                 var elmsX = YAHOO.util.Selector.query("#menu-table ul.main > li");
                 for(var i=0;i<elmsX.length;i++){
                     elmsX[i].style.display = "none";
                 }
                showSection('region1_configure_menu');
                setMainMenus();
                selectTab(menuSlider3);
            });
            YAHOO.util.Event.on(menuSlider4, "click", function(e) {     //Handle click for tools menu
                menuPanel.style.display = "";
                YAHOO.util.Dom.removeClass(menuSlider0, 'hiddenToShow');
                YAHOO.util.Dom.addClass(menuSlider0, 'showToHidden');
                document.cookie = "menuPanel=visible;path=/;expires=" + cookie_date.toGMTString();
                document.cookie = "menuPanelType=tools;path=/;expires=" + cookie_date.toGMTString();
                onMenuSlide.fire('visible');

                //hide the config section
                 var elmsX = YAHOO.util.Selector.query("#menu-table ul.main > li");
                 for(var i=0;i<elmsX.length;i++){
                     elmsX[i].style.display = "none";
                 }
                showSection('region5_tools_menu');

                setMainMenus();
                selectTab(menuSlider4);                                 

            });
            YAHOO.util.Event.on(menuSlider5, "click", function(e) {     //Handle click for extensions menu
                menuPanel.style.display = "";
                YAHOO.util.Dom.removeClass(menuSlider0, 'hiddenToShow');
                YAHOO.util.Dom.addClass(menuSlider0, 'showToHidden');
                document.cookie = "menuPanel=visible;path=/;expires=" + cookie_date.toGMTString();
                document.cookie = "menuPanelType=extensions;path=/;expires=" + cookie_date.toGMTString();
                onMenuSlide.fire('visible');

                //hide the config section
                 var elmsX = YAHOO.util.Selector.query("#menu-table ul.main > li");
                 for(var i=0;i<elmsX.length;i++){
                     elmsX[i].style.display = "none";
                 }
                showSection('region3_extensions_menu');

                setMainMenus();
                selectTab(menuSlider5);

            });
            setMainMenus();            
        }
);
function hideSection(sectionId) {
    if(sectionId !=null && document.getElementById(sectionId)!=null){
    document.getElementById(sectionId).style.display = "none";
    nextObject(document.getElementById(sectionId)).style.display = "none";
    }
}
function showSection(sectionId) {
    if(sectionId !=null && document.getElementById(sectionId)!=null){
    document.getElementById(sectionId).style.display = "";
    nextObject(document.getElementById(sectionId)).style.display = "";
    }
}
function selectTab(tab){
    var elmsX = YAHOO.util.Selector.query(".vertical-menu-container > div");
    for(var i = 0; i<elmsX.length;i++){
        YAHOO.util.Dom.removeClass(elmsX[i], 'selected');
    }
    YAHOO.util.Dom.addClass(tab, 'selected');            
}
jQuery(document).ready(
  function() {
      if (jQuery('#menu-table li a').length <= 1) {
          document.getElementById('vertical-menu-container').style.display = "none";
          document.getElementById('menu-panel').style.display = "none";
      }

  }
);
