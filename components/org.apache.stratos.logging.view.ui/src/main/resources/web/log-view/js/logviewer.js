/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

/*
log.js contains scripts need to handle log information.
*/
function viewSingleLogLevels() {
	  var loglevel = document.getElementById("logLevelID");
	    var loglevel_index = null;
	    var loglevel_value = null;
	    if (loglevel != null)
	    {
	        loglevel_index = loglevel.selectedIndex;
	        if (loglevel_index != null) {
	            loglevel_value = loglevel.options[loglevel_index].value;
	        }
	    }
	    if (loglevel_value != null && loglevel_value != "") {
	        location.href = "index.jsp?type=" + loglevel_value;
	    } else {
	        return;
	    }
}

function viewApplicationLogs() {
	  var loglevel = document.getElementById("logLevelID");
	  var appName = document.getElementById("appName");
	    var loglevel_index = null;
	    var loglevel_value = null;
	    var appName_index = null;
	    var appName_value = null;
	    if (loglevel != null)
	    {
	        loglevel_index = loglevel.selectedIndex;
	        if (loglevel_index != null) {
	            loglevel_value = loglevel.options[loglevel_index].value;
	        }
	    }
	    
	    if (appName != null)
	    {
	    	appName_index = appName.selectedIndex;
	        if (appName_index != null) {
	        	appName_value = appName.options[appName_index].value;
	        }
	    }
	    if (loglevel_value == "Custom") {
			loglevel_value = "ALL";
		}
	    if (loglevel_value != null && loglevel_value != "") {
	        location.href = "application_log_viewer.jsp?type=" + loglevel_value +"&appName="+appName_value;
	    } else if (appName_value != null && appName_value != "") {
	    	  location.href = "application_log_viewer.jsp?type=" + loglevel_value +"&appName="+appName_value;
	    } else {
	    	return;
	    }
}

function viewSingleLogLevel() {
    var loglevel = document.getElementById("logLevelID");
	var tenantDomain = document.getElementById("tenantDomain").value;
	var serviceName = document.getElementById("serviceName").value;
	var logFile = document.getElementById("logFile").value;
    var loglevel_index = null;
    var loglevel_value = null;
    if (loglevel != null)
    {
        loglevel_index = loglevel.selectedIndex;
        if (loglevel_index != null) {
            loglevel_value = loglevel.options[loglevel_index].value;
        }
    }
    if (loglevel_value != null && loglevel_value != "") {
        location.href = "syslog_index.jsp?type=" + loglevel_value+"&logFile="+logFile+"&tenantDomain="+tenantDomain+"&serviceName="+serviceName;
    } else {
        return;
    }

}

function getTenantSpecificIndex () {
    var loglevel = document.getElementById("logLevelID");
    var loglevel_index = null;
    var loglevel_value = null;
    var tenantDomain = document.getElementById("tenantDomain").value;
    var serviceName = document.getElementById("serviceName");
    var servicelevel_idex = null;
    var service_value = null;
    if (loglevel != null)
    {
        loglevel_index = loglevel.selectedIndex;
        if (loglevel_index != null) {
            loglevel_value = loglevel.options[loglevel_index].value;
        }
        if (loglevel_value == "Custom") {
            loglevel_value = "ALL";
        }
    }
    if(serviceName !=  null)
    {
        servicelevel_idex = serviceName.selectedIndex;
        if(servicelevel_idex != null) {
            service_value = serviceName.options[servicelevel_idex].value;
        }
    }
    var keyword = document.getElementById("logkeyword");
    if (keyword != null && keyword != undefined && keyword.value != null && keyword.value != undefined) {
        if (keyword.value == "") {
            if(serviceName != null) {
                location.href = "index.jsp?type=" + loglevel_value + "&tenantDomain=" + tenantDomain;
            } else {
                location.href = "index.jsp?type=" + loglevel_value + "&tenantDomain=" + tenantDomain + "&serviceName="+serviceName;
            }
        } else {
            if(serviceName != null) {
                location.href = "index.jsp?type=" + loglevel_value + "&tenantDomain="+
                    tenantDomain+"&serviceName="+serviceName+"&keyword=" + keyword.value;
            } else {
                location.href = "index.jsp?type=" + loglevel_value + "&tenantDomain="+
                    tenantDomain +"&keyword=" + keyword.value;
            }
        }
    } else {
        return;
    }

}

