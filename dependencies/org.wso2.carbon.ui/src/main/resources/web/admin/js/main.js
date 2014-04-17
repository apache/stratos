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
var serviceGroupId;
var userNameString;
var numDaysToKeepCookie = 2;
var locationString = self.location.href;


/*
 * two variables to hold the width and the height of the message box
 */
var messageBoxWidth = 300;
var messageBoxHeight = 90;
var warningMessageImage = 'images/oops.gif';
var informationMessageImage = 'images/information.gif';
var warningnMessagebackColor = '#FFC';
var informationMessagebackColor = '#BBF';
var runPoleHash = false;

/* constants for Message types */
var INFORMATION_MESSAGE = 1;
var WARNING_MESSAGE = 2;

/* == URL and Host. Injected the values using AdminUIServletFilter == */
var URL;
var GURL;
var serverURL;
var HTTP_PORT;
var HTTPS_PORT;
var HTTP_URL;
var HOST;
var SERVICE_PATH;
var ROOT_CONTEXT;
/* ================== */

var lastHash;

var userName;

var isServerRestarting = false;

var tabcount = 0;

var tabCharactors = " ";

var requestFromServerPending = false;

/*
 * mainMenuObject will be used to hold the <a/> objects, that's been used
 * clicked in main menu items.
 */
var mainMenuObjectId = null;
var mainMenuObjectIndex = -1;

var sessionCookieValue;

/*
 * Everything will be related to wso2 namespace. If wso2 object dosenot present
 * create it first
 */
if (typeof(wso2) == "undefined") {
    var wso2 = {};
}

/*
Create the objects with associative style
*/
wso2.namespace = function() {
    var a = arguments, o = null, i, j, d;
    for (i = 0; i < a.length; i = i + 1) {
        d = a[i].split(".");
        o = wso2;

        // wso2 is implied, so it is ignored if it is included
        for (j = (d[0] == "wso2") ? 1 : 0; j < d.length; j = j + 1) {
            o[d[j]] = o[d[j]] || {};
            o = o[d[j]];
        }
    }

    return o;
};

wso2.init = function() {
    this.namespace("wsf");
}
/*Create only wso2.wsf namespace */
wso2.init();

/* Usage of native WSRequest object */
wso2.wsf.READY_STATE_UNINITIALIZED = 0;
wso2.wsf.READY_STATE_LOADING = 1;
wso2.wsf.READY_STATE_LOADED = 2;
wso2.wsf.READY_STATE_INTERACTIVE = 3;
wso2.wsf.READY_STATE_COMPLETE = 4;

/**
 * wso2.wsf.WSRequest is the stub that wraps the native WSRequest to invoke a
 * web service. If the onLoad method is given, this will communicate with the
 * web service async. Sync invocation is not burned into this stub.
 * 
 * If onError method is undefined, default onError will come into play. onError
 * will be invoked if SOAP fault is received.
 * 
 * Usage of onLoad : new
 * wso2.wsf.WSRequest("http://my.web.service","urn:myAction","<foo/>",callback);
 * 
 * callback = function(){ // to get the response xml call this.req.responseXML
 * //to get the response text call this.req.responseText //if an object needs
 * the values of this.req call bar.call(this,x,y,z); this.params;
 *  }
 * 
 * @url : Endpoint referece (EPR)
 * @action : WSA Action for the EPR
 * @payLoad : Pay load to be send
 * @onLoad : Function that should be called when onreadystate has been called
 * @params : Will allow to pass parameters to the callback and later can be used
 * @onError : Function that should be called when an error or SOAP fault has
 *          been received.
 */
wso2.wsf.WSRequest = function(url, action, payLoad, onLoad, params, onError, proxyAddress, accessibleDomain) {
    this.url = url;
    this.payLoad = payLoad;
    this.params = params;
    this.onLoad = (onLoad) ? onLoad : this.defaultOnLoad;
    this.onError = (onError) ? onError : this.defaultError;
    this.req = null;
    this.options = new Array();
    this.options["useBindng"] = "SOAP 1.1";
    this.options["action"] = this._parseAction(action);
    this.options["accessibleDomain"] = accessibleDomain;
    this.proxyAddress = proxyAddress;
    this.loadXMLDoc();
}

wso2.wsf.WSRequest.prototype = {
    /**
     * Action should be a valid URI
     */
    _parseAction : function(action) {
        if (!action) {
            return '""';
        }

        if (action.indexOf("urn:") > -1 ||
            action.indexOf("URN:") > -1 ||
            action.indexOf("http://") > -1) {
            return action;
        }
        return "urn:" + action;

    },
    defaultError : function() {
        var error = this.req.error;
        if (!error) {
            var reason = "";
            var a = arguments;
            if (a.length > 0) {
                reason = a[0];
            }
            // This is to fix problems encountered in Windows browsers.
            var status = this.req._xmlhttp.status;
            if (status && status == 500) {
                return;
            } else {
                CARBON.showErrorDialog("Console has received an error. Please refer" +
                                           " to system admin for more details. " +
                                           reason.toString());
            }

            if (typeof(stoppingRefreshingMethodsHook) != "undefined" &&
                typeof(logoutVisual) != "undefined") {
                stoppingRefreshingMethodsHook();
                logoutVisual();
            }
            return;
        }

        if (error.reason != null) {
            if (typeof (error.reason.indexOf) != "undefined") {
                if (error.reason.indexOf("Access Denied. Please login first") > -1) {
                    if (typeof(stoppingRefreshingMethodsHook) != "undefined" &&
                        typeof(logoutVisual) != "undefined") {
                        stoppingRefreshingMethodsHook();
                        logoutVisual();
                    }
                }
            }
        }

        if (error.detail != null) {
            if (typeof (error.detail.indexOf) != "undefined") {
                if (error.detail.indexOf("NS_ERROR_NOT_AVAILABLE") > -1) {
                    if (typeof(stoppingRefreshingMethodsHook) != "undefined" &&
                        typeof(logoutVisual) != "undefined") {
                        stoppingRefreshingMethodsHook();
                        logoutVisual();
                    }
                }
            }
        }

        CARBON.showErrorDialog(error.reason);

    },

    defaultOnLoad : function() {
        /*default onLoad is reached and do not do anything.*/
    },

    loadXMLDoc : function() {
        try {
            stopWaitAnimation(); /*
									 * This will stop the wait animation if
									 * consecutive requests are made.
									 */
            this.req = new WSRequest();
            this.req.proxyAddress = this.proxyAddress;
            var loader = this;
            if (this.req) {
                executeWaitAnimation();
                this.req.onreadystatechange = function() {
                    loader.onReadyState.call(loader);
                }
                this.req.open(this.options, this.url, true);
                this.req.send(this.payLoad);
            } else {
                stopWaitAnimation()
                wso2.wsf.Util.alertWarning("Native XMLHttpRequest can not be found.")
            }
        } catch(e) {
            stopWaitAnimation();
            wso2.wsf.Util.alertWarning("Erro occured while communicating with the server " +
                                       e.toString());
        }

    },

    onReadyState : function() {
        try {
            var ready = this.req.readyState;
            if (ready == wso2.wsf.READY_STATE_COMPLETE) {
                wso2.wsf.Util.cursorClear();
                stopWaitAnimation();
                var httpStatus;
                if (this.req.sentRequestUsingProxy) {
                    httpStatus = this.req.httpStatus;
                } else {
                    httpStatus = this.req._xmlhttp.status;
                }
                if (httpStatus == 200 || httpStatus == 202) {
                    this.onLoad.call(this);
                } else if (httpStatus >= 400) {
                    this.onError.call(this);
                }
            }
        } catch(e) {
            wso2.wsf.Util.cursorClear();
            stopWaitAnimation();
            this.onError.call(this,e);
        }
    }
};


