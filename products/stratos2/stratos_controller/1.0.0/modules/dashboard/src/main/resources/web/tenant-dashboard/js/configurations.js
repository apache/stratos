//service urls
var managerUrl;
var asUrl;
var dssUrl;
var esbUrl;
var gregUrl;
var msUrl;
var gsUrl;
var mbUrl;
var cepUrl;
var isUrl;
var bpsUrl;
var brsUrl;
var bamUrl;
var csgUrl;
var linkSuffix;

// array definitions to store service feature urls
var managerFeaturesURL=new Array;
var asFeaturesUrl = new Array;
var dssFeaturesUrl = new Array;
var esbFeaturesUrl = new Array;
var gregFeaturesUrl = new Array;
var msFeaturesUrl = new Array;
var gsFeaturesUrl = new Array;
var mbFeaturesUrl = new Array;
var cepFeaturesUrl = new Array;
var isFeaturesUrl = new Array;
var bpsFeaturesUrl = new Array;
var brsFeaturesUrl = new Array;
var bamFeaturesUrl = new Array;
var csgFeaturesUrl = new Array;


// urls of features

// Manager
managerFeaturesURL[0]="/carbon/tenant-billing/past_invoice.jsp"; // link to billing component
managerFeaturesURL[1]="/carbon/tenant-billing/docs/userguide.html"; // billing component docs
managerFeaturesURL[2]="/carbon/tenant-usage/tenant_usage.jsp" ; // link to metering component
managerFeaturesURL[3]="/carbon/tenant-usage/docs/userguide.html"
managerFeaturesURL[4]="/carbon/account-mgt/account_mgt.jsp"
managerFeaturesURL[5]="/carbon/account-mgt/docs/userguide.html"
managerFeaturesURL[6]="/carbon/userstore/index.jsp"
managerFeaturesURL[7]="/carbon/userstore/docs/userguide.html"
//app server
asFeaturesUrl[0]="/carbon/service-mgt/index.jsp";  // link to web service hosting
asFeaturesUrl[1]="/carbon/service-mgt/docs/userguide.html"; //web service hosting docs
asFeaturesUrl[2]="/carbon/webapp-mgt/upload.jsp";//web -app hosting
asFeaturesUrl[3]="/carbon/webapp-mgt/docs/userguide.html";//web -app hosting docs
asFeaturesUrl[4]="/carbon/tracer/index.jsp";//message tracing
asFeaturesUrl[5]="/carbon/tracer/docs/userguide.html";//message tracing docs
asFeaturesUrl[6]="/carbon/wsdl2code/index.jsp";//WSDL2Java Tool
asFeaturesUrl[7]="/carbon/wsdl2code/docs/userguide.html";//WSDL2Java Tool docs
asFeaturesUrl[8]="/carbon/java2wsdl/index.jsp";//Java2WSDL Tool
asFeaturesUrl[9]="/carbon/java2wsdl/docs/userguide.html";//Java2WSDL Tool docs
asFeaturesUrl[10]="/carbon/wsdl_validator/index.jsp";//WSDL Validator
asFeaturesUrl[11]="/carbon/wsdl_validator/docs/userguide.html";//WSDL Validator docs
asFeaturesUrl[12]="/carbon/modulemgt/index.jsp";//axis2 modules mgmnt
asFeaturesUrl[13]="/carbon/modulemgt/docs/userguide.html";//axis2 modules mgmnt docs
asFeaturesUrl[14]="/carbon/tryit/index.jsp";//Service testing
asFeaturesUrl[15]="/carbon/tryit/docs/userguide.html";//service testing docs

//bam server
bamFeaturesUrl[0]="/carbon/bam-server-data/mediation_data.jsp";  // link to Real Time Mediation Monitoring
bamFeaturesUrl[1]="/carbon/bam-server-data/docs/userguide.html";  // link to Real Time Mediation Monitoring docs
bamFeaturesUrl[2]="/carbon/bam-server-data/mediation_analytics.jsp"; //link to Mediation Analysis
bamFeaturesUrl[3]="/carbon/bam-server-data/docs/userguide.html"; //link to Mediation Analysis docs
bamFeaturesUrl[4]="/carbon/bam-server-data/service_data.jsp";//link to Real time Service Monitoring
bamFeaturesUrl[5]="/carbon/bam-server-data/docs/userguide.html";//link to Real time Service Monitoring docs
bamFeaturesUrl[6]="";//link to Summary Generation
bamFeaturesUrl[7]="";//link to Summary Generation
bamFeaturesUrl[8]="/carbon/bam-server-data/service_stats.jsp";//link to Service Invocation Analysis
bamFeaturesUrl[9]="/carbon/bam-server-data/docs/userguide.html";//link to Service Invocation Analysis docs
bamFeaturesUrl[10]="/carbon/dashboard/index.jsp";//link to Dashboard
bamFeaturesUrl[11]="/carbon/dashboard/docs/userguide.html";//link to Dashboard docs
bamFeaturesUrl[12]="";//link to Activity Correlation and Monitoring
bamFeaturesUrl[13]="";//link to Activity Correlation and Monitoring docs
bamFeaturesUrl[14]="";//link to Message Collection and Archival
bamFeaturesUrl[15]="";//link to Message Collection and Archival docs