function getTenantApplicationSpecificIndex() {
    var loglevel = document.getElementById("logLevelID");
    var appName = document.getElementById("appName");
    var loglevel_index = null;
    var loglevel_value = null;
    var appName_index = null;
    var appName_value = null;
    var servicelevel_idex = null;
    var service_value = null;
    var tenantDomain = document.getElementById("tenantDomain");
    var serviceName = document.getElementById("serviceName");
    if (loglevel != null)
    {
        loglevel_index = loglevel.selectedIndex;
        if (loglevel_index != null) {
            loglevel_value = loglevel.options[loglevel_index].value;
        }
    }

    if (appName != null)
    {
        appName_index = appName.selectedIndex;
        if (appName_index != null) {
            appName_value = appName.options[appName_index].value;
        }
    }

    if(serviceName !=  null)
    {
        servicelevel_idex = serviceName.selectedIndex;
        if(servicelevel_idex != null) {
            service_value = serviceName.options[servicelevel_idex].value;
        }
    }

    if (loglevel_value == "Custom") {
        loglevel_value = "ALL";
    }
    var keyword = document.getElementById("logkeyword");
    if (keyword != null && keyword != undefined ) {
        if(keyword.value == "") {
            if(serviceName != null) {
                location.href = "application_log_viewer.jsp?type=" + loglevel_value + "&serviceName="+service_value
                    + "&appName="+appName_value + "&tenantDomain=" + tenantDomain.value;
            } else {
                location.href = "application_log_viewer.jsp?type=" + loglevel_value + "&appName="+appName_value +
                    "&tenantDomain=" + tenantDomain.value ;
            }
        } else {
            if(serviceName != null) {
                location.href = "application_log_viewer.jsp?type="+loglevel_value + "&keyword=" + keyword.value +
                    "&serviceName="+service_value + "&appName="+appName_value;
            } else {
                location.href = "application_log_viewer.jsp?type="+loglevel_value + "&keyword=" + keyword.value +
                    "&appName="+appName_value;
            }
        }
    } else {
        return;
    }


}

function getTenantProductSpecificIndex() {
    var loglevel = document.getElementById("logLevelID");
    var appName = document.getElementById("appName");
    var loglevel_index = null;
    var loglevel_value = null;
    var appName_index = null;
    var appName_value = null;
    var servicelevel_idex = null;
    var service_value = null;
    var tenantDomain = document.getElementById("tenantDomain");
    var serviceName = document.getElementById("serviceName");
    if (loglevel != null)
    {
        loglevel_index = loglevel.selectedIndex;
        if (loglevel_index != null) {
            loglevel_value = loglevel.options[loglevel_index].value;
        }
    }

    if(serviceName !=  null)
    {
        servicelevel_idex = serviceName.selectedIndex;
        if(servicelevel_idex != null) {
            service_value = serviceName.options[servicelevel_idex].value;
        }
    }

    if (appName != null)
    {
        appName_index = appName.selectedIndex;
        if (appName_index != null) {
            appName_value = appName.options[appName_index].value;
        }
    }
    if (loglevel_value == "Custom") {
        loglevel_value = "ALL";
    }
    var keyword = document.getElementById("logkeyword");
    if (keyword != null && keyword != undefined) {
        if (keyword.value == "") {
            if(tenantDomain != null && tenantDomain != undefined ) {
                location.href = "application_log_viewer.jsp?type=" + loglevel_value +"&appName="+appName_value+
                    "&tenantDomain=" + tenantDomain.value + "&serviceName=" + service_value;
            } else {
                location.href = "application_log_viewer.jsp?type=" + loglevel_value +"&appName="+appName_value+
                     "&serviceName=" + service_value;
            }
        } else {
            if(tenantDomain != null && tenantDomain != undefined ) {
                location.href = "application_log_viewer.jsp?type=" + loglevel_value +"&appName="+appName_value+
                    "&tenantDomain=" + tenantDomain.value + "&serviceName=" + service_value + "&keyword=" + keyword.value;
            } else {
                location.href = "application_log_viewer.jsp?type=" + loglevel_value +"&appName="+appName_value+
                    "&serviceName=" + service_value + "&keyword=" + keyword.value;
            }
        }
    } else {
        return;
    }
}