/*
 * Utility class
 */
wso2.wsf.Util = {
    _msxml : [
            'MSXML2.XMLHTTP.3.0',
            'MSXML2.XMLHTTP',
            'Microsoft.XMLHTTP'
            ],

    getBrowser : function() {
        var ua = navigator.userAgent.toLowerCase();
        if (ua.indexOf('opera') != -1) { // Opera (check first in case of spoof)
            return 'opera';
        } else if (ua.indexOf('msie 7') != -1) { // IE7
            return 'ie7';
        } else if (ua.indexOf('msie') != -1) { // IE
            return 'ie';
        } else if (ua.indexOf('safari') !=
                   -1) { // Safari (check before Gecko because it includes "like Gecko")
            return 'safari';
        } else if (ua.indexOf('gecko') != -1) { // Gecko
            return 'gecko';
        } else {
            return false;
        }
    },
    createXMLHttpRequest : function() {
        var xhrObject;

        try {
            xhrObject = new XMLHttpRequest();
        } catch(e) {
            for (var i = 0; i < this._msxml.length; ++i) {
                try
                {
                    // Instantiates XMLHttpRequest for IE and assign to http.
                    xhrObject = new ActiveXObject(this._msxml[i]);
                    break;
                }
                catch(e) {
                    // do nothing
                }
            }
        } finally {
            return xhrObject;
        }
    },

    isIESupported : function() {
        var browser = this.getBrowser();
        if (this.isIEXMLSupported() && (browser == "ie" || browser == "ie7")) {
            return true;
        }

        return false;

    },

    isIEXMLSupported: function() {
        if (!window.ActiveXObject) {
            return false;
        }
        try {
            new ActiveXObject("Microsoft.XMLDOM");
            return true;

        } catch(e) {
            return false;
        }
    },

/*
This function will be used as an xml to html
transformation helper in callback objects. Works only with wso2.wsf.WSRequest.
@param xml : XML document
@param xsltFile : XSLT file
@param objDiv  : Div that trasformation should be applied
@param doNotLoadDiv : flag that store the div in browser history
@param isAbsPath : If xsltFile is absolute, then isAbsPath should be true
*/
    callbackhelper : function(xml, xsltFile, objDiv, doNotLoadDiv, isAbsPath) {
        this.processXML(xml, xsltFile, objDiv, isAbsPath);
        if (!doNotLoadDiv) {
            this.showOnlyOneMain(objDiv);
        }

    },

/*
@parm xml : DOM document that needed to be transformed
@param xslFileName : XSLT file name. This could be foo.xsl, which is reside in /extensions/core/js
                     or bar/car/foo.xsl. If the later version is used, the isAbstPath should be true.
@param objDiv : Div object, the transformed fragment will be append to it.
@param isAbsPath : Used to indicate whether the usr provided is a absolute path.

*/
    processXML : function (xml, xslFileName, objDiv, isAbsPath) {
        var xsltHelperObj = new wso2.wsf.XSLTHelper();
        xsltHelperObj.transform(objDiv, xml, xslFileName, isAbsPath);
    },

/*
Login method
*/
    login :function(userName, password, callbackFunction) {

        if (typeof(callbackFunction) != "function") {
            this.alertWarning("Login can not be continued due to technical errors.");
            return;
        }

        var bodyXML = ' <ns1:login  xmlns:ns1="http://org.apache.axis2/xsd">\n' +
                      ' <arg0>' + userName + '</arg0>\n' +
                      ' <arg1>' + password + '</arg1>\n' +
                      ' </ns1:login>\n';
        var callURL = serverURL + "/" + GLOBAL_SERVICE_STRING + "/" + "login";

        new wso2.wsf.WSRequest(callURL, "urn:login", bodyXML, callbackFunction);

    },
/*
Logout method
*/
    logout : function(callbackFunction) {
        // stopping all refressing methods
        stoppingRefreshingMethodsHook();
        historyStorage.reset();
        var bodyXML = ' <ns1:logout  xmlns:ns1="http://org.apache.axis2/xsd"/>\n';

        var callURL = serverURL + "/" + GLOBAL_SERVICE_STRING + "/" + "logout";
        new wso2.wsf.WSRequest(callURL, "urn:logout", bodyXML, callbackFunction);
    },
/*
This method will store the given the div in the browser history
@param objDiv : Div that needed to be stored.
@param isReloadDiv : div is restored.
*/
    showOnlyOneMain : function(objDiv, isReloadDiv) {
        if (objDiv == null)
            return;

        var par = objDiv.parentNode;

        var len = par.childNodes.length;
        var count;
        for (count = 0; count < len; count++) {
            if (par.childNodes[count].nodeName == "DIV") {
                par.childNodes[count].style.display = 'none';
            }
        }
        objDiv.style.display = 'inline';
        var output = objDiv.attributes;
        var attLen = output.length;
        var c;
        var divNameStr;
        for (c = 0; c < attLen; c++) {
            if (output[c].name == 'id') {
                divNameStr = output[c].value;
            }
        }
        //alert(divNameStr);
        this.setDivTabsToMinus(objDiv);
        this._storeDiv(divNameStr, isReloadDiv)
    },

    _storeDiv : function(divName, isReloadDiv) {
        if (lastHash != "___" + divName) {
            if (!isReloadDiv) {
                lastHash = "___" + divName;
                // alert("Storing div " + lastHash);
                if (mainMenuObjectId != null && mainMenuObjectIndex != -1) {
                    dhtmlHistory.add(lastHash,
                    {menuObj:mainMenuObjectId + ':' + mainMenuObjectIndex});

                } else {
                    dhtmlHistory.add(lastHash, true);
                }
            }
        }
    },

/*
This will set all the tabindexes in all the child divs to -1.
This way no div will get focus  when some one is tabbing around.
@parm objDiv : parent div
*/
    setDivTabsToMinus : function (objDiv) {
        var divs = objDiv.getElementsByTagName("div");
        for (var index = 0; index < divs.length; index++) {
            divs[index].setAttribute("tabindex", "-1");
        }
    },

/*
 Set a cookie.
 @param name : Cookie name
 @param value : Cookie value
 @param expires : Date of expire
 @param secure: If the given cookie should be secure.

*/
    setCookie : function(name, value, expires, secure) {
        document.cookie = name + "=" + escape(value) +
                          ((expires) ? "; expires=" + expires.toGMTString() : "") +
                          ((secure) ? "; secure" : "");
    },

/*
Get Cookie value.
@param name : Cookie name
*/
    getCookie : function (name) {
        var dc = document.cookie;
        var prefix = name + "=";
        var begin = dc.indexOf("; " + prefix);
        if (begin == -1) {
            begin = dc.indexOf(prefix);
            if (begin != 0) return null;
        } else {
            begin += 2;
        }
        var end = document.cookie.indexOf(";", begin);
        if (end == -1) {
            end = dc.length;
        }
        return unescape(dc.substring(begin + prefix.length, end));
    },

/*
Delete a Cookie.
@param name : Cookie name
*/
    deleteCookie : function(name) {
        document.cookie = name + "=" + "; EXPIRES=Thu, 01-Jan-70 00:00:01 GMT";

    },
/*
Given DOM document will be serialized into a String.
@param paylod : DOM payload.
*/
    xmlSerializerToString : function (payload) {
        var browser = this.getBrowser();

        switch (browser) {
            case "gecko":
                var serializer = new XMLSerializer();
                return serializer.serializeToString(payload);
                break;
            case "ie":
                return payload.xml;
                break;
            case "ie7":
                return payload.xml;
                break;
            case "opera":
                var xmlSerializer = document.implementation.createLSSerializer();
                return xmlSerializer.writeToString(payload);
                break;
            case "safari":
            // use the safari method
                throw new Error("Not implemented");
            case "undefined":
                throw new Error("XMLHttp object could not be created");
        }
    },

/*
Check if the give the brower is IE
*/
    isIE : function() {
        return this.isIESupported();
    },

/*
   This method will restart the server.
*/
    restartServer : function (callbackFunction) {
        var msgStat = confirm("Do you want to restart the server?");
        if(!msgStat){
            return;
        }

        var bodyXML = '<req:restartRequest xmlns:req="http://org.apache.axis2/xsd"/>\n';

        var callURL = serverURL + "/" + ADMIN_SERVER_URL ;
        if (callbackFunction && (typeof(callbackFunction) == "function")) {
            new wso2.wsf.WSRequest(callURL, "urn:restart", bodyXML, callbackFunction);
        } else {
            new wso2.wsf.WSRequest(callURL, "urn:restart", bodyXML, wso2.wsf.Util.restartServer["callback"]);
        }
    },

/*
   This method will restart the server gracefully.
*/
    restartServerGracefully : function (callbackFunction) {
        var msgStat = confirm("Do you want to gracefully restart the server?");
        if(!msgStat){
            return;
        }
        var bodyXML = '<req:restartGracefullyRequest xmlns:req="http://org.apache.axis2/xsd"/>\n';

        var callURL = serverURL + "/" + ADMIN_SERVER_URL ;
        if (callbackFunction && (typeof(callbackFunction) == "function")) {
            new wso2.wsf.WSRequest(callURL, "urn:restartGracefully", bodyXML, callbackFunction);
        } else {
            new wso2.wsf.WSRequest(callURL, "urn:restartGracefully", bodyXML, wso2.wsf.Util.restartServerGracefully["callback"]);
        }
    },

/*
   This method will shutdown the server gracefully.
*/
    shutdownServerGracefully : function (callbackFunction) {
        var msgStat = confirm("Do you want to gracefully shutdown the server?");
        if(!msgStat){
            return;
        }
        var bodyXML = '<req:shutdownGracefullyRequest xmlns:req="http://org.apache.axis2/xsd"/>\n';

        var callURL = serverURL + "/" + ADMIN_SERVER_URL ;
        if (callbackFunction && (typeof(callbackFunction) == "function")) {
            new wso2.wsf.WSRequest(callURL, "urn:shutdownGracefully", bodyXML, callbackFunction);
        } else {
            new wso2.wsf.WSRequest(callURL, "urn:shutdownGracefully", bodyXML, wso2.wsf.Util.shutdownServerGracefully["callback"]);
        }
    },

/*
   This method will shutdown the server immediately.
*/
    shutdownServer : function (callbackFunction) {
        var msgStat = confirm("Do you want to shutdown the server?");
        if(!msgStat){
            return;
        }
        var bodyXML = '<req:shutdownRequest xmlns:req="http://org.apache.axis2/xsd"/>\n';

        var callURL = serverURL + "/" + ADMIN_SERVER_URL ;
        if (callbackFunction && (typeof(callbackFunction) == "function")) {
            new wso2.wsf.WSRequest(callURL, "urn:shutdown", bodyXML, callbackFunction);
        } else {
            new wso2.wsf.WSRequest(callURL, "urn:shutdown", bodyXML, wso2.wsf.Util.shutdownServer["callback"]);
        }
    },

/*
Trim the give string
*/
    trim: function (strToTrim) {
        return(strToTrim.replace(/^\s+|\s+$/g, ''));
    },

/*
Busy cursor
*/
    cursorWait : function () {
        document.body.style.cursor = 'wait';
    },

/*
Normal cursor
*/
    cursorClear : function() {
        document.body.style.cursor = 'default';
    },

/*
Open a new window and show the results
*/
    openWindow : function(value) {
        // This will return a String of foo/bar/ OR foo/bar/Foo
        window.open(serviceURL + '/' + value);
    },

/*
Propmpt a prompt box
*/
    getUserInput : function() {
        return this.getUserInputCustum("Please enter the parameter name", "Please enter the parameter value for ", true);
    },

/*
Will use the promt provided by the user prompting for parameters. If the
useParamNameInPrompt is true then the param value prompt will be appended
the paramName to the back of the paramValuePrompt value.
*/

    getUserInputCustum : function (paramNamePrompt, paramValuePrompt, useParamNameInPrompt) {
        var returnArray = new Array();
        var tempValue = window.prompt(paramNamePrompt);
        if (tempValue == '' || tempValue == null) {
            return null;
        }
        returnArray[0] = tempValue;
        if (useParamNameInPrompt) {
            tempValue = window.prompt(paramValuePrompt + returnArray[0]);
        } else {
            tempValue = window.prompt(paramValuePrompt);
        }
        if (tempValue == '' || tempValue == null) {
            return null;
        }
        returnArray[1] = tempValue;
        return returnArray;
    },

/*
Show Response
*/
    showResponseMessage : function (response) {
        var returnStore = response.getElementsByTagName("return")[0];
        this.alertMessage(returnStore.firstChild.nodeValue);
    },

/*shows the a custom alert box public*/
    alertInternal : function (message, style) {

        var messageBox = document.getElementById('alertMessageBox');
        var messageBoxTextArea = document.getElementById('alertMessageBoxMessageArea');
        // var messageBoxImage =
		// document.getElementById('alertMessageBoxImg');alertMessageBox
        // set the left and top positions

        var theWidth;
        if (window.innerWidth)
        {
            theWidth = window.innerWidth
        }
        else if (document.documentElement && document.documentElement.clientWidth)
        {
            theWidth = document.documentElement.clientWidth
        }
        else if (document.body)
        {
            theWidth = document.body.clientWidth
        }

        var theHeight;
        if (window.innerHeight)
        {
            theHeight = window.innerHeight
        }
        else if (document.documentElement && document.documentElement.clientHeight)
        {
            theHeight = document.documentElement.clientHeight
        }
        else if (document.body)
        {
            theHeight = document.body.clientHeight
        }

        var leftPosition = theWidth / 2 - messageBoxWidth / 2 ;
        var topPosition = theHeight / 2 - messageBoxHeight / 2;
        var bkgr;
        messageBox.style.left = leftPosition + 'px';
        messageBox.style.top = topPosition + 'px';
        // set the width and height
        messageBox.style.width = messageBoxWidth + 'px';
        // messageBox.style.height = messageBoxHeight+ 'px';

        // set the pictures depending on the style
        if (style == WARNING_MESSAGE) {
            bkgr =
            "url(" + warningMessageImage + ") " + warningnMessagebackColor + " no-repeat 15px 17px";
        } else if (style == INFORMATION_MESSAGE) {
            bkgr = "url(" + informationMessageImage + ") " + informationMessagebackColor +
                   " no-repeat 15px 17px";
        }
        messageBox.style.background = bkgr;
        // set the message
        messageBoxTextArea.innerHTML = message;
        messageBox.style.display = 'inline';
        document.getElementById('alertBoxButton').focus();
        return false;
    },

/*
Convenience methods that call the alertInternal
show a information message
*/
    alertMessage : function (message) {
        this.alertInternal(message, INFORMATION_MESSAGE);
    },

/*
Show a warning message
*/
    alertWarning : function (message) {
        var indexOfExceptionMsg = message.indexOf('; nested exception is: ');
        if (indexOfExceptionMsg != -1) {
            message = message.substring(0, indexOfExceptionMsg);
        }
        this.alertInternal(message, WARNING_MESSAGE);
    },

/*
Find the host and assingend it to HOST
*/
    initURLs : function() {
        var locationHref = self.location.href;

        var tmp1 = locationHref.indexOf("://");
	var tmp2 = locationHref.substring(tmp1 + 3, locationHref.indexOf("?"));
        var tmp3 = tmp2.indexOf(":");
        if (tmp3 > -1) {
            HOST = tmp2.substring(0, tmp3);
        } else {
            tmp3 = tmp2.indexOf("/");
            HOST = tmp2.substring(0, tmp3);
        }

        URL = "https://" + HOST +
              (HTTPS_PORT != 443 ? (":" + HTTPS_PORT + ROOT_CONTEXT)  : ROOT_CONTEXT);
        GURL = "http://" + HOST +
              (HTTP_PORT != 80 ? (":" + HTTP_PORT + ROOT_CONTEXT)  : ROOT_CONTEXT);

        HTTP_URL = "http://" + HOST +
                   (HTTP_PORT != 80 ? (":" + HTTP_PORT + ROOT_CONTEXT)  : ROOT_CONTEXT) +
                   "/" + SERVICE_PATH;
        serverURL = "https://" + HOST +
                    (HTTPS_PORT != 443 ? (":" + HTTPS_PORT + ROOT_CONTEXT)  : ROOT_CONTEXT) +
                    "/" + SERVICE_PATH;

    },

    getProtocol : function() {
        var _tmpURL = locationString.substring(0, locationString.lastIndexOf('/'));
        if (_tmpURL.indexOf('https') > -1) {
            return 'https';
        } else if (_tmpURL.indexOf('http') > -1) {
            return 'http';
        } else {
            return null;
        }
    },

    getServerURL : function() {
        var _tmpURL = locationString.substring(0, locationString.lastIndexOf('/'));
        if (_tmpURL.indexOf('https') == -1) {
            return HTTP_URL;
        }
        return serverURL;
    },

    getBackendServerURL : function(frontendURL, backendURL) {
        if (backendURL.indexOf("localhost") >= 0 || backendURL.indexOf("127.0.0.1") >= 0) {
            return frontendURL;
        } else {
            return backendURL;
        }
    }
};