//bps server
bpsFeaturesUrl[0]="";  // link to WS-BPEL 2.0 and BPELWS 1.1
bpsFeaturesUrl[1]="/carbon/admin/docs/userguide.html";  // link to WS-BPEL 2.0 and BPELWS 1.1 docs
bpsFeaturesUrl[2]="";  // link Secure Business Processes
bpsFeaturesUrl[3]="/carbon/admin/docs/userguide.html";  // link Secure Business Processes docs
bamFeaturesUrl[4]="";//link to Process Monitoring
bamFeaturesUrl[5]="/carbon/admin/docs/userguide.html";//link to Process Monitoring docs
bpsFeaturesUrl[6]=""; //link to Instance Data Cleanup
bpsFeaturesUrl[7]="/carbon/admin/docs/userguide.html"; //link to Instance Data Cleanup docs
bpsFeaturesUrl[8]=""; //link to BPEL Extensions
bpsFeaturesUrl[9]="/carbon/admin/docs/userguide.html"; //link to BPEL Extensions docs
bamFeaturesUrl[10]="";//link to Process Versioning
bamFeaturesUrl[11]="/carbon/admin/docs/userguide.html";//link to Process Versioning docs

//brs server
brsFeaturesUrl[0]="/carbon/service-mgt/index.jsp";  // link to web service hosting support
brsFeaturesUrl[1]="/carbon/service-mgt/docs/userguide.html"; //web service hosting docs
brsFeaturesUrl[2]="/carbon/ruleservices/rule_service_wizard_step1.jsp";//Rule service creation support
brsFeaturesUrl[3]="/carbon/ruleservices/docs/userguide.html";//Rule service creation support docs
brsFeaturesUrl[4]="/carbon/resources/resource.jsp";//Registry as a Rule Repository
brsFeaturesUrl[5]="/carbon/resources/docs/userguide.html";//Registry as a Rule Repository docs
brsFeaturesUrl[6]="/carbon/tryit/index.jsp";//Service testing
brsFeaturesUrl[7]="/carbon/tryit/docs/userguide.html";//service testing docs
/*brsFeaturesUrl[8]="/carbon/tracer/index.jsp";//message tracing
brsFeaturesUrl[9]="/carbon/tracer/docs/userguide.html";//message tracing docs
brsFeaturesUrl[10]="/carbon/wsdl2code/index.jsp";//WSDL2Java Tool
brsFeaturesUrl[11]="/carbon/wsdl2code/docs/userguide.html";//WSDL2Java Tool docs
brsFeaturesUrl[12]="/carbon/java2wsdl/index.jsp";//Java2WSDL Tool
brsFeaturesUrl[13]="/carbon/java2wsdl/docs/userguide.html";//Java2WSDL Tool docs
brsFeaturesUrl[14]="/carbon/wsdl_validator/index.jsp";//WSDL Validator
brsFeaturesUrl[15]="/carbon/wsdl_validator/docs/userguide.html";//WSDL Validator docs */

//cep server
cepFeaturesUrl[0]="/carbon/CEP/cep_queries.jsp"; // link to CEP buckets list
cepFeaturesUrl[1]="/carbon/CEP/docs/userguide.html"; // link to CEP buckets doc
cepFeaturesUrl[2]= "/carbon/resources/resource.jsp?region=region3&item=resource_browser_menu&viewType=std&path=/_system/governance/message" ; // path to registry stored message boxes
cepFeaturesUrl[3]="/carbon/messagebox/docs/userguide.html"; // message box docs