function getProductTenantSpecificIndex() {
    var loglevel = document.getElementById("logLevelID");
    var loglevel_index = null;
    var loglevel_value = null;
    var tenantDomain = document.getElementById("tenantDomain");

    var serviceName = document.getElementById("serviceName");
    if (loglevel != null)
    {
        loglevel_index = loglevel.selectedIndex;
        if (loglevel_index != null) {
            loglevel_value = loglevel.options[loglevel_index].value;
        }
        if (loglevel_value == "Custom") {
            loglevel_value = "ALL";
        }
    }
    var keyword = document.getElementById("logkeyword");
    if (keyword != null && keyword != undefined) {
        if (keyword.value == "") {
            if(tenantDomain != null && tenantDomain != undefined ) {
                location.href = "index.jsp?type=" + loglevel_value + "&tenantDomain="+tenantDomain.value + "&serviceName="+serviceName.value;
            } else {
                location.href = "index.jsp?type=" + loglevel_value + "&serviceName=" + serviceName.value;
            }

        } else {
            if(tenantDomain != null && tenantDomain != undefined ) {
                location.href = "index.jsp?type=" + loglevel_value + "&serviceName=" + serviceName.value +
                    "&tenantDomain=" + tenantDomain.value + "&keyword=" + keyword.value;
            } else {
                location.href = "index.jsp?type=" + loglevel_value + "&serviceName=" + serviceName.value +
                    "&keyword=" + keyword.value;
            }
        }
    } else {
        return;
    }
}

function submitenter(e) {
	var keycode;
	if (window.event) {
		keycode = window.event.keyCode;
	} else if (e) {
		keycode = e.which;
	}
	if (keycode == 13) {
		searchLogs();
		return true;
	} else {
		return true;
	}
}

function submitenterNormal(e) {
    var keycode;
    if (window.event) {
        keycode = window.event.keyCode;
    } else if (e) {
        keycode = e.which;
    }
    if (keycode == 13) {
        searchNormal();
        return true;
    } else {
        return true;
    }
}

function submitenterNormalManager(e) {
    var keycode;
    if (window.event) {
        keycode = window.event.keyCode;
    } else if (e) {
        keycode = e.which;
    }
    if (keycode == 13) {
        searchNormalManager();
        return true;
    } else {
        return true;
    }
}
function submitenterTenant(e) {
    var keycode;
    if (window.event) {
        keycode = window.event.keyCode;
    } else if (e) {
        keycode = e.which;
    }
    if (keycode == 13) {
        getTenantSpecificIndex ();
        return true;
    } else {
        return true;
    }
}

function submitenterbottomUp(e)
{
    var keycode;
    if (window.event) {
        keycode = window.event.keyCode;
    }
    else if (e) {
        keycode = e.which;
    }
    if (keycode == 13)
    {
    	searchLogBottomLogs();
        return true;
    }
    else {
        return true;
    }
}

function isNumeric(str)
{
	var validChars = "0123456789";
	var isNumber = true;
	var char;
	for (i = 0; i < str.length && isNumber == true; i++) {
		char = str.charAt(i);
		if (validChars.indexOf(char) == -1) {
			isNumber = false;
		}
	}
	return isNumber;
}