/*
 * XSLT helper will be used to communicate with a server and aquire XSLT
 * resource. The communication will be sync. This will quire the resource with
 * reference to the brower it will be injected.
 * 
 * XSLT helper caches the loaded XSLT documents. In order to initiate, Used has
 * to first call the, wso2.wsf.XSLTHelper.init() method in window.onLoad.
 * 
 */
wso2.wsf.XSLTHelper = function() {
    this.req = null;
}
/*
 xslName is add to the array
*/
wso2.wsf.XSLTHelper.xsltCache = null;

wso2.wsf.XSLTHelper.init = function() {
    wso2.wsf.XSLTHelper.xsltCache = new Array();
}

wso2.wsf.XSLTHelper.add = function(xslName, xslObj) {
    wso2.wsf.XSLTHelper.xsltCache[xslName] = xslObj;
}
wso2.wsf.XSLTHelper.get = function(xslName) {
    return wso2.wsf.XSLTHelper.xsltCache[xslName];
}

wso2.wsf.XSLTHelper.prototype = {
    load : function(url, fileName, params) {
        try {
            if (window.XMLHttpRequest && window.XSLTProcessor) {
                this.req = new XMLHttpRequest();
                this.req.open("GET", url, false);
                // Sync call
                this.req.send(null);
                var httpStatus = this.req.status;
                if (httpStatus == 200) {
                    wso2.wsf.XSLTHelper.add(fileName, this.req.responseXML);
                } else {
                    this.defaultError.call(this);
                }

            } else if (window.ActiveXObject) {
                try {
                    this.req = new ActiveXObject("Microsoft.XMLDOM");
                    this.req.async = false;
                    this.req.load(url);
                    wso2.wsf.XSLTHelper.add(fileName, this.req);
                } catch(e) {
                    wso2.wsf.Util.alertWarning("Encounterd an error  : " + e);
                }
            }

        } catch(e) {
            this.defaultError.call(this);
        }

    },

    defaultError : function() {
        CARBON.showWarningDialog("Error Fetching XSLT file.")
    },

    transformMozilla : function(container, xmlDoc, fileName, isAbsPath, xslExtension, params) {
        var xslStyleSheet = wso2.wsf.XSLTHelper.get(fileName);
        if (xslStyleSheet == undefined) {
            var url = this.calculateURL(fileName, isAbsPath, xslExtension);
            this.load(url, fileName, params);
        }
        xslStyleSheet = wso2.wsf.XSLTHelper.get(fileName);
        if (xslStyleSheet == undefined || xslStyleSheet == null) {
            wso2.wsf.Util.alertWarning("XSL Style Sheet is not available");
            return;
        }

        try {
            var xsltProcessor = new XSLTProcessor();

            if (params) {
                var len = params.length;
                for (var i = 0; i < len; i++) {
                    xsltProcessor.setParameter(null, params[i][0], params[i][1]);
                }

            }
            xsltProcessor.importStylesheet(xslStyleSheet);
            var fragment = xsltProcessor.transformToFragment(xmlDoc, document);

            container.innerHTML = "";
            container.appendChild(fragment);
        } catch(e) {
          //  wso2.wsf.Util.alertWarning("Encounterd an error  : " + e.toString());
        }
    },

    transformIE : function(container, xmlDoc, fileName, isAbsPath, xslExtension, params) {
        try {
            if (params) {
                var url = this.calculateURL(fileName, isAbsPath, xslExtension);
                // declare the local variables
                var xslDoc, docProcessor, docCache, docFragment;
                // instantiate and load the xsl document
                xslDoc = new ActiveXObject("MSXML2.FreeThreadedDOMDocument");
                xslDoc.async = false;
                xslDoc.load(url);

                // prepare the xsl document for transformation
                docCache = new ActiveXObject("MSXML2.XSLTemplate");
                docCache.stylesheet = xslDoc;
                // instantiate the document processor and submit the xml
				// document
                docProcessor = docCache.createProcessor();
                docProcessor.input = xmlDoc;
                // add parameters to the xsl document
                var len = params.length;
                for (var i = 0; i < len; i++) {
                    docProcessor.addParameter(params[i][0], params[i][1], "");
                }
                // process the documents into html and submit to the passed div to the HMTL page
                docProcessor.transform();
                // divID.innerHTML = docProcessor.output;
                container.innerHTML = "<div>" + docProcessor.output + "</div>";

            } else {
                var xslStyleSheet = wso2.wsf.XSLTHelper.get(fileName);
                if (xslStyleSheet == undefined) {
                    var url = this.calculateURL(fileName, isAbsPath, xslExtension);
                    this.load(url, fileName);
                }
                xslStyleSheet = wso2.wsf.XSLTHelper.get(fileName);
                if (xslStyleSheet == undefined || xslStyleSheet == null) {
                    wso2.wsf.Util.alertWarning("XSL Style Sheet is not available");
                    return;
                }
                var fragment = xmlDoc.transformNode(xslStyleSheet);
                container.innerHTML = "<div>" + fragment + "</div>";
            }
        } catch(e) {
            wso2.wsf.Util.alertWarning("Encounterd an error  : " + e.toString());
        }

    },

    calculateURL : function (fileName, isAbsPath, xslExtension) {
        var fullPath;

        if (!xslExtension) {
            xslExtension = 'core';
        }

        if (isAbsPath) {
            fullPath = fileName;
            return fullPath;
        }

        //        fullPath = URL + "/extensions/" + xslExtension + "/xslt/" + fileName;
        /* Using the relative paths to obtain XSLT */
        fullPath = "extensions/" + xslExtension + "/xslt/" + fileName;

        return fullPath;

    },

/**
 * @param container : DIV object. After transformation generated HTML will be injected to this location.
 * @param xmlDoc    : XML DOM Document.
 * @param fileName  : XSL file name. Make sure this being unique
 * @param isAbsPath : Used to indicate whether the usr provided is a absolute path. This is needed to reuse this
 method from outside the admin service.
 * @param xslExtension : Extension location
 * @param params : An array containing params that needed to be injected when doing transformation.
 ex: var param = new Array(["fooKey","fooValue"]);
 thus, "fooKey" will be used for find the parameter name and fooValue will be set
 as the parameter value.
 */
    transform : function(container, xmlDoc, fileName, isAbsPath, xslExtension, params) {
        if (!this.isXSLTSupported()) {
            wso2.wsf.Util.alertWarning("This browser does not support XSLT");
            return;
        }

        if (window.XMLHttpRequest && window.XSLTProcessor) {
            this.transformMozilla(container, xmlDoc, fileName, isAbsPath, xslExtension, params);

        } else if (window.ActiveXObject) {
            this.transformIE(container, xmlDoc, fileName, isAbsPath, xslExtension, params);
        }


    },
    isXSLTSupported : function() {
        return (window.XMLHttpRequest && window.XSLTProcessor) || wso2.wsf.Util.isIEXMLSupported();

    }

};