//dss server
dssFeaturesUrl[0]="/carbon/service-mgt/index.jsp";  // link to Data Service Hosting
dssFeaturesUrl[1]="/carbon/service-mgt/docs/userguide.html"; //Data Service Hosting docs
dssFeaturesUrl[2]="/carbon/ds/scriptAddSource.jsp";//Data as a Service
dssFeaturesUrl[3]="/carbon/ds/docs/userguide.html";//Data as a Service docs
dssFeaturesUrl[4]="/carbon/tryit/index.jsp";//Service testing
dssFeaturesUrl[5]="/carbon/tryit/docs/userguide.html";//service testing docs
dssFeaturesUrl[6]="/carbon/tracer/index.jsp";//message tracing
dssFeaturesUrl[7]="/carbon/tracer/docs/userguide.html";//message tracing docs
dssFeaturesUrl[8]="/carbon/adminconsole/databases.jsp";//DB Explorer
dssFeaturesUrl[9]="/carbon/adminconsole/docs/userguide.html";//DB Explorer docs
/*dssFeaturesUrl[6]="/carbon/wsdl2code/index.jsp";//WSDL2Java Tool
dssFeaturesUrl[7]="/carbon/wsdl2code/docs/userguide.html";//WSDL2Java Tool docs
dssFeaturesUrl[8]="/carbon/java2wsdl/index.jsp";//Java2WSDL Tool
dssFeaturesUrl[9]="/carbon/java2wsdl/docs/userguide.html";//Java2WSDL Tool docs
dssFeaturesUrl[10]="/carbon/wsdl_validator/index.jsp";//WSDL Validator
dssFeaturesUrl[11]="/carbon/wsdl_validator/docs/userguide.html";//WSDL Validator docs */

//esb server
esbFeaturesUrl[0]="/carbon/proxyservices/templates.jsp";  // Proxy Service
esbFeaturesUrl[1]="/carbon/proxyservices/docs/userguide.html"; //Proxy Service docs
esbFeaturesUrl[2]="/carbon/sequences/list_sequences.jsp";//Sequence
esbFeaturesUrl[3]="/carbon/sequences/docs/userguide.html";//Sequence docs
esbFeaturesUrl[4]="/carbon/endpoints/index.jsp";//Endpoint
esbFeaturesUrl[5]="/carbon/endpoints/docs/userguide.html";//Endpoint docs
esbFeaturesUrl[6]="/carbon/task/index.jsp";//Scheduled Tasks
esbFeaturesUrl[7]="/carbon/task/docs/userguide.html";//Scheduled Tasks docs
esbFeaturesUrl[8]="/carbon/message_processor/index.jsp";//Store and Forward
esbFeaturesUrl[9]="/carbon/message_processor/docs/userguide.html";//Store and Forward docs
esbFeaturesUrl[10]="/carbon/executors/list_executors.jsp";//Priority Execution
esbFeaturesUrl[11]="/carbon/executors/docs/userguide.html";//Priority Execution docs
esbFeaturesUrl[12]="/carbon/tryit/index.jsp";//Service testing
esbFeaturesUrl[13]="/carbon/tryit/docs/userguide.html";//service testing docs
esbFeaturesUrl[14]="/carbon/tracer/index.jsp";//message tracing
esbFeaturesUrl[15]="/carbon/tracer/docs/userguide.html";//message tracing docs

//greg server
gregFeaturesUrl[0]="//carbon/services/services.jsp";  // Service
gregFeaturesUrl[1]="/carbon/services/docs/userguide.html"; //Service docs
gregFeaturesUrl[2]="/carbon/wsdl/wsdl.jsp";//WSDL
gregFeaturesUrl[3]="/carbon/wsdl/docs/userguide.html";//WSDL docs
gregFeaturesUrl[4]="/carbon/schema/schema.jsp";//Schema
gregFeaturesUrl[5]="/carbon/schema/docs/userguide.html";//Schema docs
gregFeaturesUrl[6]="/carbon/policy/policy.jsp";//Policy
gregFeaturesUrl[7]="/carbon/policy/docs/userguide.html";//Policy docs
gregFeaturesUrl[8]="/carbon/search/advancedSearch.jsp";//Search
gregFeaturesUrl[9]="/carbon/search/docs/userguide.html";//Search docs
gregFeaturesUrl[10]="/carbon/activities/activity.jsp";//Activities
gregFeaturesUrl[11]="/carbon/activities/docs/userguide.html";//Activities docs
gregFeaturesUrl[12]="/carbon/notifications/notifications.jsp";//Notifications
gregFeaturesUrl[13]="/carbon/notifications/docs/userguide.html";//Notifications docs
gregFeaturesUrl[14]="/carbon/extensions/add_extensions.jsp";//Extensions
gregFeaturesUrl[15]="/carbon/extensions/docs/userguide.html";//Extensions docs