function searchLogBottomLogs() {
	var logFile = document.getElementById("logFile").value;
	var log_index = document.getElementById("logIndex").value;
	var loglevel = document.getElementById("logLevelID");
	var serviceName = document.getElementById("serviceName").value;
	var loglevel_index = null;
	var loglevel_value = null;
	var tenantDomain = document.getElementById("tenantDomain").value;
    if(log_index == ''){
        CARBON.showWarningDialog('Head index cannot be empty');
        return false;
    }
    if(! isNumeric(log_index)) {
    	 CARBON.showWarningDialog('Enter non negative numeric values for log head');
         return false;
    }
    if(log_index < 1){
    	CARBON.showWarningDialog('Log index should be between 1 and 10000000');
        return false;
    }
    if(log_index > 10000000){
        CARBON.showWarningDialog('Log index should be between 1 and 10000000');
        return false;
    }
	if (loglevel != null) {
		loglevel_index = loglevel.selectedIndex;
		if (loglevel_index != null) {
			loglevel_value = loglevel.options[loglevel_index].value;
			if (loglevel_value == "Custom") {
				loglevel_value = "ALL";
			}
		}
	}
	var keyword = document.getElementById("keyword");
	if (keyword != null && keyword != undefined && keyword.value != null
        && keyword.value != undefined) {
		if (keyword.value == "") {
			location.href = "view.jsp?type=ALL&logIndex=" + log_index+"&logFile="+logFile+"&tenantDomain="+tenantDomain+"&serviceName="+serviceName;
		} else {
			location.href = "view.jsp?type=" + loglevel_value + "&keyword="
					+ keyword.value+"&logIndex=" + log_index+"&logFile="+logFile+"&tenantDomain="+tenantDomain+"&serviceName="+serviceName;
		}
	} else {
		return;
	}
}
function searchLogs() {
    var loglevel = document.getElementById("logLevelID");
    var tenantDomain = document.getElementById("tenantDomain");
    var serviceName = document.getElementById("serviceName");
    var loglevel_index = null;
    var loglevel_value = null;
    var servicelevel_idex = null;
    var service_value = null;
    var collapseVal = document.getElementById("propertyTableSearch");
    var collapse = "false";

    if (loglevel != null)
    {
        loglevel_index = loglevel.selectedIndex;
        if (loglevel_index != null) {
            loglevel_value = loglevel.options[loglevel_index].value;
        } 
        if (loglevel_value == "Custom") {
			loglevel_value = "ALL";
		}
    }
    if(collapseVal.style.display == "") {
        collapse = "true";
    }

    if(serviceName !=  null)
    {
        servicelevel_idex = serviceName.selectedIndex;
        if(servicelevel_idex != null) {
            service_value = serviceName.options[servicelevel_idex].value;
        }
    }
    var propertyTab = document.getElementById('propertyTable');
    var propertySymbolMax =  document.getElementById('propertySymbolMax');

    var keyword = document.getElementById("logkeyword");
    if (keyword != null && keyword != undefined) {
        if (keyword.value == "") {
            if(tenantDomain != null && tenantDomain != undefined ) {
                if(serviceName != null) {
                    location.href = "index.jsp?type="+loglevel_value+"&tenantDomain=" + tenantDomain.value +
                        "&serviceName="+service_value + "&collapse=" + collapse;
                } else {
                    location.href = "index.jsp?type="+loglevel_value+"&tenantDomain=" + tenantDomain.value +
                        "&collapse=" + collapse;
                }
            } else {
                if(serviceName != null) {
                    location.href = "index.jsp?type="+loglevel_value+"&serviceName=" + service_value + "&collapse=" + collapse;
                } else {
                    location.href = "index.jsp?type=" + loglevel_value + "&collapse=" + collapse;
                }
            }
        } else {
            if(tenantDomain != null && tenantDomain != undefined ) {
                if(serviceName != null) {
                    location.href = "index.jsp?type="+loglevel_value + "&tenantDomain="+tenantDomain.value  +
                        "&serviceName=" + service_value+ "&keyword="+ keyword.value + "&collapse=" + collapse;
                } else {
                    location.href = "index.jsp?type=" + loglevel_value + "&tenantDomain="+tenantDomain.value +
                        "&keyword=" + keyword.value + "&collapse=" + collapse;
                }

            } else {
                if(serviceName != null) {
                    location.href = "index.jsp?type="+loglevel_value + "&serviceName="+service_value +
                        "&keyword=" + keyword.value + "&collapse=" + collapse;
                } else {
                    location.href = "index.jsp?type="+loglevel_value +
                        "&keyword=" + keyword.value + "&collapse=" + collapse;
                }

            }
        }
    } else {
        return ;
    }
}

function searchNormal() {
    var loglevel = document.getElementById("logLevelID");
    var loglevel_index = null;
    var loglevel_value = null;
    if (loglevel != null)
    {
        loglevel_index = loglevel.selectedIndex;
        if (loglevel_index != null) {
            loglevel_value = loglevel.options[loglevel_index].value;
        }
        if (loglevel_value == "Custom") {
            loglevel_value = "ALL";
        }
    }
    var keyword = document.getElementById("logkeyword");
    if (keyword != null && keyword != undefined && keyword.value != null && keyword.value != undefined) {
        if (keyword.value == "") {
            location.href = "index.jsp?type=" + loglevel_value;
        } else {
            location.href = "index.jsp?type=" + loglevel_value + "&keyword=" + keyword.value;
        }
    } else {
        return;
    }
}