// /////////////////////////////////////////////////////////////////////////////////////////////////
// /////////////////////////////////////////////////////////////////////////////////////////////////
/*
 * All the inline function found after this point onwards are titly bound with
 * the index.html template and users are not encourage to use them. If users
 * want to use them, they should do it with their own risk.
 */


/* public */
function finishLogin() {
    //new one;
    userNameString = "<nobr>Signed in as <strong>" + userName +
                     "</strong>&nbsp;&nbsp;|&nbsp;&nbsp;<a href='about.html' target='_blank'>About</a>&nbsp;&nbsp;|&nbsp;&nbsp;<a href='docs/index_docs.html' target='_blank'>Docs</a>&nbsp;&nbsp;|&nbsp;&nbsp;<a id='logOutA' href='#' onclick='javascript:wso2.wsf.Util.logout(wso2.wsf.Util.logout[\"callback\"]); return false;'>Sign Out</a></nobr>";
    document.getElementById("meta").innerHTML = userNameString;
    document.getElementById("navigation_general").style.display = "none";
    document.getElementById("navigation_logged_in").style.display = "inline";
    document.getElementById("content").style.display = "inline";
    updateRegisterLink();
}

/*private*/
function updateRegisterLink() {


    var bodyXML = ' <ns1:isServerRegistered xmlns:ns1="http://org.apache.axis2/xsd"/>';
    var callURL = serverURL + "/" + GLOBAL_SERVICE_STRING ;
    new wso2.wsf.WSRequest(callURL, "urn:isServerRegistered", bodyXML, updateRegisterLink["callback"]);
}

