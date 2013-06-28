function deactivate(domain) {
    sessionAwareFunction(function() {
        CARBON.showConfirmationDialog("Are you sure you want to deactivate the domain: " +
                                      domain + ".", function() {
            var submitForm = document.getElementById("deactivate_form");
            submitForm.submit();
        });
    }, "Session timed out. Please login again.");
}

function checkDomain(nname)
{

    var error = "";
    error += validateEmpty(nname, "Domain");
    if (error != "") {
        return error;
    }
    //var arr = new Array(
    //'.com','.net','.org','.biz','.coop','.info','.museum','.name',
    //'.pro','.edu','.gov','.int','.mil','.ac','.ad','.ae','.af','.ag',
    //'.ai','.al','.am','.an','.ao','.aq','.ar','.as','.at','.au','.aw',
    //'.az','.ba','.bb','.bd','.be','.bf','.bg','.bh','.bi','.bj','.bm',
    //'.bn','.bo','.br','.bs','.bt','.bv','.bw','.by','.bz','.ca','.cc',
    //'.cd','.cf','.cg','.ch','.ci','.ck','.cl','.cm','.cn','.co','.cr',
    //'.cu','.cv','.cx','.cy','.cz','.de','.dj','.dk','.dm','.do','.dz',
    //'.ec','.ee','.eg','.eh','.er','.es','.et','.fi','.fj','.fk','.fm',
    //'.fo','.fr','.ga','.gd','.ge','.gf','.gg','.gh','.gi','.gl','.gm',
    //'.gn','.gp','.gq','.gr','.gs','.gt','.gu','.gv','.gy','.hk','.hm',
    //'.hn','.hr','.ht','.hu','.id','.ie','.il','.im','.in','.io','.iq',
    //'.ir','.is','.it','.je','.jm','.jo','.jp','.ke','.kg','.kh','.ki',
    //'.km','.kn','.kp','.kr','.kw','.ky','.kz','.la','.lb','.lc','.li',
    //'.lk','.lr','.ls','.lt','.lu','.lv','.ly','.ma','.mc','.md','.mg',
    //'.mh','.mk','.ml','.mm','.mn','.mo','.mp','.mq','.mr','.ms','.mt',
    //'.mu','.mv','.mw','.mx','.my','.mz','.na','.nc','.ne','.nf','.ng',
    //'.ni','.nl','.no','.np','.nr','.nu','.nz','.om','.pa','.pe','.pf',
    //'.pg','.ph','.pk','.pl','.pm','.pn','.pr','.ps','.pt','.pw','.py',
    //'.qa','.re','.ro','.rw','.ru','.sa','.sb','.sc','.sd','.se','.sg',
    //'.sh','.si','.sj','.sk','.sl','.sm','.sn','.so','.sr','.st','.sv',
    //'.sy','.sz','.tc','.td','.tf','.tg','.th','.tj','.tk','.tm','.tn',
    //'.to','.tp','.tr','.tt','.tv','.tw','.tz','.ua','.ug','.uk','.um',
    //'.us','.uy','.uz','.va','.vc','.ve','.vg','.vi','.vn','.vu','.ws',
    //'.wf','.ye','.yt','.yu','.za','.zm','.zw');

    var mai = nname.value;

    var val = true;
    var dot = mai.lastIndexOf(".");
    var dname = mai.substring(0, dot);
    var ext = mai.substring(dot, mai.length);
    //alert(ext);

    if (ext.indexOf("-trial") >= 0 || ext.indexOf("-unverified") >= 0) {
        // we are not allowing to create a domain with -trial or -unverified is in the extension
        return "The domain name you entered is not valid. Please enter a valid domain name.";
    }

    if (ext.indexOf("/") >= 0 || ext.indexOf("\\") >= 0) {
        return "The '/' and '\\' is not allowed in a domain name";
    }

    if (dot > 1 && dot < 57)
    {
        //	for(var i=0; i<arr.length; i++)
        //	{
        //	  if(ext == arr[i])
        //	  {
        //	 	val = true;
        //		break;
        //	  }
        //	  else
        //	  {
        //	 	val = false;
        //	  }
        //	}
        if (!val)
        {
            error = "Your domain extension " + ext + " is not correct";
            return error;
        }
        else
        {
            for (var j = 0; j < dname.length; j++)
            {
                var dh = dname.charAt(j);
                var hh = dh.charCodeAt(0);
                if ((hh > 47 && hh < 59) || (hh > 64 && hh < 91) || (hh > 96 && hh < 123) || hh == 45 || hh == 46)
                {
                    if ((j == 0 || j == dname.length - 1) && hh == 45)
                    {
                        error = "Domain name should not begin and end with '-'";
                        return error;
                    }
                }
                else {
                    error = "Your domain name should not have special characters";
                    return error;
                }
            }
        }
    }
    else
    {
        error = "Your Domain name is too short/long or you should have an extension to your domain.";
        return error;
    }

    return error;
}