function searchNormalManager() {
    var loglevel = document.getElementById("logLevelID");
    var serviceName = document.getElementById("serviceName");
    var servicelevel_idex = null;
    var service_value = null;
    var loglevel_index = null;
    var loglevel_value = null;
    var collapseVal = document.getElementById("propertyTableSearch");
    var collapse = "false";
    if (loglevel != null)
    {
        loglevel_index = loglevel.selectedIndex;
        if (loglevel_index != null) {
            loglevel_value = loglevel.options[loglevel_index].value;
        }
        if (loglevel_value == "Custom") {
            loglevel_value = "ALL";
        }
    }
    if(collapseVal.style.display == "") {
        collapse = "true";
    }

    if(serviceName !=  null)
    {
        servicelevel_idex = serviceName.selectedIndex;
        if(servicelevel_idex != null) {
            service_value = serviceName.options[servicelevel_idex].value;
        }
    }
    var keyword = document.getElementById("logkeyword");
    if (keyword != null && keyword != undefined && keyword.value != null && keyword.value != undefined) {
        if (keyword.value == "") {
            location.href = "index.jsp?type=" + loglevel_value + "&serviceName=" + service_value + "&collapse=" + collapse;
        } else {
            location.href = "index.jsp?type=" + loglevel_value + "&keyword=" + keyword.value +
                                "&serviceName=" + service_value + "&collapse=" + collapse;
        }
    } else {
        return ;
    }
}


function showQueryProperties() {
    var propertyTab = document.getElementById('propertyTable');
    var propertySymbolMax =  document.getElementById('propertySymbolMax');
    if(propertyTab.style.display == 'none') {
        propertyTab.style.display = '';
        propertySymbolMax.setAttribute('style','background-image:url(images/minus.gif);');
    } else {
        propertyTab.style.display = 'none';
        propertySymbolMax.setAttribute('style','background-image:url(images/plus.gif);');
    }
}

function showQueryPropertiesSearch() {
    var propertyTab = document.getElementById('propertyTableSearch');
    var propertySymbolMax =  document.getElementById('propertySymbolMaxSearch');
    if(propertyTab.style.display == 'none') {
        propertyTab.style.display = '';
        propertySymbolMax.setAttribute('style','background-image:url(images/minus.gif);');
    } else {
        propertyTab.style.display = 'none';
        propertySymbolMax.setAttribute('style','background-image:url(images/plus.gif);');
    }
}
function appSubmitenter(e) {
    var keycode;
    if (window.event) {
        keycode = window.event.keyCode;
    } else if (e) {
        keycode = e.which;
    }
    if (keycode == 13) {
        searchAppLogs();
        return true;
    } else {
        return true;
    }
}

function appSubmitenterTenant(e) {
    var keycode;
    if (window.event) {
        keycode = window.event.keyCode;
    } else if (e) {
        keycode = e.which;
    }
    if (keycode == 13) {
        getTenantApplicationSpecificIndex();
        return true;
    } else {
        return true;
    }
}
function searchAppLogs() {
    var loglevel = document.getElementById("logLevelID");
    var appName = document.getElementById("appName");
    var loglevel_index = null;
    var loglevel_value = null;
    var appName_index = null;
    var appName_value = null;
    if (loglevel != null)
    {
        loglevel_index = loglevel.selectedIndex;
        if (loglevel_index != null) {
            loglevel_value = loglevel.options[loglevel_index].value;
        }
    }
    if (loglevel_value == "Custom") {
        loglevel_value = "ALL";
    }
    if (appName != null)
    {
        appName_index = appName.selectedIndex;
        if (appName_index != null) {
            appName_value = appName.options[appName_index].value;
        }
    }
    var keyword = document.getElementById("logkeyword");
    if (keyword != null && keyword != undefined) {
        if (keyword.value == "") {
            location.href = "application_log_viewer.jsp?type=" + loglevel_value + "&appName="+appName_value;
        } else {
            location.href = "application_log_viewer.jsp?type=" + loglevel_value + "&keyword=" + keyword.value+"&appName="+appName_value;
        }
    } else {
        return;
    }
}