updateRegisterLink["callback"] = function() {
    if (this.req.responseXML.getElementsByTagName("return")[0].firstChild.nodeValue != "true") {
        document.getElementById("meta").innerHTML +=
        "&nbsp;&nbsp;|&nbsp;&nbsp;<a href='#' onclick='javascript:registerProduct(); return false;'>Register</a>";
    }
    runPoleHash = true;
// initialize();
    showHomeMenu();

}

/*private*/
function loginFail() {
    wso2.wsf.Util.alertWarning("Login failed. Please recheck the user name and password and try again.");
}

/*public*/
function registerProduct() {
    var bodyXML = ' <ns1:getServerData xmlns:ns1="http://org.apache.axis2/xsd"/>';

    var callURL = serverURL + "/" + SERVER_ADMIN_STRING ;
    new wso2.wsf.WSRequest(callURL, "urn:getServerData", bodyXML, registerProductCallback);
}


wso2.wsf.Util.login["callback"] = function() {
    var isLogInDone = this.req.responseXML.getElementsByTagName("return")[0].firstChild.nodeValue;
    if (isLogInDone != "true") {
        loginFail();
        return;
    }
    userName = document.formLogin.txtUserName.value;
    if (userName) {
        wso2.wsf.Util.setCookie("userName", userName);
    }
    finishLogin();
}


/*private*/
wso2.wsf.Util.logout["callback"] = function() {
    runPoleHash = false;
    logoutVisual();

}

wso2.wsf.Util.restartServer["callback"] = function() {
    logoutVisual();
    stopWaitAnimation();
    wso2.wsf.Util.alertMessage("The server is being restarted. <br/> This will take a few seconds. ");
    // stopping all refressing methods
// stoppingRefreshingMethodsHook();

}

wso2.wsf.Util.restartServerGracefully["callback"] = function() {
    logoutVisual();
    stopWaitAnimation();
    wso2.wsf.Util.alertMessage("The server is being gracefully restarted. <br/> This will take a few seconds. ");
    // stopping all refressing methods
// stoppingRefreshingMethodsHook();

}

wso2.wsf.Util.shutdownServerGracefully["callback"] = function() {
    logoutVisual();
    stopWaitAnimation();
    wso2.wsf.Util.alertMessage("The server is being gracefully shutdown. <br/> This will take a few seconds. ");
    // stopping all refressing methods
// stoppingRefreshingMethodsHook();

}

wso2.wsf.Util.shutdownServer["callback"] = function() {
    logoutVisual();
    stopWaitAnimation();
    wso2.wsf.Util.alertMessage("The server is being shutdown.");
    // stopping all refressing methods
// stoppingRefreshingMethodsHook();

}