function clearDomainConfirmationMsg() {
    var domain_confirmation_div = document.getElementById("domain-confirmation-msg");
    domain_confirmation_div.innerHTML = "";
}


function domainSelected() {
    var domain = document.getElementById('domainToValidate');

    var reason = validateEmpty(domain, "Domain");
    if (reason == "") {
        reason += checkDomain(domain);
    }

    if (reason != "") {
        CARBON.showWarningDialog(reason);
        return false;
    }
    sessionAwareFunction(function() {
        var validateDomainForm = document.getElementById('validateDomainForm');
        validateDomainForm.submit();
    }, "Session timed out. Please login again.");

}

function updateContact() {
    var email = document.getElementById("email");
    var oldEmail = document.getElementById("old-email");
    var reason = "";
    if (reason == "") {
        reason += validateEmpty(email, "Email");
    }
    if (reason == "") {
        reason += validateEmail(email);
    }

    if (reason == "" && email.value == oldEmail.innerHTML) {
        reason += "You have not updated your email yet."
    }

    if (reason != "") {
        CARBON.showWarningDialog(reason);
        return;
    }
    // now call the updateContact.

    sessionAwareFunction(function() {
        debugger;
        var busyContactPlaceHolder = document.getElementById("busyContact");
        busyContactPlaceHolder.innerHTML = "<img src=\"images/ajax-loader.gif\"/>";

        new Ajax.Request('../account-mgt/update_contact_ajaxprocessor.jsp',
        {
            method:'post',
            parameters: {email: email.value},

            onSuccess: function(transport) {
                var returnValue = transport.responseText;
                busyContactPlaceHolder.innerHTML = "";

                if (returnValue.search(/----failed----/) == -1) {
                    CARBON.showWarningDialog("We have sent an email to validate the contact details you just entered. Please follow " +
                                             "the instructions in there to submit the updated contact details");
                }
                else {
                    CARBON.showWarningDialog("Updating the contact information failed.");
                }
            },

            onFailure: function(transport) {
                busyContactPlaceHolder.innerHTML = "";
                CARBON.showWarningDialog("Updating the contact information failed.");
            }
        });
    }, "Session timed out. Please login again.");
}


function updateProfile() {
    var update_profile_form = document.getElementById("update_profile_form");
    var firstname = document.getElementById("firstname");
    var oldFirstname = document.getElementById("old-firstname");
    var lastname = document.getElementById("lastname");
    var oldLastname = document.getElementById("old-lastname");

    var reason = "";

    if (reason == "") {
        reason += validateEmpty(firstname, "First Name");
    }
    if (reason == "") {
        reason += validateEmpty(lastname, "Last Name");

        if ((reason == "" && firstname.value == oldFirstname.innerHTML) &&
            (reason == "" && lastname.value == oldLastname.innerHTML)) {
            reason += "You have not updated your firstname or last name yet.";
        }
    }
    if (reason != "") {
        CARBON.showWarningDialog(reason);
        return;
    }
    update_profile_form.submit();
}