function searchAppLogsAdv() {
    var loglevel = document.getElementById("logLevelID");
    var appName = document.getElementById("appName");

    var tenantDomain = document.getElementById("tenantDomain");
    var serviceName = document.getElementById("serviceName");

    var servicelevel_idex = null;
    var service_value = null;
    var loglevel_index = null;
    var loglevel_value = null;
    var appName_index = null;
    var appName_value = null;
    if (loglevel != null)
    {
        loglevel_index = loglevel.selectedIndex;
        if (loglevel_index != null) {
            loglevel_value = loglevel.options[loglevel_index].value;
        }
    }
    if (loglevel_value == "Custom") {
        loglevel_value = "ALL";
    }
    if (appName != null)
    {
        appName_index = appName.selectedIndex;
        if (appName_index != null) {
            appName_value = appName.options[appName_index].value;
        }
    }

    if(serviceName !=  null)
    {
        servicelevel_idex = serviceName.selectedIndex;
        if(servicelevel_idex != null) {
            service_value = serviceName.options[servicelevel_idex].value;
        }
    }
    var keyword = document.getElementById("logkeyword");
    if (keyword != null && keyword != undefined) {
        if (keyword.value == "") {
            if(tenantDomain != null && tenantDomain != undefined ) {
                if(serviceName != null) {
                    location.href = "application_log_viewer.jsp?type="+loglevel_value+"&tenantDomain=" +
                        tenantDomain.value + "&serviceName="+service_value + "&appName=" + appName_value;
                } else {
                    location.href = "application_log_viewer.jsp?type="+loglevel_value+"&tenantDomain=" +
                        tenantDomain.value + "&appName=" + appName_value;
                }
            } else {
                if(serviceName != null) {
                    location.href = "application_log_viewer.jsp?type="+loglevel_value+"&serviceName=" + service_value +
                        "&appName=" + appName_value;
                } else {
                    location.href = "application_log_viewer.jsp?type=" + loglevel_value + "&appName=" + appName_value;
                }
            }
        } else {
            if(tenantDomain != null && tenantDomain != undefined ) {
                if(serviceName != null) {
                    location.href = "application_log_viewer.jsp?type="+loglevel_value + "&tenantDomain="+
                        tenantDomain.value  +
                        "&serviceName=" + service_value+ "&keyword="+ keyword.value + "&appName=" + appName_value;
                } else {
                    location.href = "application_log_viewer.jsp?type=" + loglevel_value + "&tenantDomain="+tenantDomain.value +
                        "&keyword=" + keyword.value + "&appName=" + appName_value;
                }

            } else {
                if(serviceName != null) {
                    location.href = "application_log_viewer.jsp?type="+loglevel_value + "&serviceName="+service_value +
                        "&keyword=" + keyword.value + "&appName=" + appName_value;
                } else {
                    location.href = "application_log_viewer.jsp?type="+loglevel_value +
                        "&keyword=" + keyword.value + "&appName=" + appName_value;
                }

            }
        }
    } else {
        return;
    }
}

function searchLog111() {
    var loglevel = document.getElementById("logLevelID");
	var logFile = document.getElementById("logFile").value;
	var serviceName = document.getElementById("serviceName").value;
    var loglevel_index = null;
    var loglevel_value = null;
	var tenantDomain = document.getElementById("tenantDomain").value;
    if (loglevel != null)
    {
        loglevel_index = loglevel.selectedIndex;
        if (loglevel_index != null) {
            loglevel_value = loglevel.options[loglevel_index].value;
        }
    }
    var keyword = document.getElementById("keyword");
    if (keyword != null && keyword != undefined && keyword.value != null && keyword.value != undefined) {
        if (keyword.value == "") {
            location.href = "syslog_index.jsp?type=ALL"+"&logFile="+logFile+"&tenantDomain="+tenantDomain;
        } else {
            location.href = "syslog_index.jsp?type=" + loglevel_value + "&keyword=" + keyword.value+"&logFile="+logFile+"&tenantDomain="+tenantDomain+"&serviceName="+serviceName;
        }
    } else {
        return;
    }
}

function clearLogEntries(message) {
    CARBON.showConfirmationDialog(message, function() {
        location.href = "syslog_index.jsp?action=clear-logs";
    });
}

function viewSingleSysLogLevel() {
	var loglevel = document.getElementById("logLevelID");
	var serviceName = document.getElementById("serviceName").value;
	var logFile = document.getElementById("logFile").value;
	var tenantDomain = document.getElementById("tenantDomain").value;
	var loglevel_index = null;
	var loglevel_value = null;
	if (loglevel != null) {
		loglevel_index = loglevel.selectedIndex;
		if (loglevel_index != null) {
			loglevel_value = loglevel.options[loglevel_index].value;
		}
	}
	if (loglevel_value != null && loglevel_value != "") {
		location.href = "view.jsp?type=" + loglevel_value+"&logFile="+logFile+"&tenantDomain="+tenantDomain+"&serviceName="+serviceName;
	} else {
		return;
	}
}