/*private*/
function logoutVisual() {
    serviceGroupId = "";
    // deleteCookie("serviceGroupId");
    // deleteCookie("userName");

    wso2.wsf.Util.deleteCookie("JSESSIONID");

// document.formLogin.txtUserName.value = "";
// document.formLogin.txtPassword.value = "";
    // document.getElementById("container").style.display = "none";
    // document.getElementById("userGreeting").style.display = "none";
// document.getElementById("navigation_general").style.display = "inline";
// document.getElementById("navigation_logged_in").style.display = "none";
// document.getElementById("meta").innerHTML = "<nobr><a href='about.html'
// target='_blank'>About</a>&nbsp;&nbsp;|&nbsp;&nbsp;<a
// href='docs/index_docs.html'
// target='_blank'>Docs</a>&nbsp;&nbsp;|&nbsp;&nbsp;<a id='logInA' href='#'
// onclick='javascript:wsasLogin(); return false;'>Sign In</a></nobr>";
    if (typeof(showGeneralHome) != "undefined" && typeof(showGeneralHome) == "function") {
        CARBON.showInfoDialog("logoutVisual");
        showLoginPage();
        historyStorage.reset();
    }
}


var waitAnimationInterval;
var waitCount = 0;
/* private */
function executeWaitAnimation() {
    waitAnimationInterval = setInterval(function() {
        updateWaitAnimation();
    }, 200);

}
/*private*/
function stopWaitAnimation() {
    clearInterval(waitAnimationInterval);
    waitCount = 4;
    // document.getElementById("waitAnimationDiv").style.display = "none";
    var divObj = document.getElementById("waitAnimationDiv");
    if (divObj) {
        divObj.style.background = "url(images/orange_circles.gif) transparent no-repeat left top;";
        divObj.style.padding = "0;";
    }
}

/*private*/
function startWaitAnimation() {
    var divToUpdate = document.getElementById("waitAnimationDiv");
    // alert("startWaitAnimation" + divToUpdate);
    if (divToUpdate != null) {
        divToUpdate.style.display = "inline";
        waitAnimationTimeout();
    }
}

/*private */
function updateWaitAnimation() {
    var divToUpdate = document.getElementById("waitAnimationDiv");
    if (divToUpdate != null) {
        if (waitCount == 8) {
            waitCount = 1;
        } else {
            waitCount++;
        }
        divToUpdate.style.background =
        "url(images/waiting_ani_" + waitCount + ".gif) transparent no-repeat left top;";
        document.getElementById("waitAnimationDiv").style.padding = "0;";
    }
}
/* History tracking code
   Underline project has to implement handleHistoryChange function.
*/
/* private */
function initialize() {
    // initialize our DHTML history
    dhtmlHistory.initialize();
    historyStorage.reset();
    // subscribe to DHTML history change
    // events
    dhtmlHistory.addListener(
            handleHistoryChange);
}

/*public*/
function openExtraWindow(firstValue, lastValue) {
    window.open(firstValue + serviceURL + "/" + lastValue);
}

/*
	All functions of this nature will return the first value it finds. So do now use when you know that
	there can be more than one item that match (elementName + attName + attValue).
*/
/* public */
function getElementWithAttribute(elementName, attName, attValue, parentObj) {
    var objList = parentObj.getElementsByTagName(elementName);
    if (objList.length > 0) {
        for (var d = 0; d < objList.length; d++) {
            if (attValue == getAttbute(attName, objList[d])) {
                return objList[d];
            }
        }
    } else {
        return null;
    }
}
/*
 * Will return the attribute values of the named attribute from the
 * object that is passed in.
 */
/* public */
function getAttbute(attrName, objRef) {
    var attObj = getAttbuteObject(attrName, objRef);
    if (attObj != null) {
        return attObj.value;
    } else {
        return null;
    }
}

/*
 * Will return the attribute object of the named attribute from the
 * object[objRef] that is passed in.
 */
/* publc */
function getAttbuteObject(attrName, objRef) {
    var output = objRef.attributes;

    if (output == null) return null;
    var attLen = output.length;
    var c;
    var divNameStr;
    for (c = 0; c < attLen; c++) {
        if (output[c].name == attrName) {
            return output[c];
        }
    }
}

/*
 * Will return a string with all the attributes in a name="value" format
 * seperated with a space.
 */
/* public */
function getAttributeText(node) {
    var text_attributes = "";
    var output = node.attributes;
    if (output == null) return "";
    var attLen = output.length;
    var c;
    var divNameStr;
    for (c = 0; c < attLen; c++) {
        // Skiping the special attribute set by us.
        if (output[c].name != "truedomnodename") {
            text_attributes += " " + output[c].name + '="' + output[c].value + '"';
        }
    }
    return text_attributes;
}

/*
 * Will print out the DOM node that is passed into the method.
 * It will also add tabs.
 * If convertToLower is true all tagnames will be converted to lower case.
 */
/* public */
function prettyPrintDOMNode(domNode, nonFirst, tabToUse, convertToLower) {
    if (!nonFirst) {
        tabcount = 0;
        if (tabToUse == null) {
            tabCharactors = "\t";
        } else {
            tabCharactors = tabToUse;
        }
    }
    if (domNode == null) {
        return "";
    }
    var dom_text = "";
    var dom_node_value = "";
    var len = domNode.childNodes.length;
    if (len > 0) {
        if (domNode.nodeName != "#document") {
            if (nonFirst) {
                dom_text += "\n";
            }
            dom_text += getCurTabs();
            dom_text +=
            "<" + getTrueDOMNodeNameFromNode(domNode, convertToLower) + getAttributeText(domNode) +
            ">";
            tabcount++;
        }
        for (var i = 0; i < len; i++) {
            if (i == 0) {
                dom_text += prettyPrintDOMNode(domNode.childNodes[i], true, "", convertToLower);
            } else {
                dom_text += prettyPrintDOMNode(domNode.childNodes[i], true, "", convertToLower);
            }
        }
        if (domNode.nodeName != "#document") {
            tabcount--;
            if (!(domNode.childNodes.length == 1 && domNode.childNodes[0].nodeName == "#text")) {
                dom_text += "\n" + getCurTabs();
            }
            dom_text += "</" + getTrueDOMNodeNameFromNode(domNode, convertToLower) + ">";
        }

    } else {
        if (domNode.nodeName == "#text") {
            dom_text += domNode.nodeValue;
        }else if (domNode.nodeName == "#comment") {
            dom_text += "\n" + getCurTabs() + "<!--" + domNode.nodeValue + "-->";
        }else {
            dom_text += "\n" +
                        getCurTabs() + "<" + getTrueDOMNodeNameFromNode(domNode, convertToLower) +
                        getAttributeText(domNode) +
                        "/>";
        }
    }
    return dom_text;
}
// This will serialize the first node only.
/* public */
function nodeStartToText(domNode) {
    if (domNode == null) {
        return "";
    }
    var dom_text = "";
    var len = domNode.childNodes.length;
    if (len > 0) {
        if (domNode.nodeName != "#document") {
            dom_text +=
            "<" + getTrueDOMNodeNameFromNode(domNode) + getAttributeText(domNode) + ">\n";
        }
    } else {
        if (domNode.nodeName == "#text") {
            dom_text += domNode.nodeValue;
        } else {
            dom_text +=
            "<" + getTrueDOMNodeNameFromNode(domNode) + getAttributeText(domNode) + "/>\n";
        }
    }
    return dom_text;
}

