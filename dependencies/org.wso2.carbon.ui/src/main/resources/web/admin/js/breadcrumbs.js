//todo - this is a hack, should be assinged at runtime
var solutionPrefix = 'ds';

function setSolutionPrefix(prefix) {
    solutionPrefix = prefix;
}

function getCookieName() {
    return solutionPrefix + '-breadcrumb-cookie';
}

//function generateBreadcrumbs(div, currentPosition, goBackLevel) {
function generateBreadcrumbs(div, currentPosition) {

    fixForBack(currentPosition);
    var cookieDetails = getCookieDetails();
    var text = "<h4>";

    if (cookieDetails != null) {
        var currentLevel = parseInt(cookieDetails[0]);
        if (currentLevel > 0) {
            var i, back, url;
            var info = new Array();
            for (i = 1; i < currentLevel + 1; i++) {
                back = "";
                if (cookieDetails[i] != null) {
                    info = cookieDetails[i].split(',');
                    url = info[2];
                    if (url.indexOf('?') != -1) {
                        url = url.substring(0, url.indexOf('?'));
                    }
                    if (url.indexOf('#') != -1) {
                        url = url.substring(0, url.indexOf('#'));
                    }
                    if (info[0] == 'xslt') {
                        back = "?back=true";
                    }
                    text += "<a href=" + url + back + " onClick=\"javascript:deleteAdditionalEntries(" + i + ");\">" + info[1] + "</a>&#160;&gt;&#160;";
                }
            }
        }
    }
    text += currentPosition + "</h4>";
    div.innerHTML = text;
    div.style.display = 'inline';
}

function prepareCancelButton(link) {
    var cookieDetails = getCookieDetails();
    if (cookieDetails != null) {
        var currentLevel = parseInt(cookieDetails[0]);
        var back = "";
        if (currentLevel > 0) {
            var info = new Array();
            info = cookieDetails[currentLevel].split(',');
            var url = info[2];
            if (url.indexOf('?') != -1) {
                url = url.substring(0, url.indexOf('?'));
            }
            if (info[0] == 'xslt') {
                back = "?back=true";
            }
            link.href = url + back;
        }
    }
}

function fixForBack(currentPosition) {
    var cookieDetails = getCookieDetails();
    if (cookieDetails != null) {
        var currentLevel = parseInt(cookieDetails[0]);
        if (currentLevel > 0) {
            var info = new Array();
            var i;
            for (i = currentLevel; i > 0; i--) {
                info = cookieDetails[i].split(',');
                if (info[1] == currentPosition) {
                    deleteCurrentEntry();
                    deleteAdditionalEntries(i);
                }
            }
        }
    }
}

function updateHistory(breadcrumbName, url, type, jsMethod) {
    var history = new Array();
    history[0] = type;
    history[1] = breadcrumbName;
    history[2] = url;
    history[3] = jsMethod;
    history[4] = '-+-'

    var detailString = getCookie(getCookieName());
    var level, otherPart;
    if (detailString != null) {
        level = parseInt(detailString.substring(0, 1)) + 1;
        otherPart = detailString.substring(1, detailString.length) + history;
    } else {
        level = '1-+-';
        otherPart = history;
    }
    setCookie(getCookieName(), level + otherPart);
}

function getCookieDetails() {
    var details = getCookie(getCookieName());
    if (details != null) {
        var detailArray = new Array();
        detailArray = details.split('-+-');
        return detailArray;
    } else {
        return null;
    }
}

function deleteBreadcrumbCookie() {
    deleteCookie(getCookieName());
}

function goBack(steps) {
    var cookieDetails = getCookieDetails();
    var currentLevel = parseInt(cookieDetails[0]);
    if (currentLevel >= steps) {
        deleteAdditionalEntries(currentLevel - steps + 1);
    }
}

function deleteAdditionalEntries(levelToGo) {
    var cookieDetails = getCookieDetails();
    var currentLevel = parseInt(cookieDetails[0]);

    var temp = Array();
    temp = cookieDetails[levelToGo].split(',');
    if (temp[0] != 'xslt') {
        levelToGo--;
    }

    if (levelToGo < currentLevel) {
        var i;
        cookieDetails[0] = levelToGo;
        var newDetails = "";
        for (i = 0; i < levelToGo + 1; i ++) {
            newDetails += (cookieDetails[i] + '-+-');
        }
        setCookie(getCookieName(), newDetails);
    }
}

function deleteCurrentEntry() {
    var cookieDetails = getCookieDetails();
    var currentLevel = parseInt(cookieDetails[0]);
    cookieDetails[0] = currentLevel - 1;

    var newDetails = "";
    var i;
    for (i = 0; i < currentLevel; i ++) {
        newDetails += (cookieDetails[i] + '-+-');
    }
    setCookie(getCookieName(), newDetails);
}

function getDivToLoad() {
    var cookieDetails = getCookieDetails();
    var currentLevel = parseInt(cookieDetails[0]);
    var currentPageDetails = new Array();
    currentPageDetails = cookieDetails[currentLevel].split(',');
    return currentPageDetails[3];
}


function isBack(url) {
    var devidePoint = url.indexOf("?");
    if (devidePoint != -1) {
        var params = new Array();
        params = url.substring(devidePoint + 1, url.length).split('&');
        var i;
        var param = new Array();
        for (i = 0; i < params.length; i++) {
            param = params[i].split('=');
            if (param[0] == 'back' && param[1] == 'true') {
                return true;
            }
        }
    }
    return false;
}