function clearProperties() {
	document.getElementById("logIndex").value = "";
	document.getElementById("type").value="ALL";
	document.getElementById("keyword").value="";
}

function searchTenantLog() {
	var loglevel = document.getElementById("logLevelID");
	var serviceName = document.getElementById("serviceName").value;
	var logFile = document.getElementById("logFile").value;
	var tenantDomain = document.getElementById("tenantDomain").value;
	var loglevel_index = null;
	var loglevel_value = null;
	if (loglevel != null) {
		loglevel_index = loglevel.selectedIndex;
		if (loglevel_index != null) {
			loglevel_value = loglevel.options[loglevel_index].value;
			if (loglevel_value == "Custom") {
				loglevel_value = "ALL";
			}
		}
	}
	var keyword = document.getElementById("keyword");
	if (keyword != null && keyword != undefined && keyword.value != null
			&& keyword.value != undefined) {
		if (keyword.value == "") {
			location.href = "view.jsp?type=ALL"+"&logFile="+logFile+"&tenantDomain="+tenantDomain+"&serviceName="+serviceName;
		} else {
			location.href = "view.jsp?type=" + loglevel_value
					+ "&keyword=" + keyword.value+"&logFile="+logFile+"&tenantDomain="+tenantDomain+"&serviceName="+serviceName;
		}
	} else {
		return;
	}
}
    
function getFilteredLogs() {
	
	var loglevel = document.getElementById("logLevelID");
	var log_index = document.getElementById("logIndex").value;
	var appName = document.getElementById("appName");
	var start = document.getElementById("start").value;
	var end = document.getElementById("end").value;
	if (document.getElementById("NowradioDate").checked) {
		start='';
		end='';
	}
	var serviceName = null;
	var tenantDomain = null;
	 // regular expression to match required date format
	var reTdate = '\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{1,2}';
	var re = new RegExp(reTdate);
	 
	if(start != '' && !start.match(re)) {  
		CARBON.showWarningDialog('Invalid start date Format');
    	return false; 
    } 
	if(end != '' && !end.match(re)) {  
		CARBON.showWarningDialog('Invalid end date Format');
    	return false; 
    } 
//	// regular expression to match required time format 
//	var reTime = /^\d{1,2}:\d{2}([ap]m)?$/; 
//	if(time != '' && !time.match(reTime)) {  
//		CARBON.showWarningDialog('Invalid Time Format');
//    	return false; 
//    } 

	if (document.getElementById("serviceName") != null) {
		serviceName = document.getElementById("serviceName").value
	}
	if (document.getElementById("tenantDomain") != null) {
		tenantDomain = document.getElementById("tenantDomain").value
	}
	var loglevel_index = null;
	var loglevel_value = null;
	
	if (loglevel != null) {
		loglevel_index = loglevel.selectedIndex;
		if (loglevel_index != null) {
			loglevel_value = loglevel.options[loglevel_index].value;
			if (loglevel_value == "Custom") {
				loglevel_value = "ALL";
			}
		}
	}
	var keyword = document.getElementById("keyword").value;
	var logger = document.getElementById("logger").value;
	if (appName != null) {
		appName = document.getElementById("appName").value;
		location.href = "application_log_viewer.jsp?priority=" + loglevel_value
		+ "&keyword=" + keyword + "&logIndex=" + log_index
		+ "&tenantDomain=" + tenantDomain + "&serviceName=" + serviceName
		+ "&logger=" + logger + "&start=" + start + "&appName=" + appName
		+ "&end=" + end;
	} else {
		location.href = "cassandra_log_viewer.jsp?priority=" + loglevel_value
		+ "&keyword=" + keyword + "&logIndex=" + log_index
		+ "&tenantDomain=" + tenantDomain + "&serviceName=" + serviceName
		+ "&logger=" + logger + "&start=" + start 
		+ "&end=" + end;
	}
	
}



function showTrace(obj) {
    var traceTab = document.getElementById('traceTable'+obj);
    var traceSymbolMax =  document.getElementById('traceSymbolMax'+obj);
    if(traceTab.style.display == 'none') {
        traceTab.style.display = '';
        traceSymbolMax.setAttribute('style','background-image:url(images/minus.gif);');
    } else {
        traceTab.style.display = 'none';
        traceSymbolMax.setAttribute('style','background-image:url(images/plus.gif);');
    }
}