/*
 * When creating a new node using document.createElement the new node that
 * is created will have a all capital value when you get the nodeName
 * so to get the correct serialization we set a new attribute named "trueDOMNodeName" on the
 * new elements that are created. This method will check whether there is an attribute set
 * and will return the nodeName accordingly.
 * If convertToLower is true then the node name will be converted into lower case and returned.
 */
/* public */
function getTrueDOMNodeNameFromNode(objNode, convertToLower) {
    var trueNodeName = getAttbute("truedomnodename", objNode);
    if (trueNodeName == null) {
        trueNodeName = objNode.nodeName;
    }
    if (convertToLower) {
        return trueNodeName.toLowerCase();
    } else {
        return trueNodeName;
    }
}

/*
 * Will return the number of tabs to print for the current node being passed.
 */
/* public */
function getCurTabs() {
    var tabs_text = "";
    for (var a = 0; a < tabcount; a++) {
        tabs_text += tabCharactors;
    }
    return tabs_text;
}

/*
 * Use to get a node from within an object hierarchy where there are objects
 * with the same name at different levels.
 */
/* public */
function getNodeFromPath(pathString, domParent) {
    var items = pathString.split("/");
    var restOfThem = "";
    var lastStep = (items.length == 1);

    if (!lastStep) {
        for (var r = 1; r < items.length; r++) {
            restOfThem += items[r] + "/";
        }
        restOfThem = restOfThem.substring(0, restOfThem.length - 1);
    }
    var temp = domParent.getElementsByTagName(items[0]);
    if (temp == null) {
        return null;
    }
    if (temp.length < 1) {
        return null;
    }
    for (var u = 0; u < temp.length; u++) {
        var retEle;
        if (!lastStep) {
            retEle = getNodeFromPath(restOfThem, temp[u]);
        } else {
            retEle = temp[u];
        }
        if (retEle != null) {
            return retEle;
        }
    }
    return null;
}

/*
 * Changes the location of the window to service listing page.
 * This function can used when a seperate upload servlet takes control
 * and we do not have any control over the return location. 
 * See org.wso2.carbon.ui.transport.fileupload.ServiceFileUploadExecutor ->execute()
 * for a usage scenario.
 */
/* public */
function loadServiceListingPage() {
    window.location ='../service-mgt/service_mgt.jsp';
}


function showHelp() {
    var myWindow = window.open("userguide.html", "tinyWindow", 'scrollbars=yes,menubar=no,height=600,width=600,resizable=yes,toolbar=no,location=no,status=no')
    myWindow.focus()
}


function showForgotPassword(serverName, home){

	 var tableHTML = '<div tabindex="-1">' +
                            '<h4><a onclick="javascript:showSignIn(\'' + serverName + '\', \'' + home + '\'); return false;" href="#">Sign In</a>&nbsp;&gt;&nbsp;Forgot Password</h4><h2>Forgot WSO2 Data Services Management Console password</h2>' +
                             ' <dl style="margin: 1em;"><dt><strong>Non Admin User</strong></dt>' +
                                '<dd>Please contact the system Admin. The system administrator can reset the password of any non admin account.<br><br></dd>';
// '<dt><strong>Admin User</strong></dt><dd>Due to security concerns, you cannot
// retrieve your password using this Admin Console. You may change your password
// by running the <b>chpasswd</b>' +
// ' script on the machine which is hosting the ' + serverName + '.<br>' +
// 'This script is located at <i>' + home + '/bin</i><br><b>IMPORTANT:</b>
// Before executing this script, you should shutdown the WSO2 Server.
// <br></dd></dl></div>' ;

      document.getElementById("middle").innerHTML = tableHTML;

}
function showSignInHelp(serverName, home){

        var tableHTML = '<div tabindex="-1" style="display: inline;" id="noMenuContainer"><h4><a onclick="javascript:showSignIn(\'' + serverName + '\', \'' + home + '\'); return false;" href="#">Sign In</a>&nbsp;&gt;&nbsp;Sign In Help</h4>' +
                        '<h2>Help on Signing In</h2>' +
                        '<p>Following is a list of issues that you may face when Signing In and the reasons and solutions to them. </p>' +
                        '<ol style="margin: 1em;"><li><b>ERROR :: Could not connect to the server. Please try again in a moment</b>' +
                        '<p>You can get this message when the ' + serverName + ' server is down or when it can not be reached on the network.</p></li>' +
                        '<li><b>ERROR :: Login failed. Please recheck the user name and password and try again</b>' +
                        '<p>You can get this error even when you have spelt the user name and password correctly, because both user name and password are case sensitive, or due to the page being in an' +
                        'inconsistent state.Check whether the caps lock is on and whether you have spelt the user name and the password correctly. If the correct user name and the password is still failing then refresh the page and try again.' +
                        '</p></li><li><b>Forgot Password</b><p>Have a look at the <a onclick="javascript:showForgotPassword(\'' + serverName + '\', \'' + home + '\'); return false;" href="#">Forgot Password</a> page.</p></li></ol></div>';

         document.getElementById("middle").innerHTML = tableHTML;
      
}

function showSignIn(serverName, home) {

    var signInHTML = '<div id="loginbox"><div><h2>Sign-in to ' + serverName + ' Management Console</h2></div><div id="formset">' +
                     '<form action="login.action" method="POST" target="_self"><fieldset><legend>Enter login credentials</legend><div>' +
                     '<label for="txtUserName">Username:</label><input type="text" id="txtUserName" name="username" size="30"/>' +
                     '</div><div><label for="txtPassword">Password:</label><input type="password" id="txtPassword" name="password" size="30"/>' +
                     '</div><div class="buttonrow"><input type="submit" value="Log In"/><p><a href="#" onclick="javascript:showForgotPassword(\'' + serverName + '\', \'' + home + '\'); return false;">Forgot Password</a>&#160;&#160;&#160;&#160;&#160;' +
                     '<a href="#" onclick="javascript:showSignInHelp(\'' + serverName + '\', \'' + home + '\'); return false;">Sign-in Help</a>&#160;&#160;&#160;&#160;&#160;</p>' +
                     '</div></fieldset></form></div></div><div id="alertMessageBox" style="display:none;position:absolute;z-index: 600;">' +
                     '<!--the message area--><p id="alertMessageBoxMessageArea"></p><!-- the button area--><p id="alertButton" align="right">' +
                     '<input id="alertBoxButton" type="button" value="  OK  " onclick="document.getElementById("alertMessageBox").style.display="none";return false;"/>' +
                     '</p></div>';

    document.getElementById("middle").innerHTML = signInHTML;

}