//gs server
gsFeaturesUrl[0]="";  // Enterprise Information Portal
gsFeaturesUrl[1]=""; //Enterprise Information Portal docs
gsFeaturesUrl[2]="";//Easy User Options
gsFeaturesUrl[3]="";//Easy User Options docs
gsFeaturesUrl[4]="";//Author Gadgets
gsFeaturesUrl[5]="";//Author Gadgets docs
gsFeaturesUrl[6]="";//Client-side Gadgets
gsFeaturesUrl[7]="";//Client-side Gadgets docs
gsFeaturesUrl[8]="";//Enterprise Gadget Repository
gsFeaturesUrl[9]="";//Enterprise Gadget Repository docs
gsFeaturesUrl[10]="";//Anonymous Mode
gsFeaturesUrl[11]="";//Anonymous Mode docs
gsFeaturesUrl[12]="";//Secure Sign-in Options
gsFeaturesUrl[13]="";//Secure Sign-in Options docs
gsFeaturesUrl[14]="";//Extensions Management Console
gsFeaturesUrl[15]="";//Extensions Management Console docs

//is server
isFeaturesUrl[0]="/carbon/userprofile/index.jsp";  //User Profile
isFeaturesUrl[1]="/carbon/userprofile/docs/userguide.html"; //User Profile docs
isFeaturesUrl[2]="/carbon/identity-provider/index.jsp";//OpenID
isFeaturesUrl[3]="/carbon/identity-provider/docs/userguide.html";//OpenID docs
isFeaturesUrl[4]="/carbon/identity-provider/index.jsp";//Information Card
isFeaturesUrl[5]="/carbon/identity-provider/docs/userguide.html";//Information Card docs
isFeaturesUrl[6]="/carbon/sso-saml/manage_service_providers.jsp";//SAML 2.0 Single Sign-On
isFeaturesUrl[7]="/carbon/sso-saml/docs/userguide.html";//SAML 2.0 Single Sign-On docs
isFeaturesUrl[8]="/carbon/multi-factor/xmpp-config.jsp";//Multifactor Authentication
isFeaturesUrl[9]="/carbon/multi-factor/docs/userguide.html";//Multifactor Authentication docs
isFeaturesUrl[10]="/carbon/userstore/index.jsp";//Users and Roles
isFeaturesUrl[11]="/carbon/userstore/docs/userguide.html";//Users and Roles docs
isFeaturesUrl[12]="/carbon/identity-trusted-relying-parties/add-trusted-rp.jsp";//Relying Partie
isFeaturesUrl[13]="/carbon/identity-trusted-relying-parties/docs/userguide.html";//Relying Partie docs
isFeaturesUrl[14]="/carbon/keystoremgt/keystore-mgt.jsp";//Key Stores
isFeaturesUrl[15]="/carbon/keystoremgt/docs/userguide.html";//Key Stores docs

//mb server
mbFeaturesUrl[0]="";  //Publish/Subscribe to Topics
mbFeaturesUrl[1]=""; //Publish/Subscribe to Topics docs
mbFeaturesUrl[2]="";//AMQP
mbFeaturesUrl[3]="";//AMQP docs
mbFeaturesUrl[4]="";//Topic Authorization
mbFeaturesUrl[5]="";//Topic Authorization docs
mbFeaturesUrl[6]="";//Manage topics and queues permissions
mbFeaturesUrl[7]="";//Manage topics and queues permissions docs
mbFeaturesUrl[8]="";//SQS support
mbFeaturesUrl[9]="";//SQS support docs
mbFeaturesUrl[10]="";//User based authorization for queues
mbFeaturesUrl[11]="";//User based authorization for queues docs
mbFeaturesUrl[12]="";//Manage message boxes
mbFeaturesUrl[13]="";//Manage message boxes docs
mbFeaturesUrl[14]="";//Message box as event sink
mbFeaturesUrl[15]="";//Message box as event sink docs

