//******************************************************************************************//
// File for cookie related fnctions in JavaScript											//
// Author : 	   Manish Hatwalne (http://www.technofundo.com/)							//
// Feedback :  	   feedback@technofundo.com		(for feedback/bugs)							//
// Created : 	   19 August 2001  															//
// Functions :	   	  		 																//
// 			 (1) setCookie(szName, szValue [,szExpires] [,szPath] [,szDomain] [,szSecure])	//
//			 (2) getCookie(szName) 		   						  			  				//
//			 (3) deleteCookie(szName)														//
// 			 	 																			//
// Feel free to use/modify the code in this file, but always keep the header intact.		//
// And DO NOT re-distribute this file, instead provide a link to the site 	   				//
// http://www.technofundo.com/. Thank You.	  		 										//
// 									  														//
//******************************************************************************************//



//******************************************************************************************
//
// A CGI program uses the following syntax to add cookie information to the HTTP header:
//
// Set-Cookie:   name=value
// [;EXPIRES=dateValue]
// [;DOMAIN=domainName]
// [;PATH=pathName]
// [;SECURE]
//
// This function sets a client-side cookie as above.  Only first 2 parameters are required
// Rest of the parameters are optional. If no szExpires value is set, cookie is a session cookie.
//
// Prototype : setCookie(szName, szValue [,szExpires] [,szPath] [,szDomain] [,bSecure])
//******************************************************************************************


function setCookie(szName, szValue, szExpires, szDomain, bSecure)
{
    var path = '/';
    var szCookieText = 	   escape(szName) + '=' + escape(szValue);
	szCookieText +=	 	   (szExpires ? '; EXPIRES=' + szExpires.toGMTString() : '');
	szCookieText += 	   (path ? '; PATH=' + path : '');
	szCookieText += 	   (szDomain ? '; DOMAIN=' + szDomain : '');
	szCookieText += 	   (bSecure ? '; SECURE' : '');

	document.cookie = szCookieText;
}

//******************************************************************************************
// This functions reads & returns the cookie value of the specified cookie (by cookie name)
//
// Prototype : getCookie(szName)
//******************************************************************************************

function getCookie(szName)
{
 	var szValue =	  null;
	if(document.cookie)	   //only if exists
	{
           var arr = 		  document.cookie.split((escape(szName) + '='));
       	if(2 <= arr.length)
       	{
           	var arr2 = 	   arr[1].split(';');
       		szValue  = 	   unescape(arr2[0]);
       	}
	}
	return szValue;
}

//******************************************************************************************
// To delete a cookie, pass name of the cookie to be deleted
//
// Prototype : deleteCookie(szName)
//******************************************************************************************

function deleteCookie(szName)
{
 	var tmp = 	  			 	 getCookie(szName);
	if(tmp)
	{ setCookie(szName,tmp,(new Date(1))); }
}

//==========================================^-^==============================================//
//    This and many more interesting and usefull scripts at http://www.technofundo.com/		 //
//==========================================^-^==============================================//   