function addLibraryFileuplod(objDiv) {
        var blankLabelElem = document.createElement('label');
        blankLabelElem.innerHTML = "&nbsp;";
        var elem = document.createElement('input');
        var brElem = document.createElement('br');
        var nameAttr = document.createAttribute('name');
        nameAttr.value = "jarResourceWSDLView";
        var idAttr = document.createAttribute('id');
        idAttr.value = "jarResourceWSDLView";
        var sizeAttr = document.createAttribute('size');
        sizeAttr.value = "50";
        var typeAttr = document.createAttribute('type');
        typeAttr.value = "file";
        elem.attributes.setNamedItem(nameAttr);
        elem.attributes.setNamedItem(idAttr);
        elem.attributes.setNamedItem(sizeAttr);
        elem.attributes.setNamedItem(typeAttr);
        objDiv.appendChild(brElem);
        objDiv.appendChild(blankLabelElem);
        objDiv.appendChild(elem);
    }

/*Object that's used to execute second run of the '*' file upload method*/
function FileExcutor() {
}

/*
FileExcutor.execute = foo; is the method that should be called with extraStoreDirUUID(uuid);
*/
new FileExcutor();

/*
 * This is the method that's going to be invoked by
 * CertificateFileUploadExecutor for WS Call
 */
function extraStoreDirUUID(uuid) {
    FileExcutor.execute(uuid);
}

function completeServiceFileUpload(msg) {
    wso2.wsf.Util.cursorWait();
    if (msg) {
        showAARGenerationCompleteMsg(msg);
    } else {
        showAARGenerationCompleteMsg("Archive was successfully uploaded.\n" +
                                     "This page will be auto refreshed shortly.");
    }
}

function showAARGenerationCompleteMsg(msg) {
    wso2.wsf.Util.cursorClear();
    wso2.wsf.Util.alertMessage(msg);
    showServiceInitializer();
}

function alternateTableRows(id, evenStyle, oddStyle) {
    if (document.getElementsByTagName) {
        if (document.getElementById(id)) {
            var table = document.getElementById(id);
            var rows = table.getElementsByTagName("tr");
            for (var i = 0; i < rows.length; i++) {
                //manipulate rows
                if (i % 2 == 0) {
                    rows[i].className = evenStyle;
                } else {
                    rows[i].className = oddStyle;
                }
            }
        }
    }
}

function getProxyAddress() {
        return "../admin/jsp/WSRequestXSSproxy_ajaxprocessor.jsp";
}

function validatePasswordOnCreation(fld1name, fld2name, regString) {
    var error = "";
    var pw1 = document.getElementsByName(fld1name)[0].value;
    var pw2 = document.getElementsByName(fld2name)[0].value;
    
    var regEx = new RegExp(regString);

     // check for a value in both fields.
    if (pw1 == '' || pw2 == '') {
            error = "Empty Password";
            return error;
    }

    if (!pw1.match(regEx)) {
    	error = "No conformance";
    	return error;
    }

    //check the typed passwords mismatch
    if (pw1 != pw2) {
        error = "Password Mismatch";
        return error;
    }

    return error;
}


function validateEmpty(fldname) {
    var fld = document.getElementsByName(fldname)[0];
    var error = "";
    var value = fld.value;
    if (value.length == 0) {
        error = fld.name + " ";
        return error;
    }

    value = value.replace(/^\s+/, "");
    if (value.length == 0) {
        error = fld.name + "(contains only spaces) ";
        return error;
    }

    return error;
}

function isEmpty(fldname) {
    var fld = document.getElementsByName(fldname)[0];
    if (fld.value.length == 0) {
        return true;
    }
    fld.value = fld.value.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
    if (fld.value.length == 0) {
        return true;
    }

    return false;
}

function validateText(e) {
    var key = String.fromCharCode(getkey(e));
    if (key == null) {
        return true;
    }

    var regEx = /[~!@#$%^&*()\\\/+=\-:;<>'"?[\]{}|\s,]/;
    if (regEx.test(key)) {
        CARBON.showWarningDialog("Invalid character");
        return false;
    }
    return true;
}

function validateName(name) {
    var key = String.fromCharCode(getkey(name));
    if (key == null) {
        return false;
    }

    //var regEx = new RegExp("^[a-zA-Z_0-9\\-=,]{3,30}$");
    var regEx = new RegExp("^[^~!@#$;%^*'+={}\\|\\\\<>]{3,30}$");
    if (!name.match(regEx)) {
        CARBON.showWarningDialog("Invalid name entered. Please make sure the entered" +
        		" name does not contain special characters and it's length is between 3 and 30");
        return false;
    }
    return true;
}

function getkey(e) {
    if (window.event) {
        return window.event.keyCode;
    } else if (e) {
        return e.which;
    } else {
        return null;
    }
}

function sessionAwareFunction(success, message, failure) {
    var random = Math.floor(Math.random() * 2000);
    var errorMessage = "Session timed out. Please login again.";
    if (message && typeof message != "function") {
        errorMessage = message;
    } else if (success && typeof success != "function") {
        errorMessage = success;
    }
    if (!failure && typeof message == "function") {
        failure = message;
    }
    if(typeof(Ajax) != "undefined"){
	    new Ajax.Request('../admin/jsp/session-validate.jsp',
	    {
	        method:'post',
	        asynchronous:false,
	        onSuccess: function(transport) {
	            var returnValue = transport.responseText;
	            if(returnValue.search(/----valid----/) == -1){
	                if (failure && typeof failure == "function") {
	                    failure();
	                } else {
	                    CARBON.showErrorDialog(errorMessage,function(){
	                        location.href="../admin/logout_action.jsp";
	                    }, function(){
	                        location.href="../admin/logout_action.jsp";
	                    });
	                }
	            } else {
	                if (success && typeof success == "function") {
	                    success();
	                }
	            }
	        },
	        onFailure: function() {
	
	        }
	    });
    }else{
        jQuery.ajax({
        type:"POST",
        url:'../admin/jsp/session-validate.jsp',
        data: 'random='+random,
        success:
                function(data, status)
                {
                    var returnValue = data;
	            if(returnValue.search(/----valid----/) == -1){
	                if (failure && typeof failure == "function") {
	                    failure();
	                } else {
	                    CARBON.showErrorDialog(errorMessage,function(){
	                        location.href="../admin/logout_action.jsp";
	                    }, function(){
	                        location.href="../admin/logout_action.jsp";
	                    });
	                }
	            } else {
	                if (success && typeof success == "function") {
	                    success();
	                }
	            }

                }
    	});
    }
}
function spaces(len)
{
	var s = '';
	var indent = len*4;
	for (i=0;i<indent;i++) {s += " ";}

	return s;
}
function format_xml(str)
{ 
	var xml = '';

	// add newlines
	str = str.replace(/(>)(<)(\/*)/g,"$1\r$2$3");

	// add indents
	var pad = 0;
	var indent;
	var node;

	// split the string
	var strArr = str.split("\r");

	// check the various tag states
	for (var i = 0; i < strArr.length; i++) {
		indent = 0;
		node = strArr[i];

		if(node.match(/.+<\/\w[^>]*>$/)){ //open and closing in the same line
			indent = 0;
		} else if(node.match(/^<\/\w/)){ // closing tag
			if (pad > 0){pad -= 1;}
		} else if (node.match(/^<\w[^>]*[^\/]>.*$/)){ //opening tag
			indent = 1;
		} else
			indent = 0;
		//}

		xml += spaces(pad) + node + "\r";
		pad += indent;
	}

	return xml;
}