//ms server
msFeaturesUrl[0]="/carbon/js_scraper/index.jsp";  //Scrape the Web
msFeaturesUrl[1]="/carbon/js_scraper/docs/userguide.html"; //Scrape the Web docs
msFeaturesUrl[2]="/carbon/js_service/newMashup.jsp";//Compose and Expose
msFeaturesUrl[3]="/carbon/js_service/docs/userguide.html";//Compose and Expose docs
msFeaturesUrl[4]="/carbon/task/index.jsp";//Schedule Tasks
msFeaturesUrl[5]="/carbon/task/docs/userguide.html";//Schedule Tasks docs
msFeaturesUrl[6]="";//Javascript Stubs
msFeaturesUrl[7]="";//Javascript Stubs docs
msFeaturesUrl[8]="/carbon/modulemgt/index.jsp";//Modules
msFeaturesUrl[9]="/carbon/modulemgt/docs/userguide.html";//Modules docs
msFeaturesUrl[10]="/carbon/tracer/index.jsp";//Message Tracing
msFeaturesUrl[11]="/carbon/tracer/docs/userguide.html";//Message Tracing docs
/*msFeaturesUrl[12]="";//Caching & Throttling
msFeaturesUrl[13]="";//Caching & Throttling docs
msFeaturesUrl[14]="";//service testing
msFeaturesUrl[15]="";//service testing docs */


//csg server
csgFeaturesUrl[0]="/carbon/service-mgt/index.jsp";  
csgFeaturesUrl[1]="/carbon/service-mgt/docs/userguide.html"; 

// functions to generate urls of features upon onclick

 function generateManagerFeatureUrl(which){
	var featureUrl=managerFeaturesURL[which];
	return window.open(featureUrl,'_self',false);
 }


function generateAsFeatureUrl(which){
	var featureUrl=window.asUrl + asFeaturesUrl[which];
	window.open(featureUrl);
 }

function generateBamFeatureUrl(which){
	var featureUrl=window.bamUrl + bamFeaturesUrl[which];
	window.open(featureUrl);
 }

function generateBpsFeatureUrl(which){
	var featureUrl=window.bpsUrl + bpsFeaturesUrl[which];
	window.open(featureUrl);
 }

function generateBrsFeatureUrl(which){
	var featureUrl=window.brsUrl + brsFeaturesUrl[which];
	window.open(featureUrl);
 }
function generateCepFeatureUrl(which){
	var featureUrl=window.cepUrl + cepFeaturesUrl[which];
	window.open(featureUrl);
 }
function generateDssFeatureUrl(which){
	var featureUrl=window.dssUrl + dssFeaturesUrl[which];
	window.open(featureUrl);
 }
function generateEsbFeatureUrl(which){
	var featureUrl=window.esbUrl + esbFeaturesUrl[which];
	window.open(featureUrl);
 }
function generateGregFeatureUrl(which){
	var featureUrl=window.gregUrl + gregFeaturesUrl[which];
	window.open(featureUrl);
 }
function generateGsFeatureUrl(which){
	var featureUrl=window.gsUrl + gsFeaturesUrl[which];
	window.open(featureUrl);
 }
function generateIsFeatureUrl(which){
	var featureUrl=window.isUrl + isFeaturesUrl[which];
	window.open(featureUrl);
 }

function generateMbFeatureUrl(which){
	var featureUrl=window.mbUrl + mbFeaturesUrl[which];
	window.open(featureUrl);
 }
function generateMsFeatureUrl(which){
	var featureUrl=window.msUrl + msFeaturesUrl[which];
	window.open(featureUrl);
 }
function generateCsgFeatureUrl(which){
	var featureUrl=window.csgUrl + csgFeaturesUrl[which];
	window.open(featureUrl);
 }


// functions to set the urls of services


function gotoAppServer(){
  window.open(asUrl,'_newtab')  ;
}

function gotoDss(){
  window.open(dssUrl,'_newtab')  ;
}

function gotoEsb(){
  window.open(esbUrl,'_newtab')  ;
}


function gotoMs(){
  window.open(msUrl,'_newtab')  ;
}

function gotoIs(){
  window.open(isUrl,'_newtab')  ;
}

function gotoGreg(){
  window.open(gregUrl,'_newtab')  ;
}

function gotoGs(){
  window.open(gsUrl,'_newtab')  ;
}

function gotoBam(){
  window.open(bamUrl,'_newtab')  ;
}


function gotoBps(){
  window.open(bpsUrl,'_newtab')  ;
}


function gotoBrs(){
  window.open(brsUrl,'_newtab')  ;
}

function gotoCep(){
  window.open(cepUrl,'_newtab')  ;
}

function gotoMb(){
  window.open(mbUrl,'_newtab')  ;
}

function gotoCsg(){
  window.open(csgUrl,'_newtab')  ;
}